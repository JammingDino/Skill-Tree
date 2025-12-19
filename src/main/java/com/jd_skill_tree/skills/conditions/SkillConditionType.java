package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import com.jd_skill_tree.Jd_skill_tree;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record SkillConditionType<T extends SkillCondition>(Function<JsonObject, T> factory) {

    private static final Map<Identifier, SkillConditionType<?>> REGISTRY = new HashMap<>();

    public static SkillCondition create(JsonObject json) {
        Identifier typeId = new Identifier(JsonHelper.getString(json, "type"));
        SkillConditionType<?> type = REGISTRY.get(typeId);
        if (type == null) throw new IllegalArgumentException("Unknown skill condition type: " + typeId);
        return type.factory().apply(json);
    }

    public static <T extends SkillCondition> void register(Identifier id, Function<JsonObject, T> factory) {
        REGISTRY.put(id, new SkillConditionType<>(factory));
    }

    public static void registerAll() {
        Jd_skill_tree.LOGGER.info("Registering skill conditions...");
        register(new Identifier(Jd_skill_tree.MOD_ID, "hand_item"), HandItemCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "equipped_item"), EquippedItemCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "y_level"), YLevelCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "health"), HealthCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "hunger"), HungerCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "armor"), ArmorCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "time"), TimeOfDayCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "dimension"), DimensionCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "walking_on"), WalkingOnBlockCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "wetness"), WetnessCondition::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "in_lava"), InLavaCondition::fromJson);
    }
}