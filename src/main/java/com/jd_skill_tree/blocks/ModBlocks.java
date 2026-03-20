package com.jd_skill_tree.blocks;

import com.jd_skill_tree.Jd_skill_tree;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Jd_skill_tree.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Jd_skill_tree.MOD_ID);

    public static final RegistryObject<Block> SKILL_ALTAR = registerBlock("skill_altar",
            new SkillAltar(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 5),
                    1));

    public static final RegistryObject<Block> IRON_SKILL_ALTAR = registerBlock("iron_skill_altar",
            new SkillAltar(BlockBehaviour.Properties.of()
                    .strength(4.0f, 8.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7),
                    2));

    public static final RegistryObject<Block> DIAMOND_SKILL_ALTAR = registerBlock("diamond_skill_altar",
            new SkillAltar(BlockBehaviour.Properties.of()
                    .strength(5.0f, 10.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 9),
                    3));

    public static final RegistryObject<Block> EMERALD_SKILL_ALTAR = registerBlock("emerald_skill_altar",
            new SkillAltar(BlockBehaviour.Properties.of()
                    .strength(5.0f, 10.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 10),
                    4));

    public static final RegistryObject<Block> OBSIDIAN_SKILL_ALTAR = registerBlock("obsidian_skill_altar",
            new SkillAltar(BlockBehaviour.Properties.of()
                    .strength(5.0f, 15.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 11),
                    5));

    public static final RegistryObject<Block> DEVELOPER_SKILL_ALTAR = registerBlock("developer_skill_altar",
            new SkillAltar(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 15),
                    99));

    private static RegistryObject<Block> registerBlock(String name, Block block) {
        RegistryObject<Block> registeredBlock = BLOCKS.register(name, () -> block);
        ITEMS.register(name, () -> new BlockItem(block, new Item.Properties()));
        return registeredBlock;
    }
}