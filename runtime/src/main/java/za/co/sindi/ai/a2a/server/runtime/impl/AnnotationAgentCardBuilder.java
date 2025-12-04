/**
 * 
 */
package za.co.sindi.ai.a2a.server.runtime.impl;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import za.co.sindi.ai.a2a.server.runtime.PublicAgentCardBuilder;
import za.co.sindi.ai.a2a.server.spi.Agent;
import za.co.sindi.ai.a2a.server.spi.AgentCardBuilder;
import za.co.sindi.ai.a2a.server.spi.Skill;
import za.co.sindi.ai.a2a.types.AgentSkill;
import za.co.sindi.commons.utils.Preconditions;
import za.co.sindi.commons.utils.Strings;

/**
 * @author Buhake Sindi
 * @since 29 November 2025
 */
public class AnnotationAgentCardBuilder {
	
	private AnnotationAgentCardBuilder() {
		throw new AssertionError("Private constructor.");
	}
	
	public static PublicAgentCardBuilder createPublicAgentCardBuilder(final Class<?> clazz) {
		PublicAgentCardBuilder builder = new PublicAgentCardBuilderImpl();
		build(clazz, builder);
		return builder;
	}

	public static void build(final Class<?> clazz, final AgentCardBuilder builder) {
		Preconditions.checkArgument(clazz != null, "An Agent class is required.");
		Preconditions.checkArgument(builder != null, "An Agent card builder is required.");
		
		Agent agentAnnotation = clazz.getAnnotation(Agent.class);
		if (agentAnnotation != null) {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(agentAnnotation.name()), "A human-readable agent name is required on @Agent.");
			Preconditions.checkArgument(!Strings.isNullOrEmpty(agentAnnotation.description()), "A human-readable agent description is required on @Agent.");
			Preconditions.checkArgument(!Strings.isNullOrEmpty(agentAnnotation.version()), "A human-readable agent version is required on @Agent.");
			
			Map<String, AgentSkill> agentSkills = retrieveAgentSkills(clazz);
			Preconditions.checkState(agentSkills != null && !agentSkills.isEmpty(), "No @Skill annotation found in any declared method(s). An agent requires, at least, 1 skill.");
			
			builder.name(agentAnnotation.name())
				   .description(agentAnnotation.description())
				   .version(agentAnnotation.version())
				   .skills(agentSkills.values().stream().collect(Collectors.toSet()))
				   .defaultInputModes(agentAnnotation.defaultInputModes())
				   .defaultOutputModes(agentAnnotation.defaultOutputModes());
			
			if (builder instanceof PublicAgentCardBuilder pacb) {
				pacb.supportsAuthenticatedExtendedCard(agentAnnotation.supportsAuthenticatedExtendedCard());
			}
		}
	}
	
	public static boolean supportsAuthenticatedExtendedCard(final Class<?> clazz) {
		Agent agentAnnotation = clazz.getAnnotation(Agent.class);
		return agentAnnotation != null ? agentAnnotation.supportsAuthenticatedExtendedCard() : false; 
	}
	
	private static Map<String, AgentSkill> retrieveAgentSkills(final Class<?> clazz) {
		Map<String, AgentSkill> skills = new LinkedHashMap<>();
		for (Method declaredMethod : clazz.getDeclaredMethods()) {
			Skill skillAnnotation = declaredMethod.getAnnotation(Skill.class);
			if (skillAnnotation == null) continue;
			Preconditions.checkArgument(!Strings.isNullOrEmpty(skillAnnotation.name()), "A human-readable agent skill name is required on @Skill.");
			Preconditions.checkArgument(!Strings.isNullOrEmpty(skillAnnotation.description()), "A human-readable agent skill description is required on @Skill.");
			Preconditions.checkArgument(skillAnnotation.tags() != null && skillAnnotation.tags().length > 0, "A human-readable agent skill tags is required on @Skill.");
			Preconditions.checkArgument(skillAnnotation.examples() != null && skillAnnotation.examples().length > 0, "A human-readable agent skill examples is required on @Skill.");
			if (skillAnnotation != null) {
				String id = skillAnnotation.id();
				if (Strings.isNullOrEmpty(id)) id = Strings.toKebabCase(declaredMethod.getName());
				Preconditions.checkState(!skills.containsKey(id), "An agent skill ID '" + id + "' already exist.");
				skills.put(id, new AgentSkill(id, skillAnnotation.name(), skillAnnotation.description(), skillAnnotation.tags()));
				skills.get(id).setExamples(skillAnnotation.examples());
				skills.get(id).setInputModes(skillAnnotation.inputModes());
				skills.get(id).setOutputModes(skillAnnotation.outputModes());
			}
		}
		
		return Collections.unmodifiableMap(skills);
	}
}
