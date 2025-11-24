package com.jd_skill_tree;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.blocks.entity.renderer.SkillAltarBlockEntityRenderer;
import com.jd_skill_tree.client.ClientBlockInteractionHandler;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillLoader; // Needed for GSON
import com.jd_skill_tree.skills.SkillManager; // Needed to save skills
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
        // 1. Existing: Sync Unlocked Status
        ClientPlayNetworking.registerGlobalReceiver(SkillNetworking.SKILL_SYNC_PACKET_ID, (client, handler, buf, responseSender) -> {
            int size = buf.readInt();
            Set<String> unlockedSkills = new HashSet<>();
            for (int i = 0; i < size; i++) {
                unlockedSkills.add(buf.readString());
            }

            client.execute(() -> {
                ClientSkillData.setUnlockedSkills(unlockedSkills);
                if (client.player != null) {
                    ((IUnlockedSkillsData) client.player).setUnlockedSkills(unlockedSkills);
                }
            });
        });

        // 2. NEW: Sync Registry (Definitions)
        ClientPlayNetworking.registerGlobalReceiver(SkillNetworking.SKILL_REGISTRY_SYNC_PACKET_ID, (client, handler, buf, responseSender) -> {
            // We read the data into a temporary list first to avoid threading issues
            int count = buf.readInt();

            // Create a temporary list of skills to add
            java.util.List<Skill> receivedSkills = new java.util.ArrayList<>();

            for (int i = 0; i < count; i++) {
                Identifier id = buf.readIdentifier();
                String json = buf.readString();

                try {
                    // Deserialize using the SkillLoader's GSON to handle Adapters correctly
                    Skill skill = SkillLoader.GSON.fromJson(json, Skill.class);
                    skill.setId(id);
                    receivedSkills.add(skill);
                } catch (Exception e) {
                    LOGGER.error("Failed to deserialize skill sync for ID: " + id, e);
                }
            }

            // Execute on main thread
            client.execute(() -> {
                LOGGER.info("Received " + count + " skills from server.");
                // Clear existing and add new
                SkillManager.clearSkills();
                for (Skill skill : receivedSkills) {
                    SkillManager.addSkill(skill);
                }
                // Force update the legacy map helper just in case
                com.jd_skill_tree.skills.ModSkills.updateBonusMaps();
            });
        });
    }
}