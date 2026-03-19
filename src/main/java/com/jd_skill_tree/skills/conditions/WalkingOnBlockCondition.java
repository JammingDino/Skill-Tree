package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class WalkingOnBlockCondition implements SkillCondition {
    private final Block targetBlock;

    public WalkingOnBlockCondition(Block targetBlock) {
        this.targetBlock = targetBlock;
    }

    @Override
    public boolean test(Player player) {
        // getSteppingBlockState is accurate for the block the player is standing on
        return player.getBlockStateOn().isOf(targetBlock);
    }

    public static WalkingOnBlockCondition fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getString(json, "block"));
        return new WalkingOnBlockCondition(ForgeRegistries.BLOCKS.getValue(id));
    }

    public Block getTargetBlock() { return targetBlock; }
}