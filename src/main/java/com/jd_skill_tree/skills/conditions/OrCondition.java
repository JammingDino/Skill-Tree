package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import java.util.ArrayList;
import java.util.List;

public class OrCondition implements SkillCondition {
    private final List<SkillCondition> conditions;

    public OrCondition(List<SkillCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean test(PlayerEntity player) {
        // Only one needs to be true
        for (SkillCondition condition : conditions) {
            if (condition.test(player)) return true;
        }
        return false;
    }

    public List<SkillCondition> getConditions() { return conditions; }

    public static OrCondition fromJson(JsonObject json) {
        List<SkillCondition> list = new ArrayList<>();
        if (json.has("conditions")) {
            JsonArray array = json.getAsJsonArray("conditions");
            for (JsonElement element : array) {
                list.add(SkillConditionType.create(element.getAsJsonObject()));
            }
        }
        return new OrCondition(list);
    }
}