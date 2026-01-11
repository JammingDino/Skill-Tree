package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class PotionSkillEffect implements SkillEffect {

    private final Identifier effectId;
    private final int amplifier;
    private final boolean hideParticles;
    private final SkillCondition condition;

    public PotionSkillEffect(Identifier effectId, int amplifier, boolean hideParticles, SkillCondition condition) {
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.hideParticles = hideParticles;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    @Override
    public void onTick(PlayerEntity player) {
        if (player.getWorld().isClient) return;

        // Vital: We must manually check the condition here because this method is called directly
        if (!isActive(player)) return;

        StatusEffect statusEffect = Registries.STATUS_EFFECT.get(this.effectId);
        if (statusEffect == null) return;

        if (!player.hasStatusEffect(statusEffect) || player.getStatusEffect(statusEffect).getDuration() < 100) {
            boolean visible = !this.hideParticles;
            player.addStatusEffect(new StatusEffectInstance(statusEffect, 300, this.amplifier, false, visible, true));
        }
    }

    public Identifier getEffectId() { return effectId; }
    public int getAmplifier() { return amplifier; }
    public boolean shouldHideParticles() { return hideParticles; }

    public static PotionSkillEffect fromJson(JsonObject json) {
        Identifier id = new Identifier(JsonHelper.getString(json, "effect"));
        int amp = JsonHelper.getInt(json, "amplifier", 0);
        boolean hide = JsonHelper.getBoolean(json, "hide_particles", false);

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new PotionSkillEffect(id, amp, hide, cond);
    }
}