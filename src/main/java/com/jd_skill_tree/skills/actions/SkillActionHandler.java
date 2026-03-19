package com.jd_skill_tree.skills.actions;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class SkillActionHandler {

    public static void register() {
        // Block Break
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide) {
                // Target is Player, but Pos is Block location
                triggerActions(player, TriggerType.BLOCK_BREAK, player, world, pos);
            }
        });

        // Attack Entity
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide && !player.isSpectator()) {

                // 1. ATTACK_TARGET:
                // Owner = Player, Target = Zombie.
                // Commands use Zombie as source (@s = Zombie, ~ ~ ~ = Zombie Pos)
                triggerActions(player, TriggerType.ATTACK_TARGET, entity, world, entity.blockPosition());

                // 2. ATTACK_SELF:
                // Owner = Player, Target = Player.
                // Commands use Player as source (@s = Player, ~ ~ ~ = Player Pos)
                triggerActions(player, TriggerType.ATTACK_SELF, player, world, player.blockPosition());
            }
            return InteractionResult.PASS;
        });
    }

    public static void triggerActions(Player owner, TriggerType type, Entity target, Level world, BlockPos pos) {
        IUnlockedSkillsData skillData = (IUnlockedSkillsData) owner;

        for (String skillId : skillData.getUnlockedSkills()) {
            SkillManager.getSkill(new ResourceLocation(skillId)).ifPresent(skill -> {
                for (SkillAction action : skill.getActions()) {
                    if (action.getTrigger() == type) {
                        action.run(owner, target, world, pos);
                    }
                }
            });
        }
    }

    public static void handleTimerActions(Player player) {
        if (player.level().isClientSide) return;

        IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;

        for (String skillId : skillData.getUnlockedSkills()) {
            SkillManager.getSkill(new ResourceLocation(skillId)).ifPresent(skill -> {
                for (SkillAction action : skill.getActions()) {
                    // Check Trigger Type
                    if (action.getTrigger() == TriggerType.TIMER) {

                        // CRITICAL FIX: Modulo Check
                        int interval = Math.max(1, action.getInterval()); // Prevent div by zero
                        if (player.tickCount % interval == 0) {

                            // Only run if it's the correct tick
                            action.run(player, player, player.level(), player.blockPosition());
                        }
                    }
                }
            });
        }
    }

    /**
     * Triggers actions for a SPECIFIC skill only.
     * Used by the Radial Menu to fire just the selected skill.
     */
    public static void triggerSpecificSkill(Player owner, ResourceLocation skillId, TriggerType type, Entity target, Level world, BlockPos pos) {
        IUnlockedSkillsData data = (IUnlockedSkillsData) owner;

        // 1. CHECK COOLDOWN
        if (data.isSkillOnCooldown(skillId)) {
            // Optional: Play a "fail" sound or message
            return;
        }

        SkillManager.getSkill(skillId).ifPresent(skill -> {
            boolean actionsRan = false;

            for (SkillAction action : skill.getActions()) {
                if (action.getTrigger() == type) {
                    action.run(owner, target, world, pos);
                    actionsRan = true;
                }
            }

            // 2. APPLY COOLDOWN (Only if actions actually existed and ran)
            if (actionsRan && skill.getCooldown() > 0) {
                data.setSkillCooldown(skillId, skill.getCooldown());
            }
        });
    }
}