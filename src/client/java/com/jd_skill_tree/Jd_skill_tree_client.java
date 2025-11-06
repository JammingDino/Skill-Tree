package com.jd_skill_tree;

import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.blocks.entity.renderer.SkillAltarBlockEntityRenderer;
import com.jd_skill_tree.client.ClientBlockInteractionHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jd_skill_tree_client implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(Jd_skill_tree.MOD_ID);

    // Use a custom model layer for our skill altar book (not Minecraft's)
    public static final EntityModelLayer BOOK_LAYER = new EntityModelLayer(
            new Identifier(Jd_skill_tree.MOD_ID, "skill_altar_book"), "main"
    );

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing client for JD Skill Tree Mod");


        // Register block entity renderers
        BlockEntityRendererFactories.register(ModBlockEntities.SKILL_ALTAR_ENTITY, SkillAltarBlockEntityRenderer::new);

        // Register screen handlers (we'll add this in the next step)
        // registerScreenHandlers();
        ClientBlockInteractionHandler.register();

        LOGGER.info("Client initialization complete for JD Skill Tree Mod");
    }

    // We'll implement this method when we create the GUI
    // private void registerScreenHandlers() {
    //     HandledScreens.register(ModScreenHandlers.SKILL_TREE_SCREEN_HANDLER, SkillTreeScreen::new);
    // }
}