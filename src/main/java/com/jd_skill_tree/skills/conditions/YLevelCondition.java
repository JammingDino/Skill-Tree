package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.GsonHelper;

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
    public boolean test(Player player) {
        int playerY = player.blockPosition().getY();
        return switch (this.comparison) {
            case GREATER_THAN -> playerY > this.targetY;
            case LESS_THAN -> playerY < this.targetY;
            case EQUAL_TO -> playerY == this.targetY;
        };
    }

    public Comparison getComparison() { return comparison; }
    public int getTargetY() { return targetY; }

    public static YLevelCondition fromJson(JsonObject json) {
        String compStr = GsonHelper.getString(json, "comparison", "GREATER_THAN").toUpperCase();
        Comparison comp;
        try {
            comp = Comparison.valueOf(compStr);
        } catch (IllegalArgumentException e) {
            comp = Comparison.GREATER_THAN;
        }
        int y = GsonHelper.getInt(json, "y_level", 64);
        return new YLevelCondition(comp, y);
    }
}