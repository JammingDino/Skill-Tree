package com.jd_skill_tree.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SkillAltarBlockEntity extends BlockEntity {

    public int ticks;
    public float nextPageAngle;
    public float pageAngle;
    public float flipRandom;
    public float flipTurn;
    public float nextPageTurningSpeed;
    public float pageTurningSpeed;
    public float bookRotation;
    public float lastBookRotation;
    public float targetBookRotation;
    private static final RandomSource RANDOM = RandomSource.create();

    public SkillAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SKILL_ALTAR_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SkillAltarBlockEntity blockEntity) {
        blockEntity.pageTurningSpeed = blockEntity.nextPageTurningSpeed;
        blockEntity.lastBookRotation = blockEntity.bookRotation;

        Player playerEntity = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);

        if (playerEntity != null) {
            double d = playerEntity.getX() - (pos.getX() + 0.5);
            double e = playerEntity.getZ() - (pos.getZ() + 0.5);
            blockEntity.targetBookRotation = (float) Mth.atan2(e, d);
            blockEntity.nextPageTurningSpeed += 0.1F;
            if (blockEntity.nextPageTurningSpeed < 0.5F || RANDOM.nextInt(40) == 0) {
                float f = blockEntity.flipRandom;
                do {
                    blockEntity.flipRandom += (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (f == blockEntity.flipRandom);
            }
        } else {
            blockEntity.targetBookRotation += 0.02F;
            blockEntity.nextPageTurningSpeed -= 0.1F;
        }

        while (blockEntity.bookRotation >= (float) Math.PI) {
            blockEntity.bookRotation -= ((float) Math.PI * 2);
        }
        while (blockEntity.bookRotation < (float) -Math.PI) {
            blockEntity.bookRotation += ((float) Math.PI * 2);
        }
        while (blockEntity.targetBookRotation >= (float) Math.PI) {
            blockEntity.targetBookRotation -= ((float) Math.PI * 2);
        }
        while (blockEntity.targetBookRotation < (float) -Math.PI) {
            blockEntity.targetBookRotation += ((float) Math.PI * 2);
        }

        float g = blockEntity.targetBookRotation - blockEntity.bookRotation;
        while (g >= (float) Math.PI) {
            g -= ((float) Math.PI * 2);
        }
        while (g < (float) -Math.PI) {
            g += ((float) Math.PI * 2);
        }

        blockEntity.bookRotation += g * 0.4F;
        blockEntity.nextPageTurningSpeed = Mth.clamp(blockEntity.nextPageTurningSpeed, 0.0F, 1.0F);
        blockEntity.ticks++;
        blockEntity.pageAngle = blockEntity.nextPageAngle;
        float h = (blockEntity.flipRandom - blockEntity.nextPageAngle) * 0.4F;
        float i = 0.2F;
        h = Mth.clamp(h, -i, i);
        blockEntity.flipTurn += (h - blockEntity.flipTurn) * 0.9F;
        blockEntity.nextPageAngle += blockEntity.flipTurn;
    }
}