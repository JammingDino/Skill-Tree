package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class SwimSpeedSkillEffect implements SkillEffect {

    private final float multiplier;

    public SwimSpeedSkillEffect(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public float modifySwimSpeed(PlayerEntity player, float currentSpeed) {
        // Multiplier logic: 1.0 = +100% speed
        return currentSpeed * (1.0f + this.multiplier);
    }

    public float getMultiplier() {
        return multiplier;
    }

    public static SwimSpeedSkillEffect fromJson(JsonObject json) {
        float val = JsonHelper.getFloat(json, "value");
        return new SwimSpeedSkillEffect(val);
    }
}