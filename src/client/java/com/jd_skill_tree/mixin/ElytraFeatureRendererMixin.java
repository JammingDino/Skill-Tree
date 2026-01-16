package com.jd_skill_tree.mixin;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.effects.ElytraSkillEffect;
import com.jd_skill_tree.skills.effects.SkillEffect;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {

    /**
     * This intercepts the ItemStack used by the renderer.
     * If the player isn't wearing an Elytra, but has the skill, we swap in a "Fake" Elytra stack.
     */
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private ItemStack modifyElytraStack(ItemStack original, net.minecraft.client.util.math.MatrixStack matrixStack, net.minecraft.client.render.VertexConsumerProvider vertexConsumerProvider, int i, LivingEntity livingEntity, float f, float g, float h, float j, float k, float l) {
        // If they are already wearing an Elytra, let vanilla handle it (so skins/dyes work)
        if (original.isOf(Items.ELYTRA)) {
            return original;
        }

        if (livingEntity instanceof PlayerEntity player) {
            // Check for the skill
            IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
            for (String skillId : skillData.getUnlockedSkills()) {
                var skillOpt = SkillManager.getSkill(new Identifier(skillId));
                if (skillOpt.isPresent()) {
                    for (SkillEffect effect : skillOpt.get().getEffects()) {
                        if (effect instanceof ElytraSkillEffect && effect.isActive(player)) {
                            // Valid skill found! Return a dummy Elytra item so the renderer draws it.
                            return new ItemStack(Items.ELYTRA);
                        }
                    }
                }
            }
        }

        return original;
    }
}