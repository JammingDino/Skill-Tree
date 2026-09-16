package com.jd_skill_tree.blocks;

import com.jd_skill_tree.Jd_skill_tree;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    // Define all skill altar tiers
    public static final Block SKILL_ALTAR = registerBlock("skill_altar",
            Block.Settings.create()
                    .strength(3.0f, 6.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .requiresTool()
                    .luminance(state -> 5),
            1 // Tier 1
    );

    public static final Block IRON_SKILL_ALTAR = registerBlock("iron_skill_altar",
            Block.Settings.create()
                    .strength(4.0f, 8.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .requiresTool()
                    .luminance(state -> 7),
            2 // Tier 2
    );

    public static final Block DIAMOND_SKILL_ALTAR = registerBlock("diamond_skill_altar",
            Block.Settings.create()
                    .strength(5.0f, 10.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .requiresTool()
                    .luminance(state -> 9),
            3 // Tier 3
    );

    public static final Block EMERALD_SKILL_ALTAR = registerBlock("emerald_skill_altar",
            Block.Settings.create()
                    .strength(5.0f, 10.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .requiresTool()
                    .luminance(state -> 10),
            4 // Tier 4
    );

    public static final Block OBSIDIAN_SKILL_ALTAR = registerBlock("obsidian_skill_altar",
            Block.Settings.create()
                    .strength(5.0f, 15.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .requiresTool()
                    .luminance(state -> 11),
            5 // Tier 4
    );

    public static final Block DEVELOPER_SKILL_ALTAR = registerBlock("developer_skill_altar",
            Block.Settings.create()
                    .strength(-1.0f, 3600000.0f) // Unbreakable (like bedrock) or just very hard
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                    .requiresTool()
                    .luminance(state -> 15), // Glowing bright
            99 // Special Tier ID for logic check
    );

    // Helper method to register a block.
    // 1.21.x note: Block id must be set on Settings before construction
    // (getLootTableKey() NPEs otherwise) — so blocks are built inline here.
    private static Block registerBlock(String name, net.minecraft.block.AbstractBlock.Settings settings, int tier) {
        Identifier id = Identifier.of(Jd_skill_tree.MOD_ID, name);
        Block block = new SkillAltar(settings.registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)), tier);
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, id, block);
    }

    // Helper method to register the block's item form.
    // 1.21.x note: Item id must be set on the Item Settings before construction.
    // 1.21.2+ note: BlockItem no longer delegates its name to the block (the old
    // BlockItem#getTranslationKey override is gone), so an item built without an
    // explicit key defaults to the item-prefixed one ("item.jd_skill_tree.<name>")
    // and the GUI shows that raw key. useBlockPrefixedTranslationKey() is what
    // vanilla's own Items.register does for every block item, and it reuses the
    // block.* keys already in our en_us.json.
    private static Item registerBlockItem(String name, Block block) {
        Identifier id = Identifier.of(Jd_skill_tree.MOD_ID, name);
        return Registry.register(Registries.ITEM, id,
                new BlockItem(block, new Item.Settings()
                        .registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
                        .useBlockPrefixedTranslationKey()));
    }

    // Call this method in your main mod initializer
    public static void registerModBlocks() {
        Jd_skill_tree.LOGGER.info("Registering blocks for " + Jd_skill_tree.MOD_ID);

        // Add all altar tiers to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(SKILL_ALTAR);
            entries.add(IRON_SKILL_ALTAR);
            entries.add(DIAMOND_SKILL_ALTAR);
            entries.add(EMERALD_SKILL_ALTAR);
            entries.add(OBSIDIAN_SKILL_ALTAR);
            entries.add(DEVELOPER_SKILL_ALTAR);
        });
    }
}