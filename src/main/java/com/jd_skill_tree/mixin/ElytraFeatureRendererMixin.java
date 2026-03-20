package com.jd_skill_tree.mixin;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.effects.ElytraSkillEffect;
import com.jd_skill_tree.skills.effects.SkillEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@OnlyIn(Dist.CLIENT)
@Mixin(ElytraLayer.class)
public class ElytraFeatureRendererMixin {

    @ModifyVariable(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
        at = @At(value = "STORE"), ordinal = 0
    )
    private ItemStack modifyElytraStack(ItemStack original, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, LivingEntity livingEntity,
            float f1, float f2, float f3, float f4, float f5, float f6) {
        if (original.is(Items.ELYTRA)) return original;
        if (livingEntity instanceof Player player) {
            IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
            for (String skillId : skillData.getUnlockedSkills()) {
                var opt = SkillManager.getSkill(new ResourceLocation(skillId));
                if (opt.isPresent()) {
                    for (SkillEffect eff : opt.get().getEffects()) {
                        if (eff instanceof ElytraSkillEffect && eff.isActive(player)) {
                            return new ItemStack(Items.ELYTRA);
                        }
                    }
                }
            }
        }
        return original;
    }
}
