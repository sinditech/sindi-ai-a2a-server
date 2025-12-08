/**
 * 
 */
package za.co.sindi.ai.a2a.server.runtime.rest;

import java.util.List;
import java.util.concurrent.Flow.Publisher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import za.co.sindi.ai.a2a.server.A2AServerError;
import za.co.sindi.ai.a2a.server.CallContextBuilder;
import za.co.sindi.ai.a2a.server.requesthandlers.RequestHandler;
import za.co.sindi.ai.a2a.server.runtime.impl.RESTCallContextBuilder;
import za.co.sindi.ai.a2a.server.spi.AgentCardInfo;
import za.co.sindi.ai.a2a.types.AgentCapabilities;
import za.co.sindi.ai.a2a.types.AgentCard;
import za.co.sindi.ai.a2a.types.AuthenticatedExtendedCardNotConfiguredError;
import za.co.sindi.ai.a2a.types.DeleteTaskPushNotificationConfigParams;
import za.co.sindi.ai.a2a.types.Event;
import za.co.sindi.ai.a2a.types.GetTaskPushNotificationConfigParams;
import za.co.sindi.ai.a2a.types.Kind;
import za.co.sindi.ai.a2a.types.ListTaskPushNotificationConfigParams;
import za.co.sindi.ai.a2a.types.MessageSendParams;
import za.co.sindi.ai.a2a.types.PushNotificationNotSupportedError;
import za.co.sindi.ai.a2a.types.SendMessageRequest;
import za.co.sindi.ai.a2a.types.Task;
import za.co.sindi.ai.a2a.types.TaskIdParams;
import za.co.sindi.ai.a2a.types.TaskPushNotificationConfig;
import za.co.sindi.ai.a2a.types.TaskQueryParams;
import za.co.sindi.ai.a2a.types.TransportProtocol;
import za.co.sindi.ai.a2a.types.UnsupportedOperationError;

/**
 * @author Buhake Sindi
 * @since 14 November 2025
 */
@ApplicationScoped
@Path("/")
public class RESTHandler {

	private static final int API_VERSION_NUMBER = 1;
	
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
		String requestUrl = uriInfo.getRequestUri().toString();
		AgentCapabilities capabilities = new AgentCapabilities(true, null, null, null);
		if (agentCard == null) {
			agentCardInfo.getPublicAgentCardBuilder().url(requestUrl).capabilities(capabilities).preferredTransport(TransportProtocol.HTTP_JSON);
			agentCard = agentCardInfo.getPublicAgentCard();
		}
		
		if (extendedAgentCard == null && agentCard != null && agentCard.getSupportsAuthenticatedExtendedCard() != null && agentCard.getSupportsAuthenticatedExtendedCard()) {
			agentCardInfo.getExtendedAgentCardBuilder().url(requestUrl).capabilities(capabilities).preferredTransport(TransportProtocol.HTTP_JSON);
			extendedAgentCard = agentCardInfo.getExtendedAgentCard();
		}
		
		return agentCard;
	}
	
	@POST
	@Path("v" + API_VERSION_NUMBER + "/message:send")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Kind sendMessage(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
//			@HeaderParam(A2AExtensions.HTTP_EXTENSION_HEADER) List<String> headerValues,
			MessageSendParams params) {
		CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		return (Kind) requestHandler.onMessageSend(params, builder.build());
	}
	
	@POST
	@Path("v" + API_VERSION_NUMBER + "/message:stream")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void sendMessageStream(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@Suspended AsyncResponse asyncResponse,
			SendMessageRequest request) {
		if (agentCard == null || agentCard.getCapabilities() == null || agentCard.getCapabilities().streaming() == null || !agentCard.getCapabilities().streaming()) {
			throw new A2AServerError(new UnsupportedOperationError("Streaming is not supported by the agent."));
		}
		
		CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		Publisher<Event> eventPublisher = requestHandler.onMessageSendStream(request.getParams(), builder.build());
		ResponseBuilder responseBuilder = Response.ok()
				  .type(MediaType.SERVER_SENT_EVENTS_TYPE)
				  .header("Cache-Control", "no-cache, no-transform")
				  .header("Connection", "keep-alive")
				  .entity(new SSEStreamResponseStreamingOutput(eventPublisher));
		asyncResponse.resume(responseBuilder.build());
	}
	
	@GET
	@Path("v" + API_VERSION_NUMBER + "/tasks/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Task getTask(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@PathParam("id")String taskId) {
		CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		return requestHandler.onGetTask(new TaskQueryParams(taskId), builder.build());
	}
	
	@POST
	@Path("v" + API_VERSION_NUMBER + "/tasks/{id}:cancel")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Task cancelTask(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@PathParam("id")String taskId,
			CancelTaskRequest request) {
		CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		return requestHandler.onCancelTask(new TaskIdParams(taskId), builder.build());
	}
	
	@POST
	@Path("v" + API_VERSION_NUMBER + "/tasks/{id}:subscribe")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void resubscribeTask(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@Suspended AsyncResponse asyncResponse,
			@PathParam("id")String taskId,
			SubscribeTaskRequest request) {
		CallContextBuilder callContextBuilder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		Publisher<Event> eventPublisher = requestHandler.onResubmitToTask(new TaskIdParams(taskId), callContextBuilder.build());
		ResponseBuilder responseBuilder = Response.ok()
												  .type(MediaType.SERVER_SENT_EVENTS_TYPE)
												  .header("Cache-Control", "no-cache, no-transform")
												  .header("Connection", "keep-alive")
												  .entity(new SSEStreamResponseStreamingOutput(eventPublisher));
		asyncResponse.resume(responseBuilder.build());
	}
	
	@POST
	@Path("v" + API_VERSION_NUMBER + "/tasks/{id}/pushNotificationConfigs")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public TaskPushNotificationConfig setTaskPushNotificationConfig(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@PathParam("id")String taskId,
			CreateTaskPushNotificationConfigRequest request) {
		if (agentCard == null || agentCard.getCapabilities() == null || agentCard.getCapabilities().pushNotifications() == null || !agentCard.getCapabilities().pushNotifications()) {
			throw new A2AServerError(new PushNotificationNotSupportedError());
		}
		
		CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		return requestHandler.onSetTaskPushNotificationConfig(new TaskPushNotificationConfig(taskId, request.config().pushNotificationConfig()), builder.build());
	}
	
	@GET
	@Path("v" + API_VERSION_NUMBER + "/tasks/{id}/pushNotificationConfigs/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	public TaskPushNotificationConfig getTaskPushNotificationConfig(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@PathParam("id")String taskId,
			@PathParam("configId")String configId) {
		CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		return requestHandler.onGetTaskPushNotificationConfig(new GetTaskPushNotificationConfigParams(taskId, configId), builder.build());
	}
	
	@GET
	@Path("v" + API_VERSION_NUMBER + "/tasks/{id}/pushNotificationConfigs")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TaskPushNotificationConfig> listTaskPushNotificationConfig(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@PathParam("id")String taskId) {
		CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		return requestHandler.onListTaskPushNotificationConfig(new ListTaskPushNotificationConfigParams(taskId), builder.build());
	}
	
	@DELETE
	@Path("v" + API_VERSION_NUMBER + "/tasks/{id}/pushNotificationConfigs/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	public void deleteTaskPushNotificationConfig(@Context SecurityContext securityContext,
			@Context HttpHeaders httpHeaders,
			@PathParam("id")String taskId,
			@PathParam("configId")String configId) {
		if (agentCard == null || agentCard.getCapabilities() == null || !agentCard.getCapabilities().pushNotifications()) {
			throw new A2AServerError(new PushNotificationNotSupportedError());
		}
		
		CallContextBuilder builder = new RESTCallContextBuilder(securityContext, httpHeaders.getRequestHeaders());
		requestHandler.onDeleteTaskPushNotificationConfig(new DeleteTaskPushNotificationConfigParams(taskId, configId), builder.build());
	}
	
	@GET
	@Path("v" + API_VERSION_NUMBER + "/card")
	@Produces(MediaType.APPLICATION_JSON)
	public AgentCard getAuthenticatedExtendedCard(@Context SecurityContext securityContext,
			@Context UriInfo uriInfo) {
		if (agentCard == null || agentCard.getSupportsAuthenticatedExtendedCard() == null || !agentCard.getSupportsAuthenticatedExtendedCard()) {
			throw new A2AServerError(new AuthenticatedExtendedCardNotConfiguredError("Authenticated card not supported."));
		}
		if (securityContext.getUserPrincipal() == null) {
			throw new NotAuthorizedException("Bearer", new Object[0]);
		}
		if (extendedAgentCard == null) {
			throw new NotSupportedException();
		}
		
		return extendedAgentCard;
	}
}
