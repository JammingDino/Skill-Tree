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
                if (hand.getNbt() != null) {
                    obj.addProperty("nbt", hand.getNbt().toString());
                }
            }
            else if (condition instanceof EquippedItemCondition equipped) {
                obj.addProperty("type", "jd_skill_tree:equipped_item");
                obj.addProperty("item", Registries.ITEM.getId(equipped.getTargetItem()).toString());

                String slotName = switch (equipped.getSlot()) {
                    case FEET -> "boots";
                    case LEGS -> "legs";
                    case CHEST -> "chest";
                    default -> "helmet";
                };
                obj.addProperty("slot", slotName);
                if (equipped.getNbt() != null) {
                    obj.addProperty("nbt", equipped.getNbt().toString());
                }
            }
            else if (condition instanceof YLevelCondition yLevel) {
                obj.addProperty("type", "jd_skill_tree:y_level");
                obj.addProperty("comparison", yLevel.getComparison().name());
                obj.addProperty("y_level", yLevel.getTargetY());
            }
            else if (condition instanceof HealthCondition health) {
                obj.addProperty("type", "jd_skill_tree:health");
                obj.addProperty("comparison", health.getComparison().name());
                obj.addProperty("amount", health.getTargetHealth());
            }
            else if (condition instanceof HungerCondition hunger) {
                obj.addProperty("type", "jd_skill_tree:hunger");
                obj.addProperty("comparison", hunger.getComparison().name());
                obj.addProperty("amount", hunger.getTargetHunger());
            }
            else if (condition instanceof ArmorCondition armor) {
                obj.addProperty("type", "jd_skill_tree:armor");
                obj.addProperty("comparison", armor.getComparison().name());
                obj.addProperty("amount", armor.getTargetArmor());
            }
            else if (condition instanceof TimeOfDayCondition time) {
                obj.addProperty("type", "jd_skill_tree:time");
                obj.addProperty("min", time.getMinTime());
                obj.addProperty("max", time.getMaxTime());
            }
            else if (condition instanceof DimensionCondition dim) {
                obj.addProperty("type", "jd_skill_tree:dimension");
                obj.addProperty("dimension", dim.getDimensionId().toString());
            }
            else if (condition instanceof WalkingOnBlockCondition walk) {
                obj.addProperty("type", "jd_skill_tree:walking_on");
                obj.addProperty("block", Registries.BLOCK.getId(walk.getTargetBlock()).toString());
            }
            else if (condition instanceof WetnessCondition) {
                obj.addProperty("type", "jd_skill_tree:wetness");
            }
            else if (condition instanceof InLavaCondition) {
                obj.addProperty("type", "jd_skill_tree:in_lava");
            }
            jsonArray.add(obj);
        }
        return jsonArray;
    }
}