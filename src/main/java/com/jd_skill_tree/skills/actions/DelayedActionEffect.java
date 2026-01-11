package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import com.jd_skill_tree.utils.ActionScheduler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DelayedActionEffect implements SkillActionEffect {

    private final int delay;
    private final SkillActionEffect nextEffect;
    private final SkillCondition nextCondition;

    public DelayedActionEffect(int delay, SkillActionEffect nextEffect, SkillCondition nextCondition) {
        this.delay = delay;
        this.nextEffect = nextEffect;
        this.nextCondition = nextCondition;
    }

    @Override
    public void execute(PlayerEntity player, World world, BlockPos pos) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Ensure 'delay' is an int
        ActionScheduler.schedule(delay, () -> {
            if (serverPlayer.isRemoved()) return;

            if (nextCondition == null || nextCondition.test(serverPlayer)) {
                // Use the player's CURRENT position, not the position from 20 ticks ago
                nextEffect.execute(serverPlayer, world, serverPlayer.getBlockPos());
            }
        }, serverPlayer.getServer());
    }

    public int getDelay() { return delay; }
    public SkillActionEffect getNextEffect() { return nextEffect; }
    public SkillCondition getNextCondition() { return nextCondition; }

    public static DelayedActionEffect fromJson(JsonObject json) {
        int delay = JsonHelper.getInt(json, "delay");

        JsonObject effectJson = json.getAsJsonObject("effect");
        SkillActionEffect effect = SkillActionEffectType.create(effectJson);

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new DelayedActionEffect(delay, effect, cond);
    }
}