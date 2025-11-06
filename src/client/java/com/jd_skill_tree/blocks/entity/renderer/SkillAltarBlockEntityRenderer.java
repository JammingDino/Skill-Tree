package com.jd_skill_tree.blocks.entity.renderer;

import com.jd_skill_tree.blocks.entity.SkillAltarBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class SkillAltarBlockEntityRenderer implements BlockEntityRenderer<SkillAltarBlockEntity> {

    // Use a SpriteIdentifier for proper texture atlas handling, just like the vanilla renderer
    public static final SpriteIdentifier BOOK_TEXTURE = new SpriteIdentifier(
            SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("entity/enchanting_table_book")
    );
    private final BookModel bookModel;

    public SkillAltarBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.bookModel = new BookModel(ctx.getLayerModelPart(EntityModelLayers.BOOK));
    }

    @Override
    public void render(SkillAltarBlockEntity skillAltarBlockEntity, float tickDelta, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int light, int overlay) {
        matrixStack.push();
        // Translate to the center of the block and raise it to the standard enchanting table height
        matrixStack.translate(0.5F, 0.75F, 0.5F);
        // Apply the gentle bobbing animation
        float g = (float)skillAltarBlockEntity.ticks + tickDelta;
        matrixStack.translate(0.0F, 0.1F + MathHelper.sin(g * 0.1F) * 0.01F, 0.0F);

        // Calculate the interpolated rotation of the book
        float h = skillAltarBlockEntity.bookRotation - skillAltarBlockEntity.lastBookRotation;
        while (h >= (float) Math.PI) {
            h -= (float) (Math.PI * 2);
        }
        while (h < (float) -Math.PI) {
            h += (float) (Math.PI * 2);
        }
        float k = skillAltarBlockEntity.lastBookRotation + h * tickDelta;

        // Apply rotations
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(-k));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(80.0F));

        // Calculate page turning angles
        float l = MathHelper.lerp(tickDelta, skillAltarBlockEntity.pageAngle, skillAltarBlockEntity.nextPageAngle);
        float m = MathHelper.fractionalPart(l + 0.25F) * 1.6F - 0.3F;
        float n = MathHelper.fractionalPart(l + 0.75F) * 1.6F - 0.3F;
        float o = MathHelper.lerp(tickDelta, skillAltarBlockEntity.pageTurningSpeed, skillAltarBlockEntity.nextPageTurningSpeed);

        // Set the page angles on the model
        this.bookModel.setPageAngles(g, MathHelper.clamp(m, 0.0F, 1.0F), MathHelper.clamp(n, 0.0F, 1.0F), o);

        // Get the correct vertex consumer from the texture and render layer
        VertexConsumer vertexConsumer = BOOK_TEXTURE.getVertexConsumer(vertexConsumerProvider, RenderLayer::getEntitySolid);

        // Render the book model
        this.bookModel.renderBook(matrixStack, vertexConsumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.pop();
    }
}