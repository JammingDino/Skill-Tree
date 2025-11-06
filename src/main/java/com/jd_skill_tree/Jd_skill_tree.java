package com.jd_skill_tree;

import com.jd_skill_tree.utils.ModRegistries;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jd_skill_tree implements ModInitializer {

	public static final String MOD_ID = "jd_skill_tree";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        LOGGER.info("Initializing " + MOD_ID);

        ModRegistries.registerAll();

		LOGGER.info(MOD_ID + " Initialized");
	}
}