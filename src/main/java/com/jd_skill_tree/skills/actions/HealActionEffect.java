package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HealActionEffect implements SkillActionEffect {
    private final float amount;
    private final boolean isHunger; // true = feed, false = heal

    public HealActionEffect(float amount, boolean isHunger) {
        this.amount = amount;
        this.isHunger = isHunger;
    }

    @Override
    public void execute(Entity target, World world, BlockPos pos) {
        if (world.isClient) return;

        // Ensure target is alive
        if (!(target instanceof LivingEntity living)) return;

        if (isHunger) {
            // Only players have hunger mechanics
            if (living instanceof PlayerEntity p) {
                p.getHungerManager().add((int)amount, 0.5f);
            }
        } else {
            // Any living entity can be healed
            living.heal(amount);
        }
    }

    public float getAmount() { return amount; }
    public boolean isHunger() { return isHunger; }

    public static HealActionEffect fromJson(JsonObject json) {
        return new HealActionEffect(
                JsonHelper.getFloat(json, "amount", 2.0f),
                JsonHelper.getBoolean(json, "is_hunger", false)
        );
    }
}