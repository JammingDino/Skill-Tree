package com.jd_skill_tree.mixin;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.actions.SkillActionHandler;
import com.jd_skill_tree.skills.effects.AttributeSkillEffect;
import com.jd_skill_tree.skills.effects.SkillEffect;
import com.jd_skill_tree.skills.effects.EnchantmentSkillEffect;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IUnlockedSkillsData {

    // Helper method to get all active effects for the player
    private Set<SkillEffect> jd_skill_tree$getActiveEffects() {
        Set<SkillEffect> effects = new HashSet<>();
        IUnlockedSkillsData skillData = (IUnlockedSkillsData) this;
        PlayerEntity player = (PlayerEntity) (Object) this;

        for (String skillIdString : skillData.getUnlockedSkills()) {
            SkillManager.getSkill(new Identifier(skillIdString)).ifPresent(skill -> {

                // Iterate all effects and check INDIVIDUALLY
                for (SkillEffect effect : skill.getEffects()) {
                    if (effect.isActive(player)) { // This checks the specific condition
                        effects.add(effect);
                    }
                }
            });
        }
        return effects;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        EnchantmentSkillEffect.updateEnchantments(player);
        SkillActionHandler.handleTimerActions(player);

        // --- ATTRIBUTE MODIFIER HANDLING ---

        // 1. Clean Slate: Find and remove all modifiers that were added by our mod in previous ticks.
        for (EntityAttribute attribute : SkillManager.getAffectedAttributes()) {
            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance != null) {
                // We must collect the modifiers to remove first to avoid modifying a list while iterating over it.
                List<EntityAttributeModifier> modifiersToRemove = new ArrayList<>();
                for (EntityAttributeModifier modifier : instance.getModifiers()) {
                    // Identify our modifiers by the name we gave them when we created them.
                    if (AttributeSkillEffect.MODIFIER_NAME.equals(modifier.getName())) {
                        modifiersToRemove.add(modifier);
                    }
                }
                // Now, remove them by their actual UUIDs. This is the correct API usage.
                modifiersToRemove.forEach(modifier -> instance.removeModifier(modifier.getId()));
            }
        }

        // 2. Aggregate: Calculate the total bonus from all currently active skills.
        // (This part of the logic was correct and remains the same)
        Map<EntityAttribute, Map<EntityAttributeModifier.Operation, Double>> modifiersToApply = new HashMap<>();
        Set<SkillEffect> activeEffects = jd_skill_tree$getActiveEffects();
        activeEffects.stream()
                .filter(effect -> effect instanceof AttributeSkillEffect)
                .map(effect -> (AttributeSkillEffect) effect)
                .forEach(effect -> {
                    Map<EntityAttributeModifier.Operation, Double> operationMap = modifiersToApply.computeIfAbsent(effect.getAttribute(), k -> new HashMap<>());
                    operationMap.merge(effect.getOperation(), effect.getValue(), Double::sum);
                });

        // 3. Re-apply: Add one new, combined modifier for each aggregated bonus.
        // (This part of the logic was correct and remains the same)
        modifiersToApply.forEach((attribute, operationMap) -> {
            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance != null) {
                operationMap.forEach((operation, value) -> {
                    UUID modifierUuid = UUID.nameUUIDFromBytes((Registries.ATTRIBUTE.getId(attribute).toString() + operation.toString()).getBytes());
                    instance.addPersistentModifier(new EntityAttributeModifier(
                            modifierUuid,
                            AttributeSkillEffect.MODIFIER_NAME, // Apply with our special name
                            value,
                            operation
                    ));
                });
            }
        });

        // 4. Handle other tick-based effects (non-attributes)
        // (This part of the logic was correct and remains the same)
        for (SkillEffect effect : activeEffects) {
            effect.onTick(player);
        }

        boolean hasFlightSkill = false;

        // 1. Check all active effects
        for (SkillEffect effect : jd_skill_tree$getActiveEffects()) {
            if (effect instanceof com.jd_skill_tree.skills.effects.CreativeFlightSkillEffect) {
                hasFlightSkill = true;
                break;
            }
            // Run standard tick logic for other effects (like Potions)
            effect.onTick(player);
        }

        // 2. Apply Flight Logic
        // We only interfere if the player is NOT in Creative/Spectator
        if (!player.isCreative() && !player.isSpectator()) {
            boolean canFly = hasFlightSkill;

            if (player.getAbilities().allowFlying != canFly) {
                player.getAbilities().allowFlying = canFly;
                if (!canFly) {
                    player.getAbilities().flying = false; // Stop flying immediately if skill lost
                }
                player.sendAbilitiesUpdate(); // Sync to client
            }
        }
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void onGetBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        float speed = cir.getReturnValue();

        // Generic loop that applies all mining speed effects
        for (SkillEffect effect : jd_skill_tree$getActiveEffects()) {
            speed = effect.modifyBreakSpeed(block, (PlayerEntity) (Object) this, speed);
        }

        cir.setReturnValue(speed);
    }

    /**
     * This intercepts the local variable 'float n' (or 'f' in some mappings) which represents knockback strength.
     * It is usually stored just before the "if (n > 0)" check.
     */
    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 0)
    private int modifyAttackKnockback(int knockbackLevel, Entity target) {
        // Convert to float for calculations
        float newKnockback = (float) knockbackLevel;

        // Loop through all skills and apply modifiers
        for (SkillEffect effect : jd_skill_tree$getActiveEffects()) {
            newKnockback = effect.modifyAttackKnockback((PlayerEntity)(Object)this, newKnockback);
        }

        // Handle "Pull" (Negative Knockback)
        if (newKnockback < 0) {
            float strength = -newKnockback; // Make strength positive for the physics method

            // Standard Vanilla vectors based on player rotation
            float yaw = this.getYaw() * 0.017453292F;
            float sin = MathHelper.sin(yaw);
            float cos = MathHelper.cos(yaw);

            if (target instanceof LivingEntity livingTarget) {
                // Vanilla 'takeKnockback' ignores values <= 0.
                // FIX: Pass POSITIVE strength, but INVERT the direction vectors (-sin, -(-cos)) -> (-sin, +cos)
                // Standard Push: (strength, sin, -cos)
                // Pull:          (strength, -sin, cos)
                livingTarget.takeKnockback(strength * 0.5F, -sin, cos);
            } else {
                // For non-living entities, we can just do raw velocity math.
                // Logic: -(-sin) becomes +sin, etc. forcing it backwards relative to look direction.
                target.addVelocity(
                        sin * strength * 0.5F,
                        0.1D,
                        -cos * strength * 0.5F
                );
            }

            // Apply self-slowdown (Vanilla mechanic)
            this.setVelocity(this.getVelocity().multiply(0.6D, 1.0D, 0.6D));
            this.setSprinting(false);

            // Return 0 so vanilla logic skips the standard push behavior
            return 0;
        }

        return (int) newKnockback;
    }

    @ModifyVariable(method = "addExperience", at = @At("HEAD"), argsOnly = true)
    private int modifyAddedExperience(int experience) {
        // We only modify POSITIVE experience gain.
        // We don't want to multiply XP costs (negative values) used for repairing mending gear or commands.
        if (experience <= 0) return experience;

        int newXp = experience;

        for (SkillEffect effect : jd_skill_tree$getActiveEffects()) {
            newXp = effect.modifyExperience((PlayerEntity)(Object)this, newXp);
        }

        return newXp;
    }


    // --- NBT and Interface Implementation (No changes below this line) ---

    private final Set<String> unlockedSkills = new HashSet<>();

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public Set<String> getUnlockedSkills() {
        return new HashSet<>(this.unlockedSkills);
    }

    @Override
    public void unlockSkill(String skillId) {
        this.unlockedSkills.add(skillId);
    }

    @Override
    public boolean hasSkill(String skillId) {
        return this.unlockedSkills.contains(skillId);
    }

    @Override
    public void setUnlockedSkills(Set<String> skills) {
        this.unlockedSkills.clear();
        this.unlockedSkills.addAll(skills);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void onWriteCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        NbtList skillList = new NbtList();
        for (String skillId : this.unlockedSkills) {
            skillList.add(NbtString.of(skillId));
        }
        NbtCompound skillsData = new NbtCompound();
        skillsData.put("unlockedSkills", skillList);
        nbt.put("jd_skill_tree_data", skillsData);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void onReadCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        this.unlockedSkills.clear();
        if (nbt.contains("jd_skill_tree_data")) {
            NbtCompound skillsData = nbt.getCompound("jd_skill_tree_data");
            if (skillsData.contains("unlockedSkills", 9)) {
                NbtList skillList = skillsData.getList("unlockedSkills", 8);
                for (int i = 0; i < skillList.size(); i++) {
                    this.unlockedSkills.add(skillList.getString(i));
                }
            }
        }
    }

    @Override
    public void revokeSkill(String skillId) {
        this.unlockedSkills.remove(skillId);
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamageTaken(net.minecraft.entity.damage.DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // If damage was successful (returnValue is true)
        if (cir.getReturnValue()) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (!player.getWorld().isClient) {

                // 1. Trigger "On Take Damage (Self)"
                // Target is the player (for healing, buffs, etc.)
                com.jd_skill_tree.skills.actions.SkillActionHandler.triggerActions(
                        player,
                        com.jd_skill_tree.skills.actions.TriggerType.TAKE_DAMAGE_SELF,
                        player,
                        player.getWorld(),
                        player.getBlockPos()
                );

                // 2. Trigger "On Take Damage (Attacker)" - Thorns logic
                // Check if there is an actual attacker entity (Skeleton, Zombie, Player)
                net.minecraft.entity.Entity attacker = source.getAttacker();
                if (attacker != null) {
                    com.jd_skill_tree.skills.actions.SkillActionHandler.triggerActions(
                            player, // Owner of the skill is still the victim
                            com.jd_skill_tree.skills.actions.TriggerType.TAKE_DAMAGE_ATTACKER,
                            attacker, // Target is the enemy
                            player.getWorld(),
                            attacker.getBlockPos()
                    );
                }
            }
        }
    }
}