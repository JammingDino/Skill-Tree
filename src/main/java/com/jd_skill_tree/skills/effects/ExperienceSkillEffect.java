package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class ExperienceSkillEffect implements SkillEffect {

    private final float multiplier;
    private final SkillCondition condition;

    public ExperienceSkillEffect(float multiplier, SkillCondition condition) {
        this.multiplier = multiplier;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    @Override
    public int modifyExperience(PlayerEntity player, int experience) {
        return (int) Math.max(0, experience * (1.0f + this.multiplier));
    }

    public float getMultiplier() {
        return multiplier;
    }

    public static ExperienceSkillEffect fromJson(JsonObject json) {
        float val = JsonHelper.getFloat(json, "value");

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new ExperienceSkillEffect(val, cond);
    }
}