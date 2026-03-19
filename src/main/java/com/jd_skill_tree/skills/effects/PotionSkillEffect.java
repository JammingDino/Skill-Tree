package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public class PotionSkillEffect implements SkillEffect {

    private final ResourceLocation effectId;
    private final int amplifier;
    private final boolean hideParticles;
    private final SkillCondition condition;

    public PotionSkillEffect(ResourceLocation effectId, int amplifier, boolean hideParticles, SkillCondition condition) {
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
    public void onTick(Player player) {
        if (player.level().isClientSide) return;

        if (!isActive(player)) return;

        MobEffect statusEffect = ForgeRegistries.MOB_EFFECTS.getValue(this.effectId);
        if (statusEffect == null) return;

        if (!player.hasEffect(statusEffect) || player.getEffect(statusEffect).getDuration() < 100) {
            boolean visible = !this.hideParticles;
            player.addEffect(new MobEffectInstance(statusEffect, 300, this.amplifier, false, visible, true));
        }
    }

    public ResourceLocation getEffectId() { return effectId; }
    public int getAmplifier() { return amplifier; }
    public boolean shouldHideParticles() { return hideParticles; }

    public static PotionSkillEffect fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "effect"));
        int amp = GsonHelper.getAsInt(json, "amplifier", 0);
        boolean hide = GsonHelper.getAsBoolean(json, "hide_particles", false);

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new PotionSkillEffect(id, amp, hide, cond);
    }
}