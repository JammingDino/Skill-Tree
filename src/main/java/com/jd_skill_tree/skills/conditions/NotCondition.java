package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

public class NotCondition implements SkillCondition {
    private final SkillCondition condition;

    public NotCondition(SkillCondition condition) {
        this.condition = condition;
    }

    @Override
    public boolean test(Player player) {
        // Invert the result
        return !condition.test(player);
    }

    public SkillCondition getCondition() { return condition; }

    public static NotCondition fromJson(JsonObject json) {
        // Parse the single child condition
        return new NotCondition(SkillConditionType.create(json.getAsJsonObject("condition")));
    }
}