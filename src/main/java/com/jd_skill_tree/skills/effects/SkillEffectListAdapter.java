package com.jd_skill_tree.skills.effects;

import com.google.gson.*;
import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.skills.conditions.SkillConditionListAdapter;
import net.minecraft.registry.Registries;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SkillEffectListAdapter implements JsonDeserializer<List<SkillEffect>>, JsonSerializer<List<SkillEffect>> {

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

    @Override
    public JsonElement serialize(List<SkillEffect> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray jsonArray = new JsonArray();

        for (SkillEffect effect : src) {
            JsonObject obj = new JsonObject();

            // --- FIXED: Use static helper ---
            if (effect.getCondition() != null) {
                obj.add("condition", SkillConditionListAdapter.serializeCondition(effect.getCondition(), context));
            }
            // --------------------------------

            if (effect instanceof AttributeSkillEffect attrEffect) {
                obj.addProperty("type", "jd_skill_tree:attribute");
                obj.addProperty("attribute", attrEffect.getAttribute() != null ? Objects.requireNonNull(Registries.ATTRIBUTE.getId(attrEffect.getAttribute())).toString() : "");
                obj.addProperty("operation", attrEffect.getOperation().name());
                obj.addProperty("value", attrEffect.getValue());
            }
            else if (effect instanceof MiningSpeedSkillEffect miningEffect) {
                obj.addProperty("type", "jd_skill_tree:mining_speed");
                obj.addProperty("value", miningEffect.getMultiplier());
            }
            else if (effect instanceof PotionSkillEffect potionEffect) {
                obj.addProperty("type", "jd_skill_tree:potion");
                obj.addProperty("effect", potionEffect.getEffectId().toString());
                obj.addProperty("amplifier", potionEffect.getAmplifier());
                obj.addProperty("hide_particles", potionEffect.shouldHideParticles());
            }
            else if (effect instanceof EnchantmentSkillEffect enchEffect) {
                obj.addProperty("type", "jd_skill_tree:enchantment");
                obj.addProperty("enchantment", enchEffect.getEnchantmentId().toString());
                obj.addProperty("level_added", enchEffect.getLevelAdded());
                obj.addProperty("over_enchant", enchEffect.isAllowOverEnchanting());

                String slotName = switch (enchEffect.getTargetSlot()) {
                    case OFFHAND -> "offhand";
                    case FEET -> "boots";
                    case LEGS -> "legs";
                    case CHEST -> "chest";
                    case HEAD -> "helmet";
                    default -> "mainhand";
                };
                obj.addProperty("slot", slotName);
            }
            else if (effect instanceof AttackKnockbackSkillEffect kbEffect) {
                obj.addProperty("type", "jd_skill_tree:attack_knockback");
                obj.addProperty("value", kbEffect.getAmount());
            }
            else if (effect instanceof ExperienceSkillEffect xpEffect) {
                obj.addProperty("type", "jd_skill_tree:experience");
                obj.addProperty("value", xpEffect.getMultiplier());
            }
            else if (effect instanceof SwimSpeedSkillEffect swimEffect) {
                obj.addProperty("type", "jd_skill_tree:swim_speed");
                obj.addProperty("value", swimEffect.getMultiplier());
            }
            else if (effect instanceof LavaSpeedSkillEffect lavaEffect) {
                obj.addProperty("type", "jd_skill_tree:lava_speed");
                obj.addProperty("value", lavaEffect.getMultiplier());
            }
            else if (effect instanceof EffectImmunitySkillEffect immuneEffect) {
                obj.addProperty("type", "jd_skill_tree:effect_immunity");
                obj.addProperty("effect", immuneEffect.getEffectId().toString());
            }

            jsonArray.add(obj);
        }

        return jsonArray;
    }
}