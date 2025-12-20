package com.jd_skill_tree.skills.actions;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface SkillActionEffect {
    void execute(PlayerEntity player, World world, BlockPos pos);
}