package com.jd_skill_tree.skills.actions;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface SkillAction {

    TriggerType getTrigger();

    /**
     * Executes the action.
     * @param player The player performing the action.
     * @param world The world level.
     * @param pos The position of the event (block broken/placed).
     */
    void execute(PlayerEntity player, World world, BlockPos pos);
}