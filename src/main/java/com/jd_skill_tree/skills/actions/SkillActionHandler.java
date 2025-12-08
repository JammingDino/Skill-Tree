package com.jd_skill_tree.skills.actions;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;
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

        // REMOVED: UseBlockCallback logic.
        // Block Place is now handled precisely by BlockItemMixin.
    }

    // CHANGED: Changed from private to public static so the Mixin can call it
    public static void triggerActions(PlayerEntity player, World world, BlockPos pos, TriggerType type) {
        IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;

        for (String skillId : skillData.getUnlockedSkills()) {
            SkillManager.getSkill(new Identifier(skillId)).ifPresent(skill -> {

                if (!skill.areConditionsMet(player)) {
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