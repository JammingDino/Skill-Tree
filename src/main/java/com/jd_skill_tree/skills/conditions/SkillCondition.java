package com.jd_skill_tree.skills.conditions;

import net.minecraft.world.entity.player.Player;

public interface SkillCondition {
    /**
     * Checks if the condition is met.
     * @param player The player to check.
     * @return true if the condition is satisfied.
     */
    boolean test(Player player);
}