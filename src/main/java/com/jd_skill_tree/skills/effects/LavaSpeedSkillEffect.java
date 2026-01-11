package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class LavaSpeedSkillEffect implements SkillEffect {

    private final float multiplier;
    private final SkillCondition condition;

    public LavaSpeedSkillEffect(float multiplier, SkillCondition condition) {
        this.multiplier = multiplier;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    @Override
    public float modifyLavaSpeed(PlayerEntity player, float currentSpeed) {
        return currentSpeed * (1.0f + this.multiplier);
    }

    public float getMultiplier() {
        return multiplier;
    }

    public static LavaSpeedSkillEffect fromJson(JsonObject json) {
        float val = JsonHelper.getFloat(json, "value");

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new LavaSpeedSkillEffect(val, cond);
    }
}