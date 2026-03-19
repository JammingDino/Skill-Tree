package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.GsonHelper;

public class TimeOfDayCondition implements SkillCondition {
    private final int minTime;
    private final int maxTime;

    public TimeOfDayCondition(int minTime, int maxTime) {
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    @Override
    public boolean test(Player player) {
        long time = player.level().getTimeOfDay() % 24000;
        return time >= minTime && time <= maxTime;
    }

    public static TimeOfDayCondition fromJson(JsonObject json) {
        return new TimeOfDayCondition(
                GsonHelper.getInt(json, "min", 0),
                GsonHelper.getInt(json, "max", 24000)
        );
    }

    public int getMinTime() { return minTime; }
    public int getMaxTime() { return maxTime; }
}