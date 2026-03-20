package com.jd_skill_tree.skills.actions;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class SkillActionHandler {

    public static void register() {
        // No-op: Forge events handled in ForgeEventHandlers
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
                    if (action.getTrigger() == TriggerType.TIMER) {
                        int interval = Math.max(1, action.getInterval());
                        if (player.tickCount % interval == 0) {
                            action.run(player, player, player.level(), player.blockPosition());
                        }
                    }
                }
            });
        }
    }

    public static void triggerSpecificSkill(Player owner, ResourceLocation skillId, TriggerType type,
                                             Entity target, Level world, BlockPos pos) {
        IUnlockedSkillsData data = (IUnlockedSkillsData) owner;
        if (data.isSkillOnCooldown(skillId)) return;
        SkillManager.getSkill(skillId).ifPresent(skill -> {
            boolean actionsRan = false;
            for (SkillAction action : skill.getActions()) {
                if (action.getTrigger() == type) {
                    action.run(owner, target, world, pos);
                    actionsRan = true;
                }
            }
            if (actionsRan && skill.getCooldown() > 0) {
                data.setSkillCooldown(skillId, skill.getCooldown());
            }
        });
    }
}
