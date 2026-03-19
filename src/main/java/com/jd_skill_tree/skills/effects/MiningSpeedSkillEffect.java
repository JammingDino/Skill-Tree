package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

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
    public float modifyBreakSpeed(BlockState block, Player player, float currentSpeed) {
        return currentSpeed * (1.0f + this.multiplier);
    }

    public static MiningSpeedSkillEffect fromJson(JsonObject json) {
        float value = GsonHelper.getAsFloat(json, "value", 0);

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