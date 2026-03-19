package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class DimensionCondition implements SkillCondition {
    private final ResourceLocation dimensionId;

    public DimensionCondition(ResourceLocation dimensionId) {
        this.dimensionId = dimensionId;
    }

    @Override
    public boolean test(Player player) {
        return player.level().dimension().location().equals(dimensionId);
    }

    public static DimensionCondition fromJson(JsonObject json) {
        return new DimensionCondition(new ResourceLocation(GsonHelper.getString(json, "dimension")));
    }

    public ResourceLocation getDimensionId() { return dimensionId; }
}