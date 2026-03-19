package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class HealActionEffect implements SkillActionEffect {
    private final float amount;
    private final boolean isHunger; // true = feed, false = heal

    public HealActionEffect(float amount, boolean isHunger) {
        this.amount = amount;
        this.isHunger = isHunger;
    }

    @Override
    public void execute(Entity target, Level world, BlockPos pos) {
        if (world.isClientSide) return;

        // Ensure target is alive
        if (!(target instanceof LivingEntity living)) return;

        if (isHunger) {
            // Only players have hunger mechanics
            if (living instanceof Player p) {
                p.getFoodData().add((int)amount, 0.5f);
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
                GsonHelper.getFloat(json, "amount", 2.0f),
                GsonHelper.getBoolean(json, "is_hunger", false)
        );
    }
}