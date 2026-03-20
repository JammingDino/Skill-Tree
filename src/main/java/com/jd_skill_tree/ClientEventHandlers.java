package com.jd_skill_tree;

import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.blocks.entity.renderer.SkillAltarBlockEntityRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Jd_skill_tree.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandlers {

    public static final KeyMapping ACTIVATE_KEY = new KeyMapping(
            "key.jd_skill_tree.activate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.jd_skill_tree.general"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_KEY);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SKILL_ALTAR_ENTITY.get(),
                SkillAltarBlockEntityRenderer::new);
    }
}
