/**
 * 
 */
package za.co.sindi.ai.a2a.server.spi.cdi;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import za.co.sindi.ai.a2a.server.runtime.ExtendedAgentCardBuilder;
import za.co.sindi.ai.a2a.server.runtime.PublicAgentCardBuilder;
import za.co.sindi.ai.a2a.server.runtime.impl.AnnotationAgentCardBuilderUtils;
import za.co.sindi.ai.a2a.server.runtime.impl.ExtendedAgentCardBuilderImpl;
import za.co.sindi.ai.a2a.server.runtime.impl.PublicAgentCardBuilderImpl;
import za.co.sindi.ai.a2a.server.spi.Agent;
import za.co.sindi.ai.a2a.server.spi.ExtendedCard;
import za.co.sindi.ai.a2a.server.spi.PublicCard;

/**
 * @author Buhake Sindi
 * @since 17 November 2025
 */
public class A2AServerExtension implements Extension {
	private static final Logger LOGGER = Logger.getLogger(A2AServerExtension.class.getName());
    private static final Set<Class<?>> DETECTED_PUBLIC_AGENTS = new HashSet<>();
    private static final Set<Class<?>> DETECTED_EXTENDED_AGENTS = new HashSet<>();
	
	<T> void processAnnotatedType(@Observes @WithAnnotations({Agent.class}) ProcessAnnotatedType<T> pat) {
		LOGGER.info("processAnnotatedType register " + pat.getAnnotatedType().getJavaClass().getName());
		
		Class<?> clazz = pat.getAnnotatedType().getJavaClass();
		if (clazz.isAnnotationPresent(PublicCard.class)) DETECTED_PUBLIC_AGENTS.add(clazz);
		if (clazz.isAnnotationPresent(ExtendedCard.class)) DETECTED_EXTENDED_AGENTS.add(clazz);
    }

	void register(@Observes final AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
		
		if (DETECTED_PUBLIC_AGENTS.size() > 1) {
			//There can only be 1 public agent.
			afterBeanDiscovery.addDefinitionError(new DeploymentException("There can only be 1 public agent (with the @Agent annotation). Detected " + DETECTED_PUBLIC_AGENTS.size() + " annotations."));
			return ;
		}
		
		afterBeanDiscovery.addBean(new PushNotificationConfigStoreProducer());
		afterBeanDiscovery.addBean(new QueueManagerProducer());
		afterBeanDiscovery.addBean(new TaskStoreProducer());
		
		PublicAgentCardBuilder pacb;
		ExtendedAgentCardBuilder eacb; 
		
		if (!DETECTED_PUBLIC_AGENTS.isEmpty()) {
			Class<?> agentClass = DETECTED_PUBLIC_AGENTS.iterator().next();
			pacb = AnnotationAgentCardBuilderUtils.createPublicAgentCardBuilder(agentClass).supportsAuthenticatedExtendedCard(DETECTED_EXTENDED_AGENTS.contains(agentClass));
			eacb = DETECTED_EXTENDED_AGENTS.contains(agentClass) ? AnnotationAgentCardBuilderUtils.createExtendedAgentCardBuilder(agentClass) : new ExtendedAgentCardBuilderImpl();
		} else {
			pacb = new PublicAgentCardBuilderImpl();
			eacb = new ExtendedAgentCardBuilderImpl();
		}
		
		afterBeanDiscovery.addBean(new AgentCardInfoProducer(pacb, eacb));
	}
}
