package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;

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
    public int modifyExperience(Player player, int experience) {
        return (int) Math.max(0, experience * (1.0f + this.multiplier));
    }

    public float getMultiplier() {
        return multiplier;
    }

    public static ExperienceSkillEffect fromJson(JsonObject json) {
        float val = GsonHelper.getAsFloat(json, "value", 0);

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new ExperienceSkillEffect(val, cond);
    }
}