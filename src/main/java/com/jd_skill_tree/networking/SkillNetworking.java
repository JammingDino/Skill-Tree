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
import net.minecraft.util.Identifier;

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
    // It's good practice to keep the IDs in the class that uses them.
    public static final Identifier UNLOCK_SKILL_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "unlock_skill");
    public static final Identifier SKILL_SYNC_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "skill_sync");
    public static final Identifier RESET_SKILLS_PACKET_ID = new Identifier(Jd_skill_tree.MOD_ID, "reset_skills");


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

            Jd_skill_tree.LOGGER.info("[SERVER DEBUG] Received unlock request for skill '{}' from player '{}'", skillIdString, player.getName().getString());

            server.execute(() -> {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Optional<Skill> skillOpt = SkillManager.getSkill(skillId);

                if (skillOpt.isEmpty()) {
                    Jd_skill_tree.LOGGER.warn("[SERVER DEBUG] Check failed: Skill '{}' does not exist.", skillIdString);
                    return;
                }
                Skill skillToUnlock = skillOpt.get();

                if (skillData.hasSkill(skillIdString)) {
                    Jd_skill_tree.LOGGER.warn("[SERVER DEBUG] Check failed: Player '{}' already has skill '{}'.", player.getName().getString(), skillIdString);
                    return;
                }

                List<Skill> requiredSkills = skillToUnlock.getRequiredSkills();
                for (Skill requiredSkill : requiredSkills) {
                    if (!skillData.hasSkill(requiredSkill.getId().toString())) {
                        Jd_skill_tree.LOGGER.warn("[SERVER DEBUG] Check failed: Player '{}' is missing prerequisite '{}' for skill '{}'.",
                                player.getName().getString(), requiredSkill.getId().toString(), skillIdString);
                        return;
                    }
                }

                if (player.experienceLevel < skillToUnlock.getCost()) {
                    Jd_skill_tree.LOGGER.warn("[SERVER DEBUG] Check failed: Player '{}' needs {} levels for skill '{}' but only has {}.",
                            player.getName().getString(), skillToUnlock.getCost(), skillIdString, player.experienceLevel);
                    return;
                }

                player.addExperienceLevels(-skillToUnlock.getCost());
                skillData.unlockSkill(skillIdString);
                syncSkillsToClient(player);
                player.sendMessage(Text.of("§aSkill Unlocked: " + skillToUnlock.getName()), false);
            });
        });

        // --- RESET SKILLS PACKET HANDLER ---
        ServerPlayNetworking.registerGlobalReceiver(RESET_SKILLS_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            Jd_skill_tree.LOGGER.info("[SERVER DEBUG] Received skill reset request from player '{}'", player.getName().getString());

            server.execute(() -> {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Set<String> unlockedSkills = skillData.getUnlockedSkills();
                if (unlockedSkills.isEmpty()) {
                    return;
                }

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
    }

    private static void registerServerEvents() {
        // --- PLAYER JOIN EVENT ---
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Jd_skill_tree.LOGGER.info("[SERVER DEBUG] Player '{}' has joined. Syncing skill data.", handler.player.getName().getString());
            syncSkillsToClient(handler.player);
        });
    }

    /**
     * Sends the complete skill data set to a specific client.
     */
    public static void syncSkillsToClient(ServerPlayerEntity player) {
        Set<String> skills = ((IUnlockedSkillsData) player).getUnlockedSkills();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(skills.size());
        for (String skillId : skills) {
            buf.writeString(skillId);
        }

        Jd_skill_tree.LOGGER.info("[SERVER DEBUG] Syncing {} skills to client '{}'.", skills.size(), player.getName().getString());
        ServerPlayNetworking.send(player, SKILL_SYNC_PACKET_ID, buf);
    }
}