package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
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

    public PotionSkillEffect(Identifier effectId, int amplifier, boolean hideParticles) {
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.hideParticles = hideParticles;
    }

    @Override
    public void onTick(PlayerEntity player) {
        if (player.getWorld().isClient) return;

        StatusEffect statusEffect = Registries.STATUS_EFFECT.get(this.effectId);
        if (statusEffect == null) return;

        // Reapply if missing or running out (< 5 seconds / 100 ticks remaining)
        // We apply for 15 seconds (300 ticks) to ensure a smooth overlap without flickering
        if (!player.hasStatusEffect(statusEffect) || player.getStatusEffect(statusEffect).getDuration() < 100) {

            boolean visible = !this.hideParticles; // "Visible" means particles are SHOWN

            // Duration: 300 ticks. Ambient: false. Visible: calculated. ShowIcon: true.
            player.addStatusEffect(new StatusEffectInstance(statusEffect, 300, this.amplifier, false, visible, true));
        }
    }

    public Identifier getEffectId() { return effectId; }
    public int getAmplifier() { return amplifier; }
    public boolean shouldHideParticles() { return hideParticles; }

    /**
     * Factory for creating a PotionSkillEffect from JSON.
     */
    public static PotionSkillEffect fromJson(JsonObject json) {
        Identifier id = new Identifier(JsonHelper.getString(json, "effect"));
        int amp = JsonHelper.getInt(json, "amplifier", 0);
        boolean hide = JsonHelper.getBoolean(json, "hide_particles", false);
        return new PotionSkillEffect(id, amp, hide);
    }
}