package com.jd_skill_tree.utils;

import com.jd_skill_tree.blocks.ModBlocks;
import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.skills.SkillLoader;
import com.jd_skill_tree.skills.actions.SkillActionHandler;
import com.jd_skill_tree.skills.actions.SkillActionType;
import com.jd_skill_tree.skills.effects.SkillEffectType;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import com.jd_skill_tree.command.SkillCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Central registry class that coordinates all mod registrations.
 */
public class ModRegistries {

    public static void registerAll() {

        SkillEffectType.registerAll();
        SkillActionType.registerAll();

        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SkillCommand.register(dispatcher));

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(SkillLoader.INSTANCE);

        SkillActionHandler.register();
    }
}