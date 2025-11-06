package com.jd_skill_tree.blocks.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

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
    private static final Random RANDOM = Random.create();

    public SkillAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SKILL_ALTAR_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, SkillAltarBlockEntity blockEntity) {
        blockEntity.pageTurningSpeed = blockEntity.nextPageTurningSpeed;
        blockEntity.lastBookRotation = blockEntity.bookRotation;

        PlayerEntity playerEntity = world.getClosestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);

        if (playerEntity != null) {
            double d = playerEntity.getX() - (pos.getX() + 0.5);
            double e = playerEntity.getZ() - (pos.getZ() + 0.5);
            blockEntity.targetBookRotation = (float) MathHelper.atan2(e, d);
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
        blockEntity.nextPageTurningSpeed = MathHelper.clamp(blockEntity.nextPageTurningSpeed, 0.0F, 1.0F);
        blockEntity.ticks++;
        blockEntity.pageAngle = blockEntity.nextPageAngle;
        float h = (blockEntity.flipRandom - blockEntity.nextPageAngle) * 0.4F;
        float i = 0.2F;
        h = MathHelper.clamp(h, -i, i);
        blockEntity.flipTurn += (h - blockEntity.flipTurn) * 0.9F;
        blockEntity.nextPageAngle += blockEntity.flipTurn;
    }
}