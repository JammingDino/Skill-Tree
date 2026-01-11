package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class EffectImmunitySkillEffect implements SkillEffect {

    private final Identifier effectId;
    private final SkillCondition condition;

    public EffectImmunitySkillEffect(Identifier effectId, SkillCondition condition) {
        this.effectId = effectId;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    @Override
    public boolean preventsEffect(StatusEffect effect) {
        Identifier targetId = Registries.STATUS_EFFECT.getId(effect);
        return targetId != null && targetId.equals(this.effectId);
    }

    public Identifier getEffectId() {
        return effectId;
    }

    public static EffectImmunitySkillEffect fromJson(JsonObject json) {
        Identifier id = new Identifier(JsonHelper.getString(json, "effect"));

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new EffectImmunitySkillEffect(id, cond);
    }
}