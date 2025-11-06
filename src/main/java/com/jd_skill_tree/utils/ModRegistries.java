package com.jd_skill_tree.utils;

import com.jd_skill_tree.blocks.ModBlocks;
import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.skills.ModSkills;

/**
 * Central registry class that coordinates all mod registrations.
 */
public class ModRegistries {

    public static void registerAll() {

        ModSkills.registerSkills();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
    }
}