package com.jd_skill_tree.skills;

import com.jd_skill_tree.skills.effects.AttributeSkillEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

public class SkillManager {
    private static final Map<ResourceLocation, Skill> SKILLS = new HashMap<>();
    // NEW: A cache of every single AttributeSkillEffect from all loaded skills.
    private static final Set<Attribute> AFFECTED_ATTRIBUTES = new HashSet<>();

    public static void clearSkills() {
        SKILLS.clear();
        AFFECTED_ATTRIBUTES.clear(); // Clear this cache on reload too
    }

    public static void addSkill(Skill skill) {
        if (skill != null && skill.getId() != null) {
            SKILLS.put(skill.getId(), skill);

            // NEW: When a skill is added, find any attribute effects and add them to our master list.
            skill.getEffects().stream()
                    .filter(effect -> effect instanceof AttributeSkillEffect)
                    .map(effect -> ((AttributeSkillEffect) effect).getAttribute())
                    .forEach(AFFECTED_ATTRIBUTES::add);
        }
    }

    public static Set<Attribute> getAffectedAttributes() {
        return AFFECTED_ATTRIBUTES;
    }


    public static Optional<Skill> getSkill(ResourceLocation id) {
        return Optional.ofNullable(SKILLS.get(id));
    }

    public static Collection<Skill> getAllSkills() {
        return SKILLS.values();
    }
}