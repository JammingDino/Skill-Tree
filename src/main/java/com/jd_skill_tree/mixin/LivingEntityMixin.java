package com.jd_skill_tree.mixin;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.effects.SkillEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.HashSet;
import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private Set<SkillEffect> jd_skill_tree$getActiveEffects(PlayerEntity player) {
        Set<SkillEffect> effects = new HashSet<>();
        IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
        for (String skillIdString : skillData.getUnlockedSkills()) {
            SkillManager.getSkill(new Identifier(skillIdString)).ifPresent(skill -> {
                effects.addAll(skill.getEffects());
            });
        }
        return effects;
    }

    /**
     * Targeting the call to updateVelocity inside the Water check block in travel().
     * The 'ordinal = 0' must be INSIDE the @At annotation.
     * We modify index 0 (the float speed).
     */
    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;updateVelocity(FLnet/minecraft/util/math/Vec3d;)V", ordinal = 0))
    private float modifySwimSpeed(float speed) {
        if ((Object)this instanceof PlayerEntity player) {
            float newSpeed = speed;
            for (SkillEffect effect : jd_skill_tree$getActiveEffects(player)) {
                newSpeed = effect.modifySwimSpeed(player, newSpeed);
            }
            return newSpeed;
        }
        return speed;
    }

    /**
     * Targeting the call to updateVelocity inside the Lava check block in travel().
     * This corresponds to Ordinal 1.
     */
    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;updateVelocity(FLnet/minecraft/util/math/Vec3d;)V", ordinal = 1))
    private float modifyLavaSpeed(float speed) {
        if ((Object)this instanceof PlayerEntity player) {
            float newSpeed = speed;
            for (SkillEffect effect : jd_skill_tree$getActiveEffects(player)) {
                newSpeed = effect.modifyLavaSpeed(player, newSpeed);
            }
            return newSpeed;
        }
        return speed;
    }
}