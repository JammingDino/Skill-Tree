package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;

public class YLevelCondition implements SkillCondition {

    public enum Comparison {
        GREATER_THAN,
        LESS_THAN,
        EQUAL_TO
    }

    private final Comparison comparison;
    private final int targetY;

    public YLevelCondition(Comparison comparison, int targetY) {
        this.comparison = comparison;
        this.targetY = targetY;
    }

    @Override
    public boolean test(PlayerEntity player) {
        int playerY = player.getBlockPos().getY();
        return switch (this.comparison) {
            case GREATER_THAN -> playerY > this.targetY;
            case LESS_THAN -> playerY < this.targetY;
            case EQUAL_TO -> playerY == this.targetY;
        };
    }

    public Comparison getComparison() { return comparison; }
    public int getTargetY() { return targetY; }

    public static YLevelCondition fromJson(JsonObject json) {
        String compStr = JsonHelper.getString(json, "comparison", "GREATER_THAN").toUpperCase();
        Comparison comp;
        try {
            comp = Comparison.valueOf(compStr);
        } catch (IllegalArgumentException e) {
            comp = Comparison.GREATER_THAN;
        }
        int y = JsonHelper.getInt(json, "y_level", 64);
        return new YLevelCondition(comp, y);
    }
}