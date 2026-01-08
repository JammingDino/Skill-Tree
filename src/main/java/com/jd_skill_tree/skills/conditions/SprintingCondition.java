package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

public class SprintingCondition implements SkillCondition {
    @Override
    public boolean test(PlayerEntity player) {
        return player.isSprinting();
    }

    public static SprintingCondition fromJson(JsonObject json) {
        return new SprintingCondition();
    }
}