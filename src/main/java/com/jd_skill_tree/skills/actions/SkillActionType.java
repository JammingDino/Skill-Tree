package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import com.jd_skill_tree.Jd_skill_tree;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record SkillActionType<T extends SkillAction>(Function<JsonObject, T> factory) {

    private static final Map<Identifier, SkillActionType<?>> REGISTRY = new HashMap<>();

    public static SkillAction create(JsonObject json) {
        Identifier typeId = new Identifier(JsonHelper.getString(json, "type"));
        SkillActionType<?> type = REGISTRY.get(typeId);
        if (type == null) throw new IllegalArgumentException("Unknown skill action type: " + typeId);
        return type.factory().apply(json);
    }

    public static <T extends SkillAction> void register(Identifier id, Function<JsonObject, T> factory) {
        REGISTRY.put(id, new SkillActionType<>(factory));
    }

    public static void registerAll() {
        Jd_skill_tree.LOGGER.info("Registering skill actions...");
        register(new Identifier(Jd_skill_tree.MOD_ID, "command"), CommandSkillAction::fromJson);
    }
}