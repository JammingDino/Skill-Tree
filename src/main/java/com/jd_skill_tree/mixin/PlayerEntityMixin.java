package com.jd_skill_tree.mixin;

import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.ModSkills;
import com.jd_skill_tree.skills.Skill;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IUnlockedSkillsData {

    private static final UUID MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("a8a0d5e8-5a3d-4b3e-8a9a-3e9e1c0c1c0c");

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {

        /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /// Movement Modify Script
        /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        PlayerEntity player = (PlayerEntity) (Object) this;
        EntityAttributeInstance movementSpeedAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeedAttribute == null) {
            return;
        }

        // Remove the old modifier before recalculating
        movementSpeedAttribute.removeModifier(MOVEMENT_SPEED_MODIFIER_ID);

        float movementSpeedBonus = 0.0f;
        float sneakSpeedBonus = 0.0f;

        for (Map.Entry<String, Float> entry : ModSkills.getMovementSpeedBonuses().entrySet()) {
            String skillId = entry.getKey();
            float bonus = entry.getValue();
            boolean hasSkill;

            if (player.getWorld().isClient()) {
                if (Jd_skill_tree.CLIENT_SKILL_DATA_HANDLER != null) {
                    Skill skill = ModSkills.getSkill(skillId);
                    hasSkill = Jd_skill_tree.CLIENT_SKILL_DATA_HANDLER.isSkillUnlocked(skill);
                } else {
                    hasSkill = false;
                }
            } else {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) this;
                hasSkill = skillData.hasSkill(skillId);
            }

            if (hasSkill) {
                movementSpeedBonus += bonus;
            }
        }

        for (Map.Entry<String, Float> entry : ModSkills.getSneakSpeedBonuses().entrySet()) {
            String skillId = entry.getKey();
            float bonus = entry.getValue();
            boolean hasSkill;

            if (player.getWorld().isClient()) {
                if (Jd_skill_tree.CLIENT_SKILL_DATA_HANDLER != null) {
                    Skill skill = ModSkills.getSkill(skillId);
                    hasSkill = Jd_skill_tree.CLIENT_SKILL_DATA_HANDLER.isSkillUnlocked(skill);
                } else {
                    hasSkill = false;
                }
            } else {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) this;
                hasSkill = skillData.hasSkill(skillId);
            }

            if (hasSkill) {
                sneakSpeedBonus += bonus;
            }
        }

        float totalMovementBonus = movementSpeedBonus;
        if (player.isSneaking()) {
            totalMovementBonus += sneakSpeedBonus;
        }

        if (totalMovementBonus > 0) {
            EntityAttributeModifier movementSpeedModifier = new EntityAttributeModifier(
                    MOVEMENT_SPEED_MODIFIER_ID,
                    "Skill Tree Movement Speed Bonus",
                    totalMovementBonus,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL
            );
            movementSpeedAttribute.addPersistentModifier(movementSpeedModifier);
        }


    }


    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void onGetBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        float speed = cir.getReturnValue();

        for (Map.Entry<String, Float> entry : ModSkills.getMiningSpeedBonuses().entrySet()) {
            String skillId = entry.getKey();
            float bonus = entry.getValue();
            boolean hasSkill = false;

            if (player.getWorld().isClient()) {
                // On the CLIENT, call our handler.
                // We add a null check just to be safe.
                if (Jd_skill_tree.CLIENT_SKILL_DATA_HANDLER != null) {
                    Skill skill = ModSkills.getSkill(skillId);
                    hasSkill = Jd_skill_tree.CLIENT_SKILL_DATA_HANDLER.isSkillUnlocked(skill);
                }
            } else {
                // On the SERVER, use the interface as before.
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) this;
                hasSkill = skillData.hasSkill(skillId);
            }

            if (hasSkill) {
                speed *= (1.0f + bonus);
            }
        }

        cir.setReturnValue(speed);
    }

    // This is the new field we are adding to the PlayerEntity class
    private final Set<String> unlockedSkills = new HashSet<>();

    // Boilerplate constructor for mixin
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // --- Implement the interface methods  ---

    @Override
    public Set<String> getUnlockedSkills() {
        return new HashSet<>(this.unlockedSkills); // Return a copy
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
            if (skillsData.contains("unlockedSkills", 9)) { // 9 is the NBT type for List
                NbtList skillList = skillsData.getList("unlockedSkills", 8); // 8 is the NBT type for String
                for (int i = 0; i < skillList.size(); i++) {
                    this.unlockedSkills.add(skillList.getString(i));
                }
            }
        }
    }
}