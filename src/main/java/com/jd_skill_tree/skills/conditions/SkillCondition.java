package com.jd_skill_tree.skills.conditions;

import net.minecraft.entity.player.PlayerEntity;

public interface SkillCondition {
    /**
     * Checks if the condition is met.
     * @param player The player to check.
     * @return true if the condition is satisfied.
     */
    boolean test(PlayerEntity player);
}