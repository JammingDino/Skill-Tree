package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

public class CrouchingCondition implements SkillCondition {
    @Override
    public boolean test(Player player) {
        return player.isSneaking();
    }

    public static CrouchingCondition fromJson(JsonObject json) {
        return new CrouchingCondition();
    }
}