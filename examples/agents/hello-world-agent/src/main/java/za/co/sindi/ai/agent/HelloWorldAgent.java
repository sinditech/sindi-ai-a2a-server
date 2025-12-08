/**
 * 
 */
package za.co.sindi.ai.agent;

import jakarta.enterprise.context.ApplicationScoped;
import za.co.sindi.ai.a2a.server.spi.Agent;
import za.co.sindi.ai.a2a.server.spi.ExtendedCard;
import za.co.sindi.ai.a2a.server.spi.PublicCard;
import za.co.sindi.ai.a2a.server.spi.Skill;

/**
 * @author Buhake Sindi
 * @since 05 December 2025
 */
@ApplicationScoped
@Agent(name="Hello World Agent", description = "Just a hello world agent")
@PublicCard(version = "1.0.0", defaultInputModes = { "text" }, defaultOutputModes = { "text" })
@ExtendedCard(name="Hello World Agent - Extended Edition", description = "The full-featured hello world agent for authenticated users.", version = "1.0.1")
public class HelloWorldAgent {

	@Skill(id = "hello_world", name = "Returns hello world", description = "just returns hello world", tags = {"hello world"}, examples = {"hi", "hello world"})
	public String greet() {
		return "Hello world!";
	}
	
	@Skill(id = "super_hello_world", name = "Returns a SUPER Hello World", description = "A more enthusiastic greeting, only for authenticated users.", tags = {"hello world", "super", "extended"}, examples = {"super hi", "give me a super hello"}, isExtended = true)
	public String greetMe(final String name) {
		return "Hello " + name + "!";
	}
}
