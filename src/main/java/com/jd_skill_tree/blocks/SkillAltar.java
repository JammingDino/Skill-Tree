package com.jd_skill_tree.blocks;

import com.jd_skill_tree.blocks.entity.ModBlockEntities;
import com.jd_skill_tree.blocks.entity.SkillAltarBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

// 1. Extend BlockWithEntity instead of Block
public class SkillAltar extends BlockWithEntity {

    private final int tier;

    private static final VoxelShape SHAPE = VoxelShapes.union(
            createCuboidShape(0, 0, 0, 16, 12, 16)  // Base pedestal
    );

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return SHAPE;
    }

    public SkillAltar(Settings settings, int tier) {
        super(settings);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    // --- Required for the enchanted table hover effect ---

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SkillAltarBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // This tells Minecraft to use the BlockEntityRenderer instead of a JSON model.
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // This is the crucial part that links to your static tick() method.
        // We only run this on the client because the animation logic is purely visual.
        if (world.isClient()) {
            return checkType(type, ModBlockEntities.SKILL_ALTAR_ENTITY, (world1, pos, state1, be) -> SkillAltarBlockEntity.tick(world1, pos, state1, (SkillAltarBlockEntity) be));
        }
        return null; // No server-side ticking needed for animations.
    }


    // Your existing onUse method is perfectly fine.
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {

        // System.out.println("Skill Altar (Tier " + tier + ") clicked by: " + player.getName().getString());
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 2f, 0.7f);

        return ActionResult.SUCCESS;
    }
}