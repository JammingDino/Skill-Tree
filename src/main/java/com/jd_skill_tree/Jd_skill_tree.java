package com.jd_skill_tree;

import com.jd_skill_tree.blocks.ModBlocks;
import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.networking.NetworkHandler;
import com.jd_skill_tree.skills.actions.SkillActionEffectType;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import com.jd_skill_tree.skills.effects.SkillEffectType;
import com.jd_skill_tree.utils.ActionScheduler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Jd_skill_tree.MOD_ID)
public class Jd_skill_tree {

    public static final String MOD_ID = "jd_skill_tree";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Jd_skill_tree() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandlers());
        MinecraftForge.EVENT_BUS.register(this);

        NetworkHandler.register();

        LOGGER.info(MOD_ID + " mod loading");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        SkillEffectType.registerAll();
        SkillActionEffectType.registerAll();
        SkillConditionType.registerAll();
        ActionScheduler.register();
    }
}