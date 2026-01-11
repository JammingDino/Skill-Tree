package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class AttributeSkillEffect implements SkillEffect {

    public static final String MODIFIER_NAME = "JD Skill Tree Bonus";

    private final EntityAttribute attribute;
    private final EntityAttributeModifier.Operation operation;
    private final double value;
    private final SkillCondition condition; // Nested condition

    public AttributeSkillEffect(EntityAttribute attribute, EntityAttributeModifier.Operation operation, double value, SkillCondition condition) {
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    public EntityAttribute getAttribute() { return this.attribute; }
    public EntityAttributeModifier.Operation getOperation() { return this.operation; }
    public double getValue() { return this.value; }

    public static AttributeSkillEffect fromJson(JsonObject json) {
        Identifier attributeId = new Identifier(JsonHelper.getString(json, "attribute"));
        EntityAttribute attribute = Registries.ATTRIBUTE.get(attributeId);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown attribute: " + attributeId);
        }

        String operationName = JsonHelper.getString(json, "operation", "addition").toUpperCase();
        EntityAttributeModifier.Operation operation = EntityAttributeModifier.Operation.valueOf(operationName);

        double value = JsonHelper.getDouble(json, "value");

        // --- PARSE CONDITION ---
        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }
        // -----------------------

        return new AttributeSkillEffect(attribute, operation, value, cond);
    }
}