package com.jd_skill_tree;

import com.jd_skill_tree.api.IUnlockedSkillsData; // Import this
import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.blocks.entity.renderer.SkillAltarBlockEntityRenderer;
import com.jd_skill_tree.client.ClientBlockInteractionHandler;
import com.jd_skill_tree.networking.SkillNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jd_skill_tree.skills.ClientSkillData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.HashSet;
import java.util.Set;

public class Jd_skill_tree_client implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(Jd_skill_tree.MOD_ID);

    public static final EntityModelLayer BOOK_LAYER = new EntityModelLayer(
            new Identifier(Jd_skill_tree.MOD_ID, "skill_altar_book"), "main"
    );

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing client for JD Skill Tree Mod");

        Jd_skill_tree.CLIENT_SKILL_DATA_HANDLER = ClientSkillData::isSkillUnlocked;
        registerS2CPackets();

        BlockEntityRendererFactories.register(ModBlockEntities.SKILL_ALTAR_ENTITY, SkillAltarBlockEntityRenderer::new);
        ClientBlockInteractionHandler.register();

        LOGGER.info("Client initialization complete for JD Skill Tree Mod");
    }

    private void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SkillNetworking.SKILL_SYNC_PACKET_ID, (client, handler, buf, responseSender) -> {
            int size = buf.readInt();
            Set<String> unlockedSkills = new HashSet<>();
            for (int i = 0; i < size; i++) {
                unlockedSkills.add(buf.readString());
            }

            // This must be run on the client's main thread
            client.execute(() -> {
                // 1. Update the static helper (for GUI access)
                ClientSkillData.setUnlockedSkills(unlockedSkills);

                // 2. CRITICAL FIX: Update the local player entity (for Mixin/Effect access)
                if (client.player != null) {
                    ((IUnlockedSkillsData) client.player).setUnlockedSkills(unlockedSkills);
                    // Optional: Print to console to verify it's working
                    // System.out.println("Updated client player skills. Count: " + unlockedSkills.size());
                }
            });
        });
    }
}