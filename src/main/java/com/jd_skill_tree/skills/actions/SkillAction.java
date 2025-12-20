package com.jd_skill_tree.skills.actions;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SkillAction {
    private final TriggerType trigger;
    private final SkillActionEffect effect;
    private final int interval; // used for TIMER

    public SkillAction(TriggerType trigger, SkillActionEffect effect, int interval) {
        this.trigger = trigger;
        this.effect = effect;
        this.interval = interval;
    }

    public void run(PlayerEntity player, World world, BlockPos pos) {
        effect.execute(player, world, pos);
    }

    public TriggerType getTrigger() { return trigger; }
    public SkillActionEffect getEffect() { return effect; }
    public int getInterval() { return interval; }
}