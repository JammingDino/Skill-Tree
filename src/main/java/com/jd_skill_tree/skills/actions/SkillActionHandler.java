package com.jd_skill_tree.skills.actions;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SkillActionHandler {

    /**
     * Registers the initial event listeners.
     * Called in ModRegistries.
     */
    public static void register() {
        // Listen for Block Break events
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient) {
                triggerActions(player, world, pos, TriggerType.BLOCK_BREAK);
            }
        });
    }

    /**
     * Handles event-based triggers (Block Break, Block Place).
     * This is also called from your Mixins (like BlockItemMixin).
     */
    public static void triggerActions(PlayerEntity player, World world, BlockPos pos, TriggerType type) {
        IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;

        // Iterate through all skills the player has unlocked
        for (String skillId : skillData.getUnlockedSkills()) {
            SkillManager.getSkill(new Identifier(skillId)).ifPresent(skill -> {

                // Only proceed if conditions (Dimension, Hunger, etc.) are met
                if (!skill.areConditionsMet(player)) {
                    return;
                }

                // Check all actions inside this skill
                for (SkillAction action : skill.getActions()) {
                    if (action.getTrigger() == type) {
                        action.run(player, world, pos);
                    }
                }
            });
        }
    }

    /**
     * Handles timer-based triggers.
     * Called from PlayerEntityMixin's tick method.
     */
    public static void handleTimerActions(PlayerEntity player) {
        if (player.getWorld().isClient) return;

        IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;

        for (String skillId : skillData.getUnlockedSkills()) {
            SkillManager.getSkill(new Identifier(skillId)).ifPresent(skill -> {

                if (!skill.areConditionsMet(player)) {
                    return;
                }

                for (SkillAction action : skill.getActions()) {
                    // Check if it's a timer and if it's the right tick to fire
                    if (action.getTrigger() == TriggerType.TIMER) {
                        if (player.age % Math.max(1, action.getInterval()) == 0) {
                            action.run(player, player.getWorld(), player.getBlockPos());
                        }
                    }
                }
            });
        }
    }
}