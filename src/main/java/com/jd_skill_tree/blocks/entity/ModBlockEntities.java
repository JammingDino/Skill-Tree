package com.jd_skill_tree.blocks.entity;

import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.blocks.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<SkillAltarBlockEntity> SKILL_ALTAR_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    new Identifier(Jd_skill_tree.MOD_ID, "skill_altar_entity"),
                    FabricBlockEntityTypeBuilder.create(
                            SkillAltarBlockEntity::new,
                            ModBlocks.SKILL_ALTAR,
                            ModBlocks.IRON_SKILL_ALTAR,
                            ModBlocks.DIAMOND_SKILL_ALTAR,
                            ModBlocks.EMERALD_SKILL_ALTAR,
                            ModBlocks.OBSIDIAN_SKILL_ALTAR,
                            ModBlocks.DEVELOPER_SKILL_ALTAR
                    ).build()
            );

    public static void registerBlockEntities() {
        Jd_skill_tree.LOGGER.info("Registering block entities for " + Jd_skill_tree.MOD_ID);
    }
}