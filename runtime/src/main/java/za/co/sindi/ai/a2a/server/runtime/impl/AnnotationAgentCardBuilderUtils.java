/**
 * 
 */
package za.co.sindi.ai.a2a.server.runtime.impl;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import za.co.sindi.ai.a2a.server.runtime.ExtendedAgentCardBuilder;
import za.co.sindi.ai.a2a.server.runtime.PublicAgentCardBuilder;
import za.co.sindi.ai.a2a.server.spi.Agent;
import za.co.sindi.ai.a2a.server.spi.ExtendedCard;
import za.co.sindi.ai.a2a.server.spi.PublicCard;
import za.co.sindi.ai.a2a.server.spi.Skill;
import za.co.sindi.ai.a2a.types.AgentSkill;
import za.co.sindi.commons.utils.Preconditions;
import za.co.sindi.commons.utils.Strings;

/**
 * @author Buhake Sindi
 * @since 29 November 2025
 */
public class AnnotationAgentCardBuilderUtils {
	
	private AnnotationAgentCardBuilderUtils() {
		throw new AssertionError("Private constructor.");
	}
	
	public static PublicAgentCardBuilder createPublicAgentCardBuilder(final Class<?> clazz) {
		PublicAgentCardBuilder builder = new PublicAgentCardBuilderImpl();
		build(clazz, builder);
		return builder;
	}
	
	public static ExtendedAgentCardBuilder createExtendedAgentCardBuilder(final Class<?> clazz) {
		ExtendedAgentCardBuilder builder = new ExtendedAgentCardBuilderImpl();
		build(clazz, builder);
		return builder;
	}

	public static void build(final Class<?> clazz, final PublicAgentCardBuilder builder) {
		Preconditions.checkArgument(clazz != null, "An Agent class is required.");
		Preconditions.checkArgument(builder != null, "A Public Agent card builder is required.");
		
		Agent agentAnnotation = clazz.getAnnotation(Agent.class);
		if (agentAnnotation != null) {
			PublicCard publicCardAnnotation = clazz.getAnnotation(PublicCard.class);
			if (publicCardAnnotation != null) {
				Preconditions.checkArgument(!Strings.isNullOrEmpty(agentAnnotation.name()), "A human-readable agent name is required on @Agent.");
				Preconditions.checkArgument(!Strings.isNullOrEmpty(agentAnnotation.description()), "A human-readable agent description is required on @Agent.");
				Preconditions.checkArgument(!Strings.isNullOrEmpty(publicCardAnnotation.version()), "A human-readable agent version is required on @PublicCard.");
				
				Map<String, AgentSkill> agentSkills = retrieveAgentSkills(clazz, false);
				Preconditions.checkState(agentSkills != null && !agentSkills.isEmpty(), "No @Skill annotation found in any declared method(s). An agent requires, at least, 1 skill.");
				
				builder.name(agentAnnotation.name())
					   .description(agentAnnotation.description())
					   .version(publicCardAnnotation.version())
					   .skills(agentSkills.values().stream().collect(Collectors.toSet()))
					   .defaultInputModes(publicCardAnnotation.defaultInputModes())
					   .defaultOutputModes(publicCardAnnotation.defaultOutputModes());
			}
		}
	}
	
	public static void build(final Class<?> clazz, final ExtendedAgentCardBuilder builder) {
		Preconditions.checkArgument(clazz != null, "An Agent class is required.");
		Preconditions.checkArgument(builder != null, "An Extended Agent card builder is required.");
		
		Agent agentAnnotation = clazz.getAnnotation(Agent.class);
		if (agentAnnotation != null) {
			ExtendedCard extendedCardAnnotation = clazz.getAnnotation(ExtendedCard.class);
			if (extendedCardAnnotation != null) {
				String name = extendedCardAnnotation.name();
				String description = extendedCardAnnotation.description();
				if (Strings.isNullOrEmpty(name)) name = agentAnnotation.name();
				if (Strings.isNullOrEmpty(description)) name = agentAnnotation.description();
				Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "A human-readable agent name is required on for extended card.");
				Preconditions.checkArgument(!Strings.isNullOrEmpty(description), "A human-readable agent description is required for extended card.");
				Preconditions.checkArgument(!Strings.isNullOrEmpty(extendedCardAnnotation.version()), "A human-readable agent version is required on @ExtendedCard.");
				
				Map<String, AgentSkill> agentSkills = retrieveAgentSkills(clazz, true);
				Preconditions.checkState(agentSkills != null && !agentSkills.isEmpty(), "No @Skill annotation found in any declared method(s). An agent requires, at least, 1 skill.");
				
				builder.name(name)
					   .description(description)
					   .version(extendedCardAnnotation.version())
					   .skills(agentSkills.values().stream().collect(Collectors.toSet()))
					   .defaultInputModes(extendedCardAnnotation.defaultInputModes())
					   .defaultOutputModes(extendedCardAnnotation.defaultOutputModes());
			}
		}
	}
	
	private static Map<String, AgentSkill> retrieveAgentSkills(final Class<?> clazz, final boolean includeExtendedSkill) {
		Map<String, AgentSkill> skills = new LinkedHashMap<>();
		for (Method declaredMethod : clazz.getDeclaredMethods()) {
			Skill skillAnnotation = declaredMethod.getAnnotation(Skill.class);
			if (skillAnnotation == null || (!includeExtendedSkill && skillAnnotation.isExtended())) continue;
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
