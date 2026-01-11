package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import java.util.ArrayList;
import java.util.List;

public class AndCondition implements SkillCondition {
    private final List<SkillCondition> conditions;

    public AndCondition(List<SkillCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean test(PlayerEntity player) {
        // All must be true
        for (SkillCondition condition : conditions) {
            if (!condition.test(player)) return false;
        }
        return true;
    }

    public List<SkillCondition> getConditions() { return conditions; }

    public static AndCondition fromJson(JsonObject json) {
        List<SkillCondition> list = new ArrayList<>();
        if (json.has("conditions")) {
            JsonArray array = json.getAsJsonArray("conditions");
            for (JsonElement element : array) {
                // We delegate back to the Registry wrapper to parse children recursively
                list.add(SkillConditionType.create(element.getAsJsonObject()));
            }
        }
        return new AndCondition(list);
    }
}