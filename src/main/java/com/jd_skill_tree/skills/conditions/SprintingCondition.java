package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

public class SprintingCondition implements SkillCondition {
    @Override
    public boolean test(Player player) {
        return player.isSprinting();
    }

    public static SprintingCondition fromJson(JsonObject json) {
        return new SprintingCondition();
    }
}