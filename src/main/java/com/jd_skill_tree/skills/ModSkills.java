package com.jd_skill_tree.skills;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REFACTORED - This class is now a compatibility layer.
 * It provides data to the old parts of the mod by reading from the new, data-driven SkillManager.
 * The goal is to eventually remove this class entirely once all other code is updated.
 */
public class ModSkills {

    // The old hardcoded maps. We still need them for the Mixin for now.
    // TODO: This is a temporary HACK. This logic will be replaced by a proper Effect System.
    private static final Map<String, Float> MINING_SPEED_BONUSES = new HashMap<>();
    private static final Map<String, Float> MOVEMENT_SPEED_BONUSES = new HashMap<>();
    private static final Map<String, Float> SNEAK_SPEED_BONUSES = new HashMap<>();

    /**
     * This method is called by the SkillLoader after skills are loaded.
     * It populates our temporary bonus maps.
     */
    public static void updateBonusMaps() {
        MINING_SPEED_BONUSES.clear();
        MOVEMENT_SPEED_BONUSES.clear();
        SNEAK_SPEED_BONUSES.clear();
    }

    // DELETED: The old registerSkills() method is gone.

    /**
     * REFACTORED: This now gets a skill from the SkillManager using an Identifier.
     */
    public static Optional<Skill> getSkillById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return SkillManager.getSkill(new Identifier(id));
    }

    /**
     * REFACTORED: This provides compatibility for code that used the old `getSkill` method.
     */
    public static Skill getSkill(String id) {
        return getSkillById(id).orElse(null);
    }

    /**
     * REFACTORED: This method now reads all skills from the SkillManager and filters them by tier.
     * The AltarScreen can still call this method without needing any changes!
     */
    public static List<Skill> getSkillsForTier(int tier) {
        return SkillManager.getAllSkills().stream()
                .filter(skill -> skill.getTier() <= tier)
                .collect(Collectors.toList());
    }

    /**
     * REFACTORED: Now delegates to the SkillManager.
     */
    public static List<Skill> getAllSkills() {
        return new ArrayList<>(SkillManager.getAllSkills());
    }


    // --- The methods below are temporarily kept for the PlayerEntityMixin ---
    // --- They will be removed when we implement the proper Effect System ---

    public static Map<String, Float> getMiningSpeedBonuses() {
        return MINING_SPEED_BONUSES;
    }

    public static Map<String, Float> getMovementSpeedBonuses() {
        return MOVEMENT_SPEED_BONUSES;
    }

    public static Map<String, Float> getSneakSpeedBonuses() {
        return SNEAK_SPEED_BONUSES;
    }
}