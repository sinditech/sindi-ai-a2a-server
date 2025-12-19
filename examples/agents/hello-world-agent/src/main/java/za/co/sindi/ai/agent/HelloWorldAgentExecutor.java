/**
 * 
 */
package za.co.sindi.ai.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import za.co.sindi.ai.a2a.server.A2AServerError;
import za.co.sindi.ai.a2a.server.agentexecution.AgentExecutor;
import za.co.sindi.ai.a2a.server.agentexecution.RequestContext;
import za.co.sindi.ai.a2a.server.events.EventQueue;
import za.co.sindi.ai.a2a.types.InternalError;
import za.co.sindi.ai.a2a.types.TaskNotCancelableError;
import za.co.sindi.ai.a2a.utils.Messages;

/**
 * @author Buhake Sindi
 * @since 05 December 2025
 */
@ApplicationScoped
public class HelloWorldAgentExecutor implements AgentExecutor {

	@Inject
	private HelloWorldAgent agent;
	
	@Override
	public void execute(RequestContext context, EventQueue eventQueue) {
		// TODO Auto-generated method stub
		try {
			String result = agent.greet();
			eventQueue.enqueueEvent(Messages.newAgentTextMessage(result));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			Thread.currentThread().interrupt();
			throw new A2AServerError(new InternalError(e.getLocalizedMessage()));
		}
	}

	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) {
		// TODO Auto-generated method stub
		throw new A2AServerError(new TaskNotCancelableError("Cancel not supported."));
	}
}
