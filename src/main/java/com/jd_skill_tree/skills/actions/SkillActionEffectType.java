package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import com.jd_skill_tree.Jd_skill_tree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record SkillActionEffectType<T extends SkillActionEffect>(Function<JsonObject, T> factory) {
    private static final Map<ResourceLocation, SkillActionEffectType<?>> REGISTRY = new HashMap<>();

    public static SkillActionEffect create(JsonObject json) {
        ResourceLocation typeId = new ResourceLocation(GsonHelper.getString(json, "type"));
        SkillActionEffectType<?> type = REGISTRY.get(typeId);
        if (type == null) throw new IllegalArgumentException("Unknown action effect: " + typeId);
        return type.factory().apply(json);
    }

    public static <T extends SkillActionEffect> void register(ResourceLocation id, Function<JsonObject, T> factory) {
        REGISTRY.put(id, new SkillActionEffectType<>(factory));
    }

    public static void registerAll() {
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "command"), CommandActionEffect::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "burn"), BurnActionEffect::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "delayed"), DelayedActionEffect::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "heal"), HealActionEffect::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "launch"), LaunchActionEffect::fromJson);
        register(new ResourceLocation(Jd_skill_tree.MOD_ID, "raycast"), RaycastActionEffect::fromJson);
    }
}