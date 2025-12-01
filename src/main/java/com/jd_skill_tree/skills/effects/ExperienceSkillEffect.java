package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class ExperienceSkillEffect implements SkillEffect {

    private final float multiplier;

    public ExperienceSkillEffect(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public int modifyExperience(PlayerEntity player, int experience) {
        // Multiplier logic: 10 XP with 0.5 multiplier becomes 15 XP.
        // 10 XP with -0.2 multiplier becomes 8 XP.
        // We use Math.max(0) to ensure we don't accidentally drain XP if the multiplier is below -1.0
        return (int) Math.max(0, experience * (1.0f + this.multiplier));
    }

    public float getMultiplier() {
        return multiplier;
    }

    public static ExperienceSkillEffect fromJson(JsonObject json) {
        float val = JsonHelper.getFloat(json, "value");
        return new ExperienceSkillEffect(val);
    }
}