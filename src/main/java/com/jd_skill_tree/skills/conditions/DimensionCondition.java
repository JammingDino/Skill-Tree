package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class DimensionCondition implements SkillCondition {
    private final Identifier dimensionId;

    public DimensionCondition(Identifier dimensionId) {
        this.dimensionId = dimensionId;
    }

    @Override
    public boolean test(PlayerEntity player) {
        return player.getWorld().getRegistryKey().getValue().equals(dimensionId);
    }

    public static DimensionCondition fromJson(JsonObject json) {
        return new DimensionCondition(new Identifier(JsonHelper.getString(json, "dimension")));
    }

    public Identifier getDimensionId() { return dimensionId; }
}