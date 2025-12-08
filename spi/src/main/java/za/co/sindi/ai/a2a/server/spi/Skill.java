/**
 * 
 */
package za.co.sindi.ai.a2a.server.spi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * @author Buhake Sindi
 * @since 10 November 2025
 */
@Documented
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface Skill {
	
	/**
	 * A unique identifier for the agent's skill.
	 * @return
	 */
	String id();

	/**
	 * A human-readable name for the skill.
	 * @return
	 */
	String name();
	
	/**
	 * A detailed description of the skill, intended to help clients or users
	 * understand its purpose and functionality.
	 * 
	 * @return
	 */
	String description();
	
	/**
	 * A set of keywords describing the skill's capabilities.
	 * 
	 * @return
	 */
	String[] tags();
	
	/**
	 * Example prompts or scenarios that this skill can handle. Provides a hint to
	 * the client on how to use the skill.
	 * @return
	 */
	String[] examples();
	
	/**
	 * The set of supported input MIME types for this skill, overriding the agent's defaults.
	 * @return
	 */
	String[] inputModes() default {};
	
	/**
	 * The set of supported output MIME types for this skill, overriding the agent's defaults.
	 * @return
	 */
	String[] outputModes() default {};
	
	boolean isExtended() default false;
}
