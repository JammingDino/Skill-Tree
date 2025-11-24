package com.jd_skill_tree;

import com.jd_skill_tree.api.IClientSkillData;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.utils.ModRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jd_skill_tree implements ModInitializer {

    public static final String MOD_ID = "jd_skill_tree";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static IClientSkillData CLIENT_SKILL_DATA_HANDLER = null;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing " + MOD_ID);

        // A single, clean call to set up all networking.
        SkillNetworking.register();

        // All other registrations are handled here.
        ModRegistries.registerAll();

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                for (net.minecraft.server.network.ServerPlayerEntity player : PlayerLookup.all(server)) {
                    SkillNetworking.syncSkillRegistry(player);
                    // Re-sync unlocked status too, just to be safe
                    SkillNetworking.syncSkillsToClient(player);
                }
            }
        });

        LOGGER.info(MOD_ID + " Initialized");
    }
}