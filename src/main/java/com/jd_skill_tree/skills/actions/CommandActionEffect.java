package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommandActionEffect implements SkillActionEffect {
    private final String command;

    public CommandActionEffect(String command) { this.command = command; }

    @Override
    public void execute(PlayerEntity player, World world, BlockPos pos) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return;
        String parsed = command.replace("@p", player.getName().getString())
                .replace("%x%", String.valueOf(pos.getX()))
                .replace("%y%", String.valueOf(pos.getY()))
                .replace("%z%", String.valueOf(pos.getZ()));
        serverPlayer.getServer().getCommandManager().executeWithPrefix(
                serverPlayer.getCommandSource().withLevel(2).withSilent(), parsed);
    }

    public String getCommand() { return command; }
    public static CommandActionEffect fromJson(JsonObject json) {
        return new CommandActionEffect(JsonHelper.getString(json, "command"));
    }
}