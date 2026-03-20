package com.jd_skill_tree.mixin;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player {

    // Boilerplate constructor
    public ServerPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    public void onCopyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        // Cast both the old and new player to our interface to access the skill data
        IUnlockedSkillsData oldData = (IUnlockedSkillsData) oldPlayer;
        IUnlockedSkillsData newData = (IUnlockedSkillsData) this;

        // Copy the skills from the old player data to the new
        newData.setUnlockedSkills(oldData.getUnlockedSkills());
    }
}