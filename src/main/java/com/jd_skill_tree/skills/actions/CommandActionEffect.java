package com.jd_skill_tree.skills.actions;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CommandActionEffect implements SkillActionEffect {
    private final String command;

    public CommandActionEffect(String command) { this.command = command; }

    @Override
    public void execute(Entity target, World world, BlockPos pos) {
        if (world.isClient || target == null) return;
        MinecraftServer server = world.getServer();
        if (server == null) return;

        // 1. Determine precise coordinates
        Vec3d coords = target.getPos();
        Vec2f rotation = target.getRotationClient();
        String name = target.getName().getString();

        // 2. Construct Source using Helper Methods
        // We start with the server's generic source, then chain modifiers to anchor it to the target.
        ServerCommandSource source = server.getCommandSource()
                .withWorld((ServerWorld) world)
                .withPosition(coords)
                .withRotation(rotation)
                .withLevel(2)
                .withSilent()
                .withEntity(target); // This is crucial: sets the 'executor' for @s selector

        // 3. Parse explicit placeholders
        String parsed = command
                .replace("@p", name)
                .replace("%target%", name)
                .replace("%uuid%", target.getUuidAsString())
                .replace("%x%", String.valueOf(coords.x))
                .replace("%y%", String.valueOf(coords.y))
                .replace("%z%", String.valueOf(coords.z));

        // 4. Execute
        server.getCommandManager().executeWithPrefix(source, parsed);
    }

    public String getCommand() { return command; }

    public static CommandActionEffect fromJson(JsonObject json) {
        return new CommandActionEffect(JsonHelper.getString(json, "command"));
    }
}