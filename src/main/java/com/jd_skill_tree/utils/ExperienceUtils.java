package com.jd_skill_tree.utils;

import net.minecraft.entity.player.PlayerEntity;

public class ExperienceUtils {

    /**
     * Calculates the total amount of XP points a player currently has.
     * Minecraft stores XP as (Level) + (Progress Bar %).
     * This converts that into a single raw integer.
     */
    public static int getPlayerTotalXp(PlayerEntity player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        // 1. Calculate XP required to reach the current level (Base XP)
        long baseXp = getXpForLevel(level);

        // 2. Calculate XP currently in the bar (Progress * XP for next level)
        int xpToNextLevel = player.getNextLevelExperience();
        int barXp = (int) (progress * xpToNextLevel);

        return (int) (baseXp + barXp);
    }

    /**
     * Vanilla Minecraft formula for total XP required to reach a specific level.
     */
    public static long getXpForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) {
            return (long) (Math.pow(level, 2) + 6L * level);
        } else if (level <= 31) {
            return (long) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else {
            return (long) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }
    }
}