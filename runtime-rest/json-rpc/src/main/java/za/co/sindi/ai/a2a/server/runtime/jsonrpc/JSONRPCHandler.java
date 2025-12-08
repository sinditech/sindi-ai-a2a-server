/**
 * 
 */
package za.co.sindi.ai.a2a.server.runtime.jsonrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import za.co.sindi.ai.a2a.server.A2AServerError;
import za.co.sindi.ai.a2a.server.CallContextBuilder;
import za.co.sindi.ai.a2a.server.ServerCallContext;
import za.co.sindi.ai.a2a.server.requesthandlers.RequestHandler;
import za.co.sindi.ai.a2a.server.runtime.impl.RESTCallContextBuilder;
import za.co.sindi.ai.a2a.server.spi.AgentCardInfo;
import za.co.sindi.ai.a2a.types.A2AError;
import za.co.sindi.ai.a2a.types.A2ARequest;
import za.co.sindi.ai.a2a.types.AgentCapabilities;
import za.co.sindi.ai.a2a.types.AgentCard;
import za.co.sindi.ai.a2a.types.AuthenticatedExtendedCardNotConfiguredError;
import za.co.sindi.ai.a2a.types.CancelTaskRequest;
import za.co.sindi.ai.a2a.types.CancelTaskSuccessResponse;
import za.co.sindi.ai.a2a.types.ContentTypeNotSupportedError;
import za.co.sindi.ai.a2a.types.DeleteTaskPushNotificationConfigRequest;
import za.co.sindi.ai.a2a.types.DeleteTaskPushNotificationConfigSuccessResponse;
import za.co.sindi.ai.a2a.types.Event;
import za.co.sindi.ai.a2a.types.GetAuthenticatedExtendedCardRequest;
import za.co.sindi.ai.a2a.types.GetAuthenticatedExtendedCardSuccessResponse;
import za.co.sindi.ai.a2a.types.GetTaskPushNotificationConfigRequest;
import za.co.sindi.ai.a2a.types.GetTaskPushNotificationConfigSuccessResponse;
import za.co.sindi.ai.a2a.types.GetTaskRequest;
import za.co.sindi.ai.a2a.types.GetTaskSuccessResponse;
import za.co.sindi.ai.a2a.types.InternalError;
import za.co.sindi.ai.a2a.types.InvalidAgentResponseError;
import za.co.sindi.ai.a2a.types.InvalidParamsError;
import za.co.sindi.ai.a2a.types.InvalidRequestError;
import za.co.sindi.ai.a2a.types.JSONParseError;
import za.co.sindi.ai.a2a.types.JSONRPCRequest;
import za.co.sindi.ai.a2a.types.Kind;
import za.co.sindi.ai.a2a.types.ListTaskPushNotificationConfigRequest;
import za.co.sindi.ai.a2a.types.ListTaskPushNotificationConfigSuccessResponse;
import za.co.sindi.ai.a2a.types.ListTasksRequest;
import za.co.sindi.ai.a2a.types.MethodNotFoundError;
import za.co.sindi.ai.a2a.types.PushNotificationNotSupportedError;
import za.co.sindi.ai.a2a.types.SendMessageRequest;
import za.co.sindi.ai.a2a.types.SendMessageSuccessResponse;
import za.co.sindi.ai.a2a.types.SendStreamingMessageRequest;
import za.co.sindi.ai.a2a.types.SetTaskPushNotificationConfigRequest;
import za.co.sindi.ai.a2a.types.SetTaskPushNotificationConfigSuccessResponse;
import za.co.sindi.ai.a2a.types.Task;
import za.co.sindi.ai.a2a.types.TaskNotFoundError;
import za.co.sindi.ai.a2a.types.TaskPushNotificationConfig;
import za.co.sindi.ai.a2a.types.TaskResubscriptionRequest;
import za.co.sindi.ai.a2a.types.TransportProtocol;
import za.co.sindi.ai.a2a.types.UnsupportedOperationError;

/**
 * @author Buhake Sindi
 * @since 14 November 2025
 */
@ApplicationScoped
@Path("/")
public class JSONRPCHandler {
	
	private static final Map<Class<? extends A2AError>, Status> STATUS_CODES = new HashMap<>();
	private static final Status DEFAULT_ERROR_STATUS = Status.INTERNAL_SERVER_ERROR;
	
	static {
		STATUS_CODES.put(InvalidRequestError.class, Status.BAD_REQUEST);
		STATUS_CODES.put(InvalidParamsError.class, Status.BAD_REQUEST);
		STATUS_CODES.put(MethodNotFoundError.class, Status.NOT_FOUND);
		STATUS_CODES.put(TaskNotFoundError.class, Status.NOT_FOUND);
		STATUS_CODES.put(InternalError.class, Status.INTERNAL_SERVER_ERROR);
		STATUS_CODES.put(PushNotificationNotSupportedError.class, Status.NOT_IMPLEMENTED);
		STATUS_CODES.put(UnsupportedOperationError.class, Status.NOT_IMPLEMENTED);
		STATUS_CODES.put(ContentTypeNotSupportedError.class, Status.NOT_IMPLEMENTED);
		STATUS_CODES.put(InvalidAgentResponseError.class, Status.INTERNAL_SERVER_ERROR);
	}

	@Inject
	private AgentCardInfo agentCardInfo;
	
	@Inject
	private RequestHandler requestHandler;
	
	private volatile AgentCard agentCard;
	private volatile AgentCard extendedAgentCard;
	
	@GET
	@Path(".well-known/agent-card.json")
	@Produces(MediaType.APPLICATION_JSON)
	public AgentCard getPublicAgentCard(@Context UriInfo uriInfo) {
		if (agentCard == null) {
			String requestUrl = uriInfo.getRequestUri().toString();
			agentCardInfo.getPublicAgentCardBuilder().url(requestUrl).capabilities(new AgentCapabilities(true, null, null, null)).preferredTransport(TransportProtocol.JSONRPC);
			agentCard = agentCardInfo.getPublicAgentCard();
		}
		
		return agentCard;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void request(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@Suspended AsyncResponse asyncResponse,
			A2ARequest request) {
		//This if statement is always true, unless someone changed the implementation (but all A2ARequest implementations extends JSONRequest<T>
		if(request instanceof JSONRPCRequest jsonRpcRequest) {
			CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
			
			switch(jsonRpcRequest.getMethod()) {
				case SendMessageRequest.DEFAULT_METHOD:
						sendMessage((SendMessageRequest) request, builder.build(), asyncResponse);
					break;
				case SendStreamingMessageRequest.DEFAULT_METHOD:
						sendMessageStream((SendStreamingMessageRequest) request, builder.build(), asyncResponse);
					break;
				case GetTaskRequest.DEFAULT_METHOD:
						getTask((GetTaskRequest) request, builder.build(), asyncResponse);
					break;
				case CancelTaskRequest.DEFAULT_METHOD:
						cancelTask((CancelTaskRequest) request, builder.build(), asyncResponse);
					break;
				case ListTasksRequest.DEFAULT_METHOD:
						listTasks((ListTasksRequest) request, builder.build(), asyncResponse);
					break;
				case SetTaskPushNotificationConfigRequest.DEFAULT_METHOD:
						setPushNotificationConfig((SetTaskPushNotificationConfigRequest) request, builder.build(), asyncResponse);
					break;
				case GetTaskPushNotificationConfigRequest.DEFAULT_METHOD:
						getPushNotificationConfig((GetTaskPushNotificationConfigRequest) request, builder.build(), asyncResponse);
					break;
				case TaskResubscriptionRequest.DEFAULT_METHOD:
						resubscribeTask((TaskResubscriptionRequest) request, builder.build(), asyncResponse);
					break;
				case ListTaskPushNotificationConfigRequest.DEFAULT_METHOD:
						listPushNotificationConfig((ListTaskPushNotificationConfigRequest) request, builder.build(), asyncResponse);
					break;
				case DeleteTaskPushNotificationConfigRequest.DEFAULT_METHOD:
						deletePushNotificationConfig((DeleteTaskPushNotificationConfigRequest) request, builder.build(), asyncResponse);
					break;
				case GetAuthenticatedExtendedCardRequest.DEFAULT_METHOD:
						getAuthenticatedExtendedCard((GetAuthenticatedExtendedCardRequest) request, builder.build(), asyncResponse);
					break;
				default: 
						asyncResponse.resume(toResponse(new MethodNotFoundError()));
					break;
			}
		} else {
			//There is a problem here...abort!
			asyncResponse.resume(toResponse(new JSONParseError()));
		}
	}
	
	private Response toResponse(A2AServerError exception) {
		// TODO Auto-generated method stub
		return toResponse(exception.getError());
	}
	
	private Response toResponse(A2AError error) {
		// TODO Auto-generated method stub
		Status status = Optional.ofNullable(STATUS_CODES.get(error.getClass())).orElse(DEFAULT_ERROR_STATUS);
		return Response.status(status).entity(error).type(MediaType.APPLICATION_JSON_TYPE).build();
	}
	
	private void sendMessage(final SendMessageRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			Kind kind = (Kind)requestHandler.onMessageSend(request.getParams(), context);
			SendMessageSuccessResponse response = new SendMessageSuccessResponse(request.getId(), kind);
			asyncResponse.resume(Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void sendMessageStream(final SendStreamingMessageRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			if (agentCard == null || agentCard.getCapabilities() == null || agentCard.getCapabilities().streaming() == null || !agentCard.getCapabilities().streaming()) {
				throw new A2AServerError(new UnsupportedOperationError("Streaming is not supported by the agent."));
			}
			
			Publisher<Event> eventPublisher = requestHandler.onMessageSendStream(request.getParams(), context);
			ResponseBuilder responseBuilder = Response.ok()
					  .type(MediaType.SERVER_SENT_EVENTS_TYPE)
					  .header("Cache-Control", "no-cache, no-transform")
					  .header("Connection", "keep-alive")
					  .entity(new SSEStreamResponseStreamingOutput(request.getId(), eventPublisher));
			asyncResponse.resume(responseBuilder.build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void getTask(final GetTaskRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			Task task = requestHandler.onGetTask(request.getParams(), context);
			GetTaskSuccessResponse response = new GetTaskSuccessResponse(request.getId(), task);
			asyncResponse.resume(Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void cancelTask(final CancelTaskRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			Task task = requestHandler.onCancelTask(request.getParams(), context);
			CancelTaskSuccessResponse response = new CancelTaskSuccessResponse(request.getId(), task);
			asyncResponse.resume(Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void listTasks(final ListTasksRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		asyncResponse.resume(toResponse(new UnsupportedOperationError()));
	}
	
	private void resubscribeTask(final TaskResubscriptionRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			Publisher<Event> eventPublisher = requestHandler.onResubmitToTask(request.getParams(), context);
			ResponseBuilder responseBuilder = Response.ok()
					  .type(MediaType.SERVER_SENT_EVENTS_TYPE)
					  .header("Cache-Control", "no-cache, no-transform")
					  .header("Connection", "keep-alive")
					  .entity(new SSEStreamResponseStreamingOutput(request.getId(), eventPublisher));
			asyncResponse.resume(responseBuilder.build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void getPushNotificationConfig(final GetTaskPushNotificationConfigRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(request.getParams(), context);
			GetTaskPushNotificationConfigSuccessResponse response = new GetTaskPushNotificationConfigSuccessResponse(request.getId(), config);
			asyncResponse.resume(Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void setPushNotificationConfig(final SetTaskPushNotificationConfigRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			if (agentCard == null || agentCard.getCapabilities() == null || agentCard.getCapabilities().pushNotifications() == null || !agentCard.getCapabilities().pushNotifications()) {
				throw new A2AServerError(new PushNotificationNotSupportedError("Push notifications are not supported by the agent."));
			}
			
			TaskPushNotificationConfig config = requestHandler.onSetTaskPushNotificationConfig(request.getParams(), context);
			SetTaskPushNotificationConfigSuccessResponse response = new SetTaskPushNotificationConfigSuccessResponse(request.getId(), config);
			asyncResponse.resume(Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void listPushNotificationConfig(final ListTaskPushNotificationConfigRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			List<TaskPushNotificationConfig> configs = requestHandler.onListTaskPushNotificationConfig(request.getParams(), context);
			ListTaskPushNotificationConfigSuccessResponse response = new ListTaskPushNotificationConfigSuccessResponse(request.getId(), configs == null || configs.isEmpty() ? null : configs.toArray(new TaskPushNotificationConfig[configs.size()]));
			asyncResponse.resume(Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void deletePushNotificationConfig(final DeleteTaskPushNotificationConfigRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			requestHandler.onDeleteTaskPushNotificationConfig(request.getParams(), context);
			DeleteTaskPushNotificationConfigSuccessResponse response = new DeleteTaskPushNotificationConfigSuccessResponse(request.getId());
			asyncResponse.resume(Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
	
	private void getAuthenticatedExtendedCard(final GetAuthenticatedExtendedCardRequest request, final ServerCallContext context, final AsyncResponse asyncResponse) {
		
		try {
			if (agentCard == null || agentCard.getSupportsAuthenticatedExtendedCard() == null || !agentCard.getSupportsAuthenticatedExtendedCard()) {
				throw new A2AServerError(new AuthenticatedExtendedCardNotConfiguredError("Authenticated card not supported."));
			}
			
			AgentCard baseCard = extendedAgentCard;
			if (baseCard == null) baseCard = agentCard;
			GetAuthenticatedExtendedCardSuccessResponse response = new GetAuthenticatedExtendedCardSuccessResponse(request.getId(), baseCard);
			asyncResponse.resume(Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build());
		} catch (A2AServerError exception) {
			asyncResponse.resume(toResponse(exception));
		}
	}
}
