package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BurnActionEffect implements SkillActionEffect {
    private final int duration; // in ticks
    private final boolean ignoreArmor;

    public BurnActionEffect(int duration, boolean ignoreArmor) {
        this.duration = duration;
        this.ignoreArmor = ignoreArmor;
    }

    @Override
    public void execute(Entity target, World world, BlockPos pos) {
        if (world.isClient) return;
        target.setOnFireFor(duration / 20);

        if (ignoreArmor) {
            // In 1.20, 'magic' is the standard damage source that bypasses armor.
            // We use this for the 'ignoreArmor' toggle.
            target.damage(world.getDamageSources().magic(), 1.0f);
        } else {
            // Standard fire damage (respects armor)
            target.damage(world.getDamageSources().onFire(), 1.0f);
        }
    }

    public int getDuration() { return duration; }
    public boolean isIgnoreArmor() { return ignoreArmor; }

    public static BurnActionEffect fromJson(JsonObject json) {
        return new BurnActionEffect(
                JsonHelper.getInt(json, "duration", 100),
                JsonHelper.getBoolean(json, "ignore_armor", false)
        );
    }
}