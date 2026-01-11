package com.jd_skill_tree.skills.actions;

import com.jd_skill_tree.skills.conditions.SkillCondition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SkillAction {
    private final TriggerType trigger;
    private final SkillActionEffect effect;
    private final int interval; // used for TIMER
    private final SkillCondition condition; // The specific condition for this action

    public SkillAction(TriggerType trigger, SkillActionEffect effect, int interval, SkillCondition condition) {
        this.trigger = trigger;
        this.effect = effect;
        this.interval = interval;
        this.condition = condition;
    }

    /**
     * Checks if the action is allowed to run for this player.
     * Returns true if there is no condition, or if the condition is met.
     */
    public boolean shouldRun(PlayerEntity player) {
        return condition == null || condition.test(player);
    }

    /**
     * Executes the action, BUT ONLY IF the condition is met.
     */
    public void run(PlayerEntity player, World world, BlockPos pos) {
        if (shouldRun(player)) {
            effect.execute(player, world, pos);
        }
    }

    public TriggerType getTrigger() { return trigger; }
    public SkillActionEffect getEffect() { return effect; }
    public int getInterval() { return interval; }
    public SkillCondition getCondition() { return condition; }
}