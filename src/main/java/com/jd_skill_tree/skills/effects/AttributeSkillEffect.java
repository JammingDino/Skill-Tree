package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public class AttributeSkillEffect implements SkillEffect {

    public static final String MODIFIER_NAME = "JD Skill Tree Bonus";

    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final double value;
    private final SkillCondition condition;

    public AttributeSkillEffect(Attribute attribute, AttributeModifier.Operation operation, double value, SkillCondition condition) {
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    public Attribute getAttribute() { return this.attribute; }
    public AttributeModifier.Operation getOperation() { return this.operation; }
    public double getValue() { return this.value; }

    public static AttributeSkillEffect fromJson(JsonObject json) {
        ResourceLocation attributeId = new ResourceLocation(GsonHelper.getAsString(json, "attribute"));
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown attribute: " + attributeId);
        }

        String operationName = GsonHelper.getAsString(json, "operation", "addition").toUpperCase();
        AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(operationName);

        double value = GsonHelper.getAsDouble(json, "value", 0);

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new AttributeSkillEffect(attribute, operation, value, cond);
    }
}