package com.jd_skill_tree.blocks.entity.renderer;

import com.jd_skill_tree.blocks.entity.SkillAltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkillAltarBlockEntityRenderer implements BlockEntityRenderer<SkillAltarBlockEntity> {

    public static final Material BOOK_TEXTURE = new Material(
            InventoryMenu.BLOCK_ATLAS,
            new ResourceLocation("entity/enchanting_table_book")
    );

    private final BookModel bookModel;

    public SkillAltarBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.bookModel = new BookModel(ctx.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(SkillAltarBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.75F, 0.5F);
        float g = (float) blockEntity.ticks + partialTick;
        poseStack.translate(0.0F, 0.1F + Mth.sin(g * 0.1F) * 0.01F, 0.0F);

        float h = blockEntity.bookRotation - blockEntity.lastBookRotation;
        while (h >= (float) Math.PI) h -= (float) (Math.PI * 2);
        while (h < (float) -Math.PI) h += (float) (Math.PI * 2);
        float k = blockEntity.lastBookRotation + h * partialTick;

        poseStack.mulPose(Axis.YP.rotation(-k));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));

        float l = Mth.lerp(partialTick, blockEntity.pageAngle, blockEntity.nextPageAngle);
        float m = Mth.frac(l + 0.25F) * 1.6F - 0.3F;
        float n = Mth.frac(l + 0.75F) * 1.6F - 0.3F;
        float o = Mth.lerp(partialTick, blockEntity.pageTurningSpeed, blockEntity.nextPageTurningSpeed);

        this.bookModel.setupAnim(g, Mth.clamp(m, 0.0F, 1.0F), Mth.clamp(n, 0.0F, 1.0F), o);

        var vertexConsumer = BOOK_TEXTURE.buffer(bufferSource, RenderType::entitySolid);
        this.bookModel.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}
