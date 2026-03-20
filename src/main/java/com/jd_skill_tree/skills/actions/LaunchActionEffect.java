package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class LaunchActionEffect implements SkillActionEffect {
    private final float strength;
    private final float vertical;

    public LaunchActionEffect(float strength, float vertical) {
        this.strength = strength;
        this.vertical = vertical;
    }

    @Override
    public void execute(Entity target, Level world, BlockPos pos) {
        // Velocity must be applied to the target entity
        Vec3 look = target.getLookAngle();

        target.push(
                look.x * strength,
                vertical,
                look.z * strength
        );

        // Mark velocity as changed so the game syncs it
        target.velocityModified = true;
    }

    public float getStrength() { return strength; }
    public float getVertical() { return vertical; }

    public static LaunchActionEffect fromJson(JsonObject json) {
        return new LaunchActionEffect(
                GsonHelper.getFloat(json, "strength", 1.0f),
                GsonHelper.getFloat(json, "vertical", 0.5f)
        );
    }
}