package com.jd_skill_tree.networking;

import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles all server-side networking for the mod.
 * This includes registering packet receivers and player connection events.
 */
public class SkillNetworking {

    // --- Packet IDs ---
    public static final Identifier UNLOCK_SKILL_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "unlock_skill");
    public static final Identifier SKILL_SYNC_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "skill_sync");
    public static final Identifier RESET_SKILLS_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "reset_skills");
    public static final Identifier SAVE_SKILL_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "save_skill");


    /**
     * A single method to register all our server-side networking.
     * Call this from the main mod's onInitialize method.
     */
    public static void register() {
        registerC2SPackets();
        registerServerEvents();
    }

    private static void registerC2SPackets() {
        // --- UNLOCK SKILL PACKET HANDLER ---
        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_SKILL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            Identifier skillId = buf.readIdentifier();
            String skillIdString = skillId.toString();

            server.execute(() -> {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Optional<Skill> skillOpt = SkillManager.getSkill(skillId);

                if (skillOpt.isEmpty()) return;
                Skill skillToUnlock = skillOpt.get();

                if (skillData.hasSkill(skillIdString)) return;

                List<Skill> requiredSkills = skillToUnlock.getRequiredSkills();
                for (Skill requiredSkill : requiredSkills) {
                    if (!skillData.hasSkill(requiredSkill.getId().toString())) return;
                }

                if (player.experienceLevel < skillToUnlock.getCost()) return;

                player.addExperienceLevels(-skillToUnlock.getCost());
                skillData.unlockSkill(skillIdString);
                syncSkillsToClient(player);
                player.sendMessage(Text.of("§aSkill Unlocked: " + skillToUnlock.getName()), false);
            });
        });

        // --- RESET SKILLS PACKET HANDLER ---
        ServerPlayNetworking.registerGlobalReceiver(RESET_SKILLS_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Set<String> unlockedSkills = skillData.getUnlockedSkills();
                if (unlockedSkills.isEmpty()) return;

                int totalRefundAmount = 0;
                for (String skillIdString : unlockedSkills) {
                    Optional<Skill> skillOpt = SkillManager.getSkill(new Identifier(skillIdString));
                    if (skillOpt.isPresent()) {
                        totalRefundAmount += skillOpt.get().getCost();
                    }
                }

                skillData.setUnlockedSkills(new HashSet<>());
                player.addExperienceLevels(totalRefundAmount);
                syncSkillsToClient(player);
                player.sendMessage(Text.of("§eSkills have been reset. " + totalRefundAmount + " levels refunded."), false);
            });
        });

        // --- SAVE SKILL / EXPORT DATAPACK PACKET HANDLER ---
        ServerPlayNetworking.registerGlobalReceiver(SAVE_SKILL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            String namespace = buf.readString();
            String fileName = buf.readString();
            String jsonContent = buf.readString();

            server.execute(() -> {
                // Security Check: Only OPs should be able to write files to the server
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("You need OP permissions to export skills.").formatted(Formatting.RED), false);
                    return;
                }

                try {
                    Path datapackDir = server.getSavePath(WorldSavePath.DATAPACKS);

                    // Create datapack folder structure
                    String datapackName = namespace + "_skills_datapack";
                    Path datapackPath = datapackDir.resolve(datapackName);
                    Path dataPath = datapackPath.resolve("data").resolve(namespace);
                    Path skillsPath = dataPath.resolve("skills");

                    // Create directories
                    Files.createDirectories(skillsPath);

                    // Create pack.mcmeta if it doesn't exist
                    Path packMetaPath = datapackPath.resolve("pack.mcmeta");
                    if (!Files.exists(packMetaPath)) {
                        String packMcmeta = "{\n" +
                                "  \"pack\": {\n" +
                                "    \"pack_format\": 15,\n" +
                                "    \"description\": \"Skills datapack generated by Developer Console\"\n" +
                                "  }\n" +
                                "}";
                        Files.writeString(packMetaPath, packMcmeta, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    }

                    // Clean filename
                    String cleanFileName = fileName.replaceAll("[^a-zA-Z0-9_\\-.]", "");
                    if (!cleanFileName.endsWith(".json")) cleanFileName += ".json";

                    Path skillFilePath = skillsPath.resolve(cleanFileName);
                    Files.writeString(skillFilePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                    player.sendMessage(Text.literal("Exported to: " + datapackName + "/" + cleanFileName).formatted(Formatting.GREEN), false);
                    Jd_skill_tree.LOGGER.info("Exported skill JSON to {}", skillFilePath);

                    // Reload Datapacks
                    server.getCommandManager().executeWithPrefix(server.getCommandSource(), "reload");

                } catch (IOException e) {
                    player.sendMessage(Text.literal("Export failed: " + e.getMessage()).formatted(Formatting.RED), false);
                    Jd_skill_tree.LOGGER.error("Failed to export skill datapack", e);
                }
            });
        });
    }

    private static void registerServerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            syncSkillsToClient(handler.player);
        });
    }

    public static void syncSkillsToClient(ServerPlayerEntity player) {
        Set<String> skills = ((IUnlockedSkillsData) player).getUnlockedSkills();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(skills.size());
        for (String skillId : skills) {
            buf.writeString(skillId);
        }
        ServerPlayNetworking.send(player, SKILL_SYNC_PACKET_ID, buf);
    }
}