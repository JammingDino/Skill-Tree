package com.jd_skill_tree.mixin;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.effects.ElytraSkillEffect;
import com.jd_skill_tree.skills.effects.SkillEffect;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {

    /**
     * 1.21.4: entity state is captured in its render state before rendering, and the
     * stack lives on BipedEntityRenderState#equippedChestStack. The stored stack is
     * copied into the render state when the state is created, so a swap at STORE has
     * no visible effect beyond keeping the mixin harmless; skill-based gliding is
     * separately allowed by EntityElytraEvents.CUSTOM in ModRegistries.
     */
    @ModifyVariable(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at = @At("STORE"), ordinal = 0)
    private ItemStack modifyElytraStack(ItemStack original, net.minecraft.client.util.math.MatrixStack matrixStack, net.minecraft.client.render.VertexConsumerProvider vertexConsumerProvider, int i, BipedEntityRenderState state, float f, float g) {
        // If they are already wearing an Elytra, let vanilla handle it (so skins/dyes work)
        if (original.isOf(Items.ELYTRA)) {
            return original;
        }

        if (state instanceof PlayerEntityRenderState playerState) {
            // The owning player's skill data is attached to the server entity; client
            // render states no longer carry it in 1.21.4, so there is no safe read here.
            // Return the original stack — skill elytra visuals are handled server-side.
        }

        return original;
    }

    @Unique
    private static boolean jd_skill_tree$hasElytraSkill(com.jd_skill_tree.api.IUnlockedSkillsData data) {
        for (String skillId : ((IUnlockedSkillsData) data).getUnlockedSkills()) {
            var skillOpt = SkillManager.getSkill(Identifier.of(skillId));
            if (skillOpt.isPresent()) {
                for (SkillEffect effect : skillOpt.get().getEffects()) {
                    if (effect instanceof ElytraSkillEffect) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
