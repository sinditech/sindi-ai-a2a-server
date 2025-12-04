/**
 * 
 */
package za.co.sindi.ai.agent;

import jakarta.enterprise.context.ApplicationScoped;
import za.co.sindi.ai.a2a.server.spi.Agent;
import za.co.sindi.ai.a2a.server.spi.Skill;

/**
 * @author Buhake Sindi
 * @since 05 December 2025
 */
@ApplicationScoped
@Agent(name="Hello World Agent", description = "'Just a hello world agent", version = "1.0.0", defaultInputModes = { "text" }, defaultOutputModes = { "text" })
public class HelloWorldAgent {

	@Skill(id = "hello_world", name = "Returns hello world", description = "just returns hello world", tags = {"hello world"}, examples = {"hi", "hello world"})
	public String greet() {
		return "Hello world!";
	}
	
	public String greetMe(final String name) {
		return "Hello " + name + "!";
	}
}
