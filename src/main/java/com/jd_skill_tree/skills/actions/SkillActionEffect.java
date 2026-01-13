package com.jd_skill_tree.skills.actions;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface SkillActionEffect {
    // Changed from PlayerEntity to Entity to support targeting mobs
    void execute(Entity target, World world, BlockPos pos);
}