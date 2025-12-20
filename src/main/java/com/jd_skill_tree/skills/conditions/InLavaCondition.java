package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

public class InLavaCondition implements SkillCondition {
    @Override
    public boolean test(PlayerEntity player) {
        return player.isInLava();
    }

    public static InLavaCondition fromJson(JsonObject json) {
        return new InLavaCondition();
    }
}