package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class HungerCondition implements SkillCondition {

    public enum Comparison { GREATER_THAN, LESS_THAN, EQUAL_TO }

    private final Comparison comparison;
    private final int targetHunger;

    public HungerCondition(Comparison comparison, int targetHunger) {
        this.comparison = comparison;
        this.targetHunger = targetHunger;
    }

    @Override
    public boolean test(PlayerEntity player) {
        int currentHunger = player.getHungerManager().getFoodLevel();
        return switch (this.comparison) {
            case GREATER_THAN -> currentHunger > this.targetHunger;
            case LESS_THAN -> currentHunger < this.targetHunger;
            case EQUAL_TO -> currentHunger == this.targetHunger;
        };
    }

    public static HungerCondition fromJson(JsonObject json) {
        Comparison comp = Comparison.valueOf(JsonHelper.getString(json, "comparison", "GREATER_THAN").toUpperCase());
        int amount = JsonHelper.getInt(json, "amount", 20);
        return new HungerCondition(comp, amount);
    }

    public Comparison getComparison() { return comparison; }
    public int getTargetHunger() { return targetHunger; }
}