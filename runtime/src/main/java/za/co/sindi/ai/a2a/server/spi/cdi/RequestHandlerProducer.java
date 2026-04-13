/**
 * 
 */
package za.co.sindi.ai.a2a.server.spi.cdi;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import za.co.sindi.ai.a2a.server.agentexecution.AgentExecutor;
import za.co.sindi.ai.a2a.server.events.QueueManager;
import za.co.sindi.ai.a2a.server.requesthandlers.DefaultRequestHandler;
import za.co.sindi.ai.a2a.server.requesthandlers.RequestHandler;
import za.co.sindi.ai.a2a.server.tasks.PushNotificationConfigStore;
import za.co.sindi.ai.a2a.server.tasks.PushNotificationSender;
import za.co.sindi.ai.a2a.server.tasks.TaskStore;

/**
 * @author Buhake Sindi
 * @since 17 November 2025
 */
@ApplicationScoped
public final class RequestHandlerProducer {
	
	@Inject
	private AgentExecutor agentExecutor;
	
	@Inject
	private TaskStore taskStore;
	
	@Inject
	private QueueManager queueManager;
	
	@Inject
	private Instance<PushNotificationConfigStore> pushConfigStore;
	
	@Inject
	private Instance<PushNotificationSender> pushSender;
	
	@Resource
	private ManagedExecutorService managedExecutorService;
	
	@Inject
	private Instance<ManagedExecutorService> instanceManagedExecutorService;
	
	@Produces
	public RequestHandler getRequestHandler() {
		return new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushConfigStore != null && pushConfigStore.isResolvable() ? pushConfigStore.get() : null, pushSender != null && pushSender.isResolvable() ? pushSender.get() : null, null, getManagedExecutorService());
	}

	/**
	 * @return the managedExecutorService
	 */
	public ManagedExecutorService getManagedExecutorService() {
		if (managedExecutorService != null) return managedExecutorService;
		if (instanceManagedExecutorService != null && instanceManagedExecutorService.isResolvable()) return instanceManagedExecutorService.get();
		throw new IllegalStateException("No ManagedExecutorService found, either via @Resource of via CDI injection. Aborting");
	}
}
