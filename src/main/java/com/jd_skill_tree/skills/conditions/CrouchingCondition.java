package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

public class CrouchingCondition implements SkillCondition {
    @Override
    public boolean test(PlayerEntity player) {
        return player.isSneaking();
    }

    public static CrouchingCondition fromJson(JsonObject json) {
        return new CrouchingCondition();
    }
}