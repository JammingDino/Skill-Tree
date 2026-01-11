package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class MiningSpeedSkillEffect implements SkillEffect {

    private final float multiplier;
    private final SkillCondition condition;

    public MiningSpeedSkillEffect(float multiplier, SkillCondition condition) {
        this.multiplier = multiplier;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    @Override
    public float modifyBreakSpeed(BlockState block, PlayerEntity player, float currentSpeed) {
        return currentSpeed * (1.0f + this.multiplier);
    }

    public static MiningSpeedSkillEffect fromJson(JsonObject json) {
        float value = JsonHelper.getFloat(json, "value");

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new MiningSpeedSkillEffect(value, cond);
    }

    public float getMultiplier() {
        return this.multiplier;
    }
}