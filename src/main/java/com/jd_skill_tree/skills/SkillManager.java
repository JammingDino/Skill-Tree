package com.jd_skill_tree.skills;

import com.jd_skill_tree.skills.effects.AttributeSkillEffect;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public class SkillManager {
    private static final Map<Identifier, Skill> SKILLS = new HashMap<>();
    // NEW: A cache of every single AttributeSkillEffect from all loaded skills.
    private static final Set<EntityAttribute> AFFECTED_ATTRIBUTES = new HashSet<>();

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

    public static Set<EntityAttribute> getAffectedAttributes() {
        return AFFECTED_ATTRIBUTES;
    }


    public static Optional<Skill> getSkill(Identifier id) {
        return Optional.ofNullable(SKILLS.get(id));
    }

    public static Collection<Skill> getAllSkills() {
        return SKILLS.values();
    }
}