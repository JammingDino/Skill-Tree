package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class AttackKnockbackSkillEffect implements SkillEffect {

    private final float amount;

    public AttackKnockbackSkillEffect(float amount) {
        this.amount = amount;
    }

    @Override
    public float modifyAttackKnockback(PlayerEntity player, float currentKnockback) {
        // Simple addition. If amount is negative, it reduces knockback (or reverses it).
        return currentKnockback + this.amount;
    }

    public float getAmount() {
        return amount;
    }

    public static AttackKnockbackSkillEffect fromJson(JsonObject json) {
        // 0.5 = small push, 1.0 = like Knockback I, -1.0 = Pull
        float val = JsonHelper.getFloat(json, "value");
        return new AttackKnockbackSkillEffect(val);
    }
}