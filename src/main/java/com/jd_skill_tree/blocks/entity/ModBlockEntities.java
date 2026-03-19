package com.jd_skill_tree.blocks.entity;

import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.blocks.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Jd_skill_tree.MOD_ID);

    public static final RegistryObject<BlockEntityType<SkillAltarBlockEntity>> SKILL_ALTAR_ENTITY =
            BLOCK_ENTITY_TYPES.register("skill_altar_entity", () ->
                    BlockEntityType.Builder.of(
                            SkillAltarBlockEntity::new,
                            ModBlocks.SKILL_ALTAR.get(),
                            ModBlocks.IRON_SKILL_ALTAR.get(),
                            ModBlocks.DIAMOND_SKILL_ALTAR.get(),
                            ModBlocks.EMERALD_SKILL_ALTAR.get(),
                            ModBlocks.OBSIDIAN_SKILL_ALTAR.get(),
                            ModBlocks.DEVELOPER_SKILL_ALTAR.get()
                    ).build(null));
}