package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SkillEffectListAdapter implements JsonDeserializer<List<SkillEffect>> {
    @Override
    public List<SkillEffect> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<SkillEffect> effects = new ArrayList<>();
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> {
                effects.add(SkillEffectType.create(element.getAsJsonObject()));
            });
        }
        return effects;
    }
}