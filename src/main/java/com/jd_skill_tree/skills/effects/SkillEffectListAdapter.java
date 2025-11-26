package com.jd_skill_tree.skills.effects;

import com.google.gson.*;
import com.jd_skill_tree.Jd_skill_tree;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SkillEffectListAdapter implements JsonDeserializer<List<SkillEffect>>, JsonSerializer<List<SkillEffect>> {

    // --- Deserialization (JSON -> Java) ---
    @Override
    public List<SkillEffect> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<SkillEffect> effects = new ArrayList<>();
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> {
                try {
                    effects.add(SkillEffectType.create(element.getAsJsonObject()));
                } catch (Exception e) {
                    Jd_skill_tree.LOGGER.error("Failed to deserialize skill effect", e);
                }
            });
        }
        return effects;
    }

    // --- Serialization (Java -> JSON) ---
    @Override
    public JsonElement serialize(List<SkillEffect> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray jsonArray = new JsonArray();

        for (SkillEffect effect : src) {
            JsonObject obj = new JsonObject();

            // 1. Identify the type based on the class
            if (effect instanceof AttributeSkillEffect attrEffect) {
                obj.addProperty("type", "jd_skill_tree:attribute");
                obj.addProperty("attribute", attrEffect.getAttribute() != null ? net.minecraft.registry.Registries.ATTRIBUTE.getId(attrEffect.getAttribute()).toString() : "");
                obj.addProperty("operation", attrEffect.getOperation().name());
                obj.addProperty("value", attrEffect.getValue());
            }
            else if (effect instanceof MiningSpeedSkillEffect miningEffect) {
                obj.addProperty("type", "jd_skill_tree:mining_speed");
                // We need to access the multiplier. You might need to add a getter to MiningSpeedSkillEffect if it's private.
                obj.addProperty("value", miningEffect.getMultiplier());
            }
            else if (effect instanceof PotionSkillEffect potionEffect) {
                obj.addProperty("type", "jd_skill_tree:potion");
                obj.addProperty("effect", potionEffect.getEffectId().toString());
                obj.addProperty("amplifier", potionEffect.getAmplifier());
                obj.addProperty("hide_particles", potionEffect.shouldHideParticles());
            }
            // Add other effect types here as you create them

            jsonArray.add(obj);
        }

        return jsonArray;
    }
}