package com.jd_skill_tree.skills.actions;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface SkillActionEffect {
    // Changed from Player to Entity to support targeting mobs
    void execute(Entity target, Level world, BlockPos pos);
}