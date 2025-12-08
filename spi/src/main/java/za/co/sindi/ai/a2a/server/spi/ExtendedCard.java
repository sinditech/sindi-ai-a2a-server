/**
 * 
 */
package za.co.sindi.ai.a2a.server.spi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * @author Buhake Sindi
 * @since 10 November 2025
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface ExtendedCard {
	
	/**
	 * A human-readable name for the agent.
	 * @return
	 */
	String name();
	
	/**
	 * A human-readable description of the agent, assisting users and other agents
	 * in understanding its purpose.
	 * 
	 * @return
	 */
	String description();

	/**
	 * The agent's own version number. The format is defined by the provider.
	 * 
	 * @return
	 */
	String version();
	
	/**
	 * Default set of supported input MIME types for all skills, which can be
	 * overridden on a per-skill basis.
	 * @return
	 */
	String[] defaultInputModes() default {};
	
	/**
	 * Default set of supported output MIME types for all skills, which can be
	 * overridden on a per-skill basis.
	 * @return
	 */
	String[] defaultOutputModes() default {};
}
