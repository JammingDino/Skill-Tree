package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import com.jd_skill_tree.utils.ActionScheduler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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
    public void execute(Entity target, World world, BlockPos pos) {
        if (world.isClient) return;

        // We need a server reference. Try to get it from the target or the world.
        net.minecraft.server.MinecraftServer server = world.getServer();
        if (server == null) return;

        ActionScheduler.schedule(delay, () -> {
            // 1. Check if target still exists
            if (target.isRemoved()) return;

            // 2. Check Condition (Only works if target is a player, otherwise ignore condition)
            if (nextCondition != null) {
                if (target instanceof PlayerEntity p) {
                    if (!nextCondition.test(p)) return;
                }
                // If target is not a player, we technically can't check 'Player' conditions on a Zombie.
                // For now, we skip the check if it's not a player.
            }

            // 3. Execute
            // Use the target's CURRENT position
            nextEffect.execute(target, world, target.getBlockPos());

        }, server);
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