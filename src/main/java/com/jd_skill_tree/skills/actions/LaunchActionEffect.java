package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LaunchActionEffect implements SkillActionEffect {
    private final float strength;
    private final float vertical;

    public LaunchActionEffect(float strength, float vertical) {
        this.strength = strength;
        this.vertical = vertical;
    }

    @Override
    public void execute(Entity target, World world, BlockPos pos) {
        // Velocity must be applied to the target entity
        Vec3d look = target.getRotationVector();

        target.addVelocity(
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
                JsonHelper.getFloat(json, "strength", 1.0f),
                JsonHelper.getFloat(json, "vertical", 0.5f)
        );
    }
}