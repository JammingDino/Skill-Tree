package com.jd_skill_tree.skills.conditions;

import com.google.gson.*;
import com.jd_skill_tree.Jd_skill_tree;
import net.minecraft.registry.Registries;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SkillConditionListAdapter implements JsonDeserializer<List<SkillCondition>>, JsonSerializer<List<SkillCondition>> {

    @Override
    public List<SkillCondition> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<SkillCondition> conditions = new ArrayList<>();
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> {
                try {
                    conditions.add(SkillConditionType.create(element.getAsJsonObject()));
                } catch (Exception e) {
                    Jd_skill_tree.LOGGER.error("Failed to deserialize skill condition", e);
                }
            });
        }
        return conditions;
    }

    @Override
    public JsonElement serialize(List<SkillCondition> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray jsonArray = new JsonArray();
        for (SkillCondition condition : src) {
            JsonObject obj = new JsonObject();
            if (condition instanceof HandItemCondition hand) {
                obj.addProperty("type", "jd_skill_tree:hand_item");
                obj.addProperty("item", Registries.ITEM.getId(hand.getTargetItem()).toString());
                obj.addProperty("count", hand.getMinCount());
                obj.addProperty("slot", hand.getSlot() == HandItemCondition.HandSlot.OFFHAND ? "offhand" : "mainhand");
            }
            jsonArray.add(obj);
        }
        return jsonArray;
    }
}