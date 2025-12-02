package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class LavaSpeedSkillEffect implements SkillEffect {

    private final float multiplier;

    public LavaSpeedSkillEffect(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public float modifyLavaSpeed(PlayerEntity player, float currentSpeed) {
        // Multiplier logic: 1.0 = +100% speed
        return currentSpeed * (1.0f + this.multiplier);
    }

    public float getMultiplier() {
        return multiplier;
    }

    public static LavaSpeedSkillEffect fromJson(JsonObject json) {
        float val = JsonHelper.getFloat(json, "value");
        return new LavaSpeedSkillEffect(val);
    }
}