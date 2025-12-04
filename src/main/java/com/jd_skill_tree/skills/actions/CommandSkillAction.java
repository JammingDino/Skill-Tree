package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommandSkillAction implements SkillAction {

    private final TriggerType trigger;
    private final String command;

    public CommandSkillAction(TriggerType trigger, String command) {
        this.trigger = trigger;
        this.command = command;
    }

    @Override
    public TriggerType getTrigger() {
        return trigger;
    }

    @Override
    public void execute(PlayerEntity player, World world, BlockPos pos) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Parse placeholders
        String parsedCommand = command
                .replace("@p", serverPlayer.getName().getString())
                .replace("%x%", String.valueOf(pos.getX()))
                .replace("%y%", String.valueOf(pos.getY()))
                .replace("%z%", String.valueOf(pos.getZ()));

        System.out.println("DEBUG: Command Action Triggered. Executing: '" + parsedCommand + "'");

        // Execute AS THE PLAYER with OP Level 2 (Allows cheats/command blocks)
        // This ensures relative coordinates (~ ~ ~) execute at the player's location.
        serverPlayer.getServer().getCommandManager().executeWithPrefix(
                serverPlayer.getCommandSource().withLevel(2).withSilent(),
                parsedCommand
        );
    }

    public String getCommand() { return command; }

    public static CommandSkillAction fromJson(JsonObject json) {
        String triggerStr = JsonHelper.getString(json, "trigger").toUpperCase();
        TriggerType trigger = TriggerType.valueOf(triggerStr);
        String command = JsonHelper.getString(json, "command");
        return new CommandSkillAction(trigger, command);
    }
}