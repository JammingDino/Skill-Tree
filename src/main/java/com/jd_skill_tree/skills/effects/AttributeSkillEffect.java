package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.UUID;

public class AttributeSkillEffect implements SkillEffect {

    // A consistent name for all modifiers created by our mod. This is how we'll find them to remove them.
    public static final String MODIFIER_NAME = "JD Skill Tree Bonus";

    private final EntityAttribute attribute;
    private final EntityAttributeModifier.Operation operation;
    private final double value;

    public AttributeSkillEffect(EntityAttribute attribute, EntityAttributeModifier.Operation operation, double value) {
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
    }

    // --- GETTERS ---
    // The Mixin will use these to create the modifier on the fly.
    public EntityAttribute getAttribute() { return this.attribute; }
    public EntityAttributeModifier.Operation getOperation() { return this.operation; }
    public double getValue() { return this.value; }

    // The onTick method is no longer needed here, as the Mixin will handle all logic.

    public static AttributeSkillEffect fromJson(JsonObject json) {
        Identifier attributeId = new Identifier(JsonHelper.getString(json, "attribute"));
        EntityAttribute attribute = Registries.ATTRIBUTE.get(attributeId);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown attribute: " + attributeId);
        }

        String operationName = JsonHelper.getString(json, "operation", "addition").toUpperCase();
        EntityAttributeModifier.Operation operation = EntityAttributeModifier.Operation.valueOf(operationName);

        double value = JsonHelper.getDouble(json, "value");

        return new AttributeSkillEffect(attribute, operation, value);
    }
}