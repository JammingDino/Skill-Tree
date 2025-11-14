package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class MiningSpeedSkillEffect implements SkillEffect {

    private final float multiplier;

    public MiningSpeedSkillEffect(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public float modifyBreakSpeed(BlockState block, PlayerEntity player, float currentSpeed) {
        // We add 1 to the multiplier so a value of 0.2 means "20% faster"
        return currentSpeed * (1.0f + this.multiplier);
    }

    /**
     * Factory for creating a MiningSpeedSkillEffect from JSON.
     */
    public static MiningSpeedSkillEffect fromJson(JsonObject json) {
        float value = JsonHelper.getFloat(json, "value");
        return new MiningSpeedSkillEffect(value);
    }
}