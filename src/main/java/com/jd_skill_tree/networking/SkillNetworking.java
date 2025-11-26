package com.jd_skill_tree.networking;

import com.google.gson.Gson;
import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillLoader;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.utils.ExperienceUtils;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SkillNetworking {

    // Existing IDs
    public static final Identifier UNLOCK_SKILL_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "unlock_skill");
    public static final Identifier SKILL_SYNC_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "skill_sync");
    public static final Identifier RESET_SKILLS_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "reset_skills");
    public static final Identifier SAVE_SKILL_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "save_skill");

    // NEW PACKET ID for syncing definitions
    public static final Identifier SKILL_REGISTRY_SYNC_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "skill_registry_sync");

    public static void register() {
        registerC2SPackets();
        registerServerEvents();
    }

    // ... (Keep registerC2SPackets exactly as it is in your provided code) ...
    private static void registerC2SPackets() {
        // Your existing code for UNLOCK, RESET, and SAVE goes here...
        // (I omitted it to save space, but do not delete it!)

        // --- PASTE YOUR EXISTING C2S CODE HERE ---
        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_SKILL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            Identifier skillId = buf.readIdentifier();
            String skillIdString = skillId.toString();

            server.execute(() -> {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Optional<Skill> skillOpt = SkillManager.getSkill(skillId);

                if (skillOpt.isEmpty()) return;
                Skill skillToUnlock = skillOpt.get();

                // 1. Check if already unlocked
                if (skillData.hasSkill(skillIdString)) return;

                // 2. Check Prerequisites
                List<Skill> requiredSkills = skillToUnlock.getRequiredSkills();
                for (Skill requiredSkill : requiredSkills) {
                    if (!skillData.hasSkill(requiredSkill.getId().toString())) return;
                }

                // 3. XP CHECK (UPDATED TO POINTS)
                int currentTotalXp = ExperienceUtils.getPlayerTotalXp(player);
                int cost = skillToUnlock.getCost();

                if (currentTotalXp < cost) {
                    player.sendMessage(Text.of("§cNot enough XP! Need: " + cost + " Points."), true);
                    return;
                }

                // 4. DEDUCT XP (UPDATED TO POINTS)
                // Passing a negative number to addExperience removes raw points and handles level-down logic automatically
                player.addExperience(-cost);

                // 5. Unlock
                skillData.unlockSkill(skillIdString);
                syncSkillsToClient(player);
                player.sendMessage(Text.of("§aSkill Unlocked: " + skillToUnlock.getName()), false);
            });
        });

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
                player.addExperience(totalRefundAmount);
                syncSkillsToClient(player);
                player.sendMessage(Text.of("§eSkills have been reset. " + totalRefundAmount + " xp points refunded."), false);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SAVE_SKILL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            // ... existing save logic
            String namespace = buf.readString();
            String fileName = buf.readString();
            String jsonContent = buf.readString();

            server.execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                try {
                    Path datapackDir = server.getSavePath(WorldSavePath.DATAPACKS);
                    String datapackName = namespace + "_skills_datapack";
                    Path datapackPath = datapackDir.resolve(datapackName);
                    Path dataPath = datapackPath.resolve("data").resolve(namespace);
                    Path skillsPath = dataPath.resolve("skills");
                    Files.createDirectories(skillsPath);

                    Path packMetaPath = datapackPath.resolve("pack.mcmeta");
                    if (!Files.exists(packMetaPath)) {
                        String packMcmeta = "{\"pack\":{\"pack_format\":15,\"description\":\"Skills datapack\"}}";
                        Files.writeString(packMetaPath, packMcmeta, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    }

                    String cleanFileName = fileName.replaceAll("[^a-zA-Z0-9_\\-.]", "");
                    if (!cleanFileName.endsWith(".json")) cleanFileName += ".json";

                    Path skillFilePath = skillsPath.resolve(cleanFileName);
                    Files.writeString(skillFilePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    server.getCommandManager().executeWithPrefix(server.getCommandSource(), "reload");
                } catch (IOException e) {
                    Jd_skill_tree.LOGGER.error("Failed export", e);
                }
            });
        });
    }

    private static void registerServerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // 1. Sync the definitions (Registry) FIRST
            syncSkillRegistry(handler.player);
            // 2. Then sync the player's unlocked state
            syncSkillsToClient(handler.player);
        });
    }

    /**
     * NEW: Sends all loaded Skill definitions to the client.
     */
    public static void syncSkillRegistry(ServerPlayerEntity player) {
        Collection<Skill> allSkills = SkillManager.getAllSkills();

        PacketByteBuf buf = PacketByteBufs.create();
        // Write size
        buf.writeInt(allSkills.size());

        // Use the GSON instance from SkillLoader to ensure adapters are used
        Gson gson = SkillLoader.GSON;

        for (Skill skill : allSkills) {
            // Write ID
            buf.writeIdentifier(skill.getId());
            // Write JSON string content
            String json = gson.toJson(skill);
            buf.writeString(json);
        }

        ServerPlayNetworking.send(player, SKILL_REGISTRY_SYNC_PACKET_ID, buf);
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