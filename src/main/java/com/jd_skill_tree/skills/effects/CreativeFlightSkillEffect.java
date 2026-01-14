package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;

public class CreativeFlightSkillEffect implements SkillEffect {

    private final SkillCondition condition;

    public CreativeFlightSkillEffect(SkillCondition condition) {
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    public static CreativeFlightSkillEffect fromJson(JsonObject json) {
        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }
        return new CreativeFlightSkillEffect(cond);
    }
}