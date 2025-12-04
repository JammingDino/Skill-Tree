package com.jd_skill_tree.skills.actions;

import com.google.gson.*;
import com.jd_skill_tree.Jd_skill_tree;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SkillActionListAdapter implements JsonDeserializer<List<SkillAction>>, JsonSerializer<List<SkillAction>> {

    @Override
    public List<SkillAction> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<SkillAction> actions = new ArrayList<>();
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> {
                try {
                    actions.add(SkillActionType.create(element.getAsJsonObject()));
                } catch (Exception e) {
                    Jd_skill_tree.LOGGER.error("Failed to deserialize skill action", e);
                }
            });
        }
        return actions;
    }

    @Override
    public JsonElement serialize(List<SkillAction> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray jsonArray = new JsonArray();
        for (SkillAction action : src) {
            JsonObject obj = new JsonObject();
            if (action instanceof CommandSkillAction cmd) {
                obj.addProperty("type", "jd_skill_tree:command");
                obj.addProperty("trigger", cmd.getTrigger().name());
                obj.addProperty("command", cmd.getCommand());
            }
            jsonArray.add(obj);
        }
        return jsonArray;
    }
}