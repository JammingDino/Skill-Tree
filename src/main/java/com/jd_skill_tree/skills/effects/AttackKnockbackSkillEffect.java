package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class AttackKnockbackSkillEffect implements SkillEffect {

    private final float amount;
    private final SkillCondition condition;

    public AttackKnockbackSkillEffect(float amount, SkillCondition condition) {
        this.amount = amount;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    @Override
    public float modifyAttackKnockback(PlayerEntity player, float currentKnockback) {
        return currentKnockback + this.amount;
    }

    public float getAmount() {
        return amount;
    }

    public static AttackKnockbackSkillEffect fromJson(JsonObject json) {
        float val = JsonHelper.getFloat(json, "value");

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new AttackKnockbackSkillEffect(val, cond);
    }
}