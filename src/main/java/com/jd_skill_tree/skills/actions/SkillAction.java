package com.jd_skill_tree.skills.actions;

import com.jd_skill_tree.skills.conditions.SkillCondition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SkillAction {
    private final TriggerType trigger;
    private final SkillActionEffect effect;
    private final int interval;
    private final SkillCondition condition;

    public SkillAction(TriggerType trigger, SkillActionEffect effect, int interval, SkillCondition condition) {
        this.trigger = trigger;
        this.effect = effect;
        this.interval = interval;
        this.condition = condition;
    }

    public boolean shouldRun(PlayerEntity owner) {
        return condition == null || condition.test(owner);
    }

    /**
     * @param owner The player who possesses the skill (used for Conditions)
     * @param target The entity being affected (used for Effects)
     */
    public void run(PlayerEntity owner, Entity target, World world, BlockPos pos) {
        if (shouldRun(owner)) {
            effect.execute(target, world, pos);
        }
    }

    public TriggerType getTrigger() { return trigger; }
    public SkillActionEffect getEffect() { return effect; }
    public int getInterval() { return interval; }
    public SkillCondition getCondition() { return condition; }
}