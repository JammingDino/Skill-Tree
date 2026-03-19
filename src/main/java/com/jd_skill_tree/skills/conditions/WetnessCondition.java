package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

public class WetnessCondition implements SkillCondition {
    @Override
    public boolean test(Player player) {
        // isWet() returns true for water, rain, or bubble columns
        return player.isWet();
    }

    public static WetnessCondition fromJson(JsonObject json) {
        return new WetnessCondition();
    }
}