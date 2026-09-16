package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
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

    // 1.20.5+: status effects are RegistryEntry<StatusEffect> in status effect instances.
    @Override
    public boolean preventsEffect(RegistryEntry<StatusEffect> effect) {
        Identifier targetId = effect.getKey().map(RegistryEntry.Reference::registryKey)
                .map(net.minecraft.registry.RegistryKey::getValue)
                .orElse(null);
        if (targetId == null) {
            // Directly-held value (dynamic entry): fall back to registry lookup
            targetId = Registries.STATUS_EFFECT.getId(effect.value());
        }
        return targetId != null && targetId.equals(this.effectId);
    }

    public Identifier getEffectId() {
        return effectId;
    }

    public static EffectImmunitySkillEffect fromJson(JsonObject json) {
        Identifier id = Identifier.of(JsonHelper.getString(json, "effect"));

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new EffectImmunitySkillEffect(id, cond);
    }
}