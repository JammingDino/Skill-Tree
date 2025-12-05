package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class HealthCondition implements SkillCondition {

    public enum Comparison {
        GREATER_THAN,
        LESS_THAN,
        EQUAL_TO
    }

    private final Comparison comparison;
    private final float targetHealth;

    public HealthCondition(Comparison comparison, float targetHealth) {
        this.comparison = comparison;
        this.targetHealth = targetHealth;
    }

    @Override
    public boolean test(PlayerEntity player) {
        float currentHealth = player.getHealth(); // 20.0f = 10 Hearts
        return switch (this.comparison) {
            case GREATER_THAN -> currentHealth > this.targetHealth;
            case LESS_THAN -> currentHealth < this.targetHealth;
            case EQUAL_TO -> Math.abs(currentHealth - this.targetHealth) < 0.01f; // Float precision check
        };
    }

    public Comparison getComparison() { return comparison; }
    public float getTargetHealth() { return targetHealth; }

    public static HealthCondition fromJson(JsonObject json) {
        String compStr = JsonHelper.getString(json, "comparison", "GREATER_THAN").toUpperCase();
        Comparison comp;
        try {
            comp = Comparison.valueOf(compStr);
        } catch (IllegalArgumentException e) {
            comp = Comparison.GREATER_THAN;
        }
        float amount = JsonHelper.getFloat(json, "amount", 20.0f);
        return new HealthCondition(comp, amount);
    }
}