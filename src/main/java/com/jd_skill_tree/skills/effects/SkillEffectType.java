package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.Jd_skill_tree;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a type of skill effect that can be loaded from JSON.
 * @param <T> The class that implements this effect's logic.
 */
public record SkillEffectType<T extends SkillEffect>(Function<JsonObject, T> factory) {

    // The registry that holds all known effect types
    private static final Map<Identifier, SkillEffectType<?>> REGISTRY = new HashMap<>();

    /**
     * Factory method for creating a SkillEffect from a JSON object.
     * This is the core of the data-driven system.
     */
    public static SkillEffect create(JsonObject json) {
        // 1. Get the "type" field from the JSON
        Identifier typeId = new Identifier(JsonHelper.getString(json, "type"));

        // 2. Look up the corresponding SkillEffectType in our registry
        SkillEffectType<?> type = REGISTRY.get(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown skill effect type: " + typeId);
        }

        // 3. Use the type's factory to create an instance of the effect class from the JSON
        return type.factory().apply(json);
    }

    /**
     * Registers a new type of skill effect.
     * This should be called for each effect we want to make available.
     */
    public static <T extends SkillEffect> SkillEffectType<T> register(Identifier id, Function<JsonObject, T> factory) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate skill effect type: " + id);
        }
        SkillEffectType<T> type = new SkillEffectType<>(factory);
        REGISTRY.put(id, type);
        return type;
    }

    /**
     * This is where we will register all our built-in effect types.
     * This method should be called once when the mod initializes.
     */
    public static void registerAll() {
        Jd_skill_tree.LOGGER.info("Registering skill effect types...");

        register(new Identifier(Jd_skill_tree.MOD_ID, "attribute"), AttributeSkillEffect::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "mining_speed"), MiningSpeedSkillEffect::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "potion"), PotionSkillEffect::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "enchantment"), EnchantmentSkillEffect::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "attack_knockback"), AttackKnockbackSkillEffect::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "experience"), ExperienceSkillEffect::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "swim_speed"), SwimSpeedSkillEffect::fromJson);
        register(new Identifier(Jd_skill_tree.MOD_ID, "lava_speed"), LavaSpeedSkillEffect::fromJson);
    }
}