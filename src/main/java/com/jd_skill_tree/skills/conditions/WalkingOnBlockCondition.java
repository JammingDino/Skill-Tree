package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class WalkingOnBlockCondition implements SkillCondition {
    private final Block targetBlock;

    public WalkingOnBlockCondition(Block targetBlock) {
        this.targetBlock = targetBlock;
    }

    @Override
    public boolean test(PlayerEntity player) {
        // getSteppingBlockState is accurate for the block the player is standing on
        return player.getSteppingBlockState().isOf(targetBlock);
    }

    public static WalkingOnBlockCondition fromJson(JsonObject json) {
        Identifier id = new Identifier(JsonHelper.getString(json, "block"));
        return new WalkingOnBlockCondition(Registries.BLOCK.get(id));
    }

    public Block getTargetBlock() { return targetBlock; }
}