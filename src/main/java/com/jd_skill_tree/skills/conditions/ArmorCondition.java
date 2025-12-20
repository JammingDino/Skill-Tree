package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class ArmorCondition implements SkillCondition {

    public enum Comparison { GREATER_THAN, LESS_THAN, EQUAL_TO }

    private final Comparison comparison;
    private final int targetArmor;

    public ArmorCondition(Comparison comparison, int targetArmor) {
        this.comparison = comparison;
        this.targetArmor = targetArmor;
    }

    @Override
    public boolean test(PlayerEntity player) {
        int currentArmor = player.getArmor();
        return switch (this.comparison) {
            case GREATER_THAN -> currentArmor > this.targetArmor;
            case LESS_THAN -> currentArmor < this.targetArmor;
            case EQUAL_TO -> currentArmor == this.targetArmor;
        };
    }

    public static ArmorCondition fromJson(JsonObject json) {
        Comparison comp = Comparison.valueOf(JsonHelper.getString(json, "comparison", "GREATER_THAN").toUpperCase());
        int amount = JsonHelper.getInt(json, "amount", 0);
        return new ArmorCondition(comp, amount);
    }

    public Comparison getComparison() { return comparison; }
    public int getTargetArmor() { return targetArmor; }
}