package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.level.Level;

public class CommandActionEffect implements SkillActionEffect {
    private final String command;

    public CommandActionEffect(String command) { this.command = command; }

    @Override
    public void execute(Entity target, Level world, BlockPos pos) {
        if (world.isClientSide) return;
        MinecraftServer server = world.getServer();
        if (server == null) return;

        // 1. Determine Executor Details
        // If target is null, default to generic values
        Vec2f rotation = (target != null) ? target.getRotationClient() : Vec2f.ZERO;
        String name = (target != null) ? target.getName().getString() : "Server";

        // 2. Construct Source
        // CRITICAL FIX: We use 'pos.toCenterPos()' to move execution to the passed BlockPos
        CommandSourceStack source = server.getCommandSource()
                .withWorld((ServerLevel) world)
                .withPosition(pos.toCenterPos()) // <--- Executes at the Raycast Hit location
                .withRotation(rotation)
                .withLevel(2)
                .withSilent();

        if (target != null) {
            source = source.withEntity(target); // @s is still the Player
        }

        // 3. Parse placeholders
        String parsed = command
                .replace("@p", name)
                .replace("%target%", name)
                .replace("%x%", String.valueOf(pos.getX()))
                .replace("%y%", String.valueOf(pos.getY()))
                .replace("%z%", String.valueOf(pos.getZ()));

        if (target != null) {
            parsed = parsed.replace("%uuid%", target.getStringUUID());
        }

        // 4. Execute
        server.getCommands().performPrefixedCommand(source, parsed);
    }

    public String getCommand() { return command; }

    public static CommandActionEffect fromJson(JsonObject json) {
        return new CommandActionEffect(GsonHelper.getString(json, "command"));
    }
}