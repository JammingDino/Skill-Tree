package com.jd_skill_tree.utils;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.blocks.ModBlocks;
import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.skills.SkillLoader;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.actions.SkillActionEffectType;
import com.jd_skill_tree.skills.actions.SkillActionHandler;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import com.jd_skill_tree.skills.effects.ElytraSkillEffect;
import com.jd_skill_tree.skills.effects.SkillEffectType;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceType;
import com.jd_skill_tree.command.SkillCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;

/**
 * Central registry class that coordinates all mod registrations.
 */
public class ModRegistries {

    public static void registerAll() {

        SkillEffectType.registerAll();
        SkillActionEffectType.registerAll();
        SkillConditionType.registerAll();

        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SkillCommand.register(dispatcher));

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(SkillLoader.INSTANCE);

        SkillActionHandler.register();
        ActionScheduler.register();

        EntityElytraEvents.CUSTOM.register((entity, tick) -> {
            if (entity instanceof PlayerEntity player) {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                for (String skillId : skillData.getUnlockedSkills()) {
                    // We have to look up the skill to check effects/conditions
                    var skillOpt = SkillManager.getSkill(new Identifier(skillId));
                    if (skillOpt.isPresent()) {
                        for (var effect : skillOpt.get().getEffects()) {
                            if (effect instanceof ElytraSkillEffect && effect.isActive(player)) {
                                return true; // Allow gliding
                            }
                        }
                    }
                }
            }
            return false;
        });

    }
}