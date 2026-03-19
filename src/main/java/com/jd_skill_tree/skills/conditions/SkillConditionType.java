package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import com.jd_skill_tree.Jd_skill_tree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record SkillConditionType<T extends SkillCondition>(Function<JsonObject, T> factory) {

    private static final Map<ResourceLocation, SkillConditionType<?>> REGISTRY = new HashMap<>();

    public static SkillCondition create(JsonObject json) {
        // Validation: Ensure 'type' exists
        if (!json.has("type")) {
            throw new IllegalArgumentException("Condition JSON missing 'type' field: " + json);
        }
        ResourceLocation typeId = new ResourceLocation(GsonHelper.getAsString(json, "type"));
        SkillConditionType<?> type = REGISTRY.get(typeId);
        if (type == null) throw new IllegalArgumentException("Unknown skill condition type: " + typeId);
        return type.factory().apply(json);
    }

    public static <T extends SkillCondition> void register(ResourceLocation id, Function<JsonObject, T> factory) {
        REGISTRY.put(id, new SkillConditionType<>(factory));
    }

    public static void registerAll() {
        Jd_skill_tree.LOGGER.info("Registering skill conditions...");

        // LOGIC
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "and"), AndCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "or"), OrCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "not"), NotCondition::fromJson);

        // ITEMS
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "hand_item"), HandItemCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "equipped_item"), EquippedItemCondition::fromJson);

        // PLAYER STATE
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "health"), HealthCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "hunger"), HungerCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "armor"), ArmorCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "sprinting"), SprintingCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "crouching"), CrouchingCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "wetness"), WetnessCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "in_lava"), InLavaCondition::fromJson);

        // WORLD STATE
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "y_level"), YLevelCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "time"), TimeOfDayCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "dimension"), DimensionCondition::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "walking_on"), WalkingOnBlockCondition::fromJson);
    }
}