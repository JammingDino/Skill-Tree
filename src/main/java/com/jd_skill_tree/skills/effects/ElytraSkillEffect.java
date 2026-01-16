package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;

public class ElytraSkillEffect implements SkillEffect {

    private final SkillCondition condition;

    public ElytraSkillEffect(SkillCondition condition) {
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    public static ElytraSkillEffect fromJson(JsonObject json) {
        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }
        return new ElytraSkillEffect(cond);
    }
}