package com.jd_skill_tree.skills.actions;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SkillActionHandler {

    public static void register() {

        // Handle Block Break
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient) {
                triggerActions(player, world, pos, TriggerType.BLOCK_BREAK);
            }
        });

        // Handle Block Place / Interact
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && hand == Hand.MAIN_HAND) {
                // Note: This fires on right-click, even if no block is placed
                // System.out.println(">> EVENT: Right Click / Place Attempt");
                triggerActions(player, world, hitResult.getBlockPos().offset(hitResult.getSide()), TriggerType.BLOCK_PLACE);
            }
            return ActionResult.PASS;
        });
    }

    private static void triggerActions(PlayerEntity player, World world, BlockPos pos, TriggerType type) {
        IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;

        for (String skillId : skillData.getUnlockedSkills()) {
            SkillManager.getSkill(new Identifier(skillId)).ifPresent(skill -> {

                if (!skill.areConditionsMet(player)) {
                    // System.out.println("DEBUG: Conditions not met for skill " + skill.getName());
                    return;
                }

                for (SkillAction action : skill.getActions()) {
                    if (action.getTrigger() == type) {
                        action.execute(player, world, pos);
                    }
                }
            });
        }
    }
}