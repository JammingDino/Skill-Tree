package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectImmunitySkillEffect implements SkillEffect {

    private final ResourceLocation effectId;
    private final SkillCondition condition;

    public EffectImmunitySkillEffect(ResourceLocation effectId, SkillCondition condition) {
        this.effectId = effectId;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    @Override
    public boolean preventsEffect(MobEffect effect) {
        ResourceLocation targetId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return targetId != null && targetId.equals(this.effectId);
    }

    public ResourceLocation getEffectId() {
        return effectId;
    }

    public static EffectImmunitySkillEffect fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "effect"));

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new EffectImmunitySkillEffect(id, cond);
    }
}