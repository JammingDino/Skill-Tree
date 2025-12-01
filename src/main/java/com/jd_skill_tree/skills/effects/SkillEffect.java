package com.jd_skill_tree.skills.effects;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;

/**
 * The base interface for all skill effects.
 * Each effect can react to different game events by overriding these methods.
 */
public interface SkillEffect {

    /**
     * Called every tick for a player who has the skill unlocked.
     * Ideal for continuous effects like attribute modifiers or particle effects.
     */
    default void onTick(PlayerEntity player) {
        // Do nothing by default
    }

    /**
     * Called when the game calculates a player's block breaking speed.
     * Allows the effect to modify the speed.
     *
     * @param block The block being broken.
     * @param player The player breaking the block.
     * @param currentSpeed The breaking speed *after* all previous effects have been applied.
     * @return The new, modified breaking speed.
     */
    default float modifyBreakSpeed(BlockState block, PlayerEntity player, float currentSpeed) {
        // Return the speed unchanged by default
        return currentSpeed;
    }

    /**
     * Called when the player attacks a target to modify the knockback strength.
     * @param player The player attacking.
     * @param currentKnockback The current knockback strength (vanilla calculation).
     * @return The new knockback strength.
     */
    default float modifyAttackKnockback(PlayerEntity player, float currentKnockback) {
        return currentKnockback;
    }

    /**
     * Called when the player gains experience.
     * @param player The player gaining xp.
     * @param experience The amount of xp being gained.
     * @return The modified amount of xp.
     */
    default int modifyExperience(PlayerEntity player, int experience) {
        return experience;
    }

    // In the future, we can add more methods here, e.g.:
    // default void onDamage(PlayerEntity player, DamageSource source, float amount) {}
    // default void onJump(PlayerEntity player) {}
}