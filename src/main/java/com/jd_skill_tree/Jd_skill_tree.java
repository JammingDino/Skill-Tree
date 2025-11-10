package com.jd_skill_tree;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.ModSkills;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.utils.ModRegistries;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.jd_skill_tree.api.IClientSkillData;

public class Jd_skill_tree implements ModInitializer {

    public static final String MOD_ID = "jd_skill_tree";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Packet IDs
    public static final Identifier UNLOCK_SKILL_PACKET_ID = new Identifier(MOD_ID, "unlock_skill");
    public static final Identifier SKILL_SYNC_PACKET_ID = new Identifier(MOD_ID, "skill_sync");
    public static final Identifier RESET_SKILLS_PACKET_ID = new Identifier(MOD_ID, "reset_skills");

    public static IClientSkillData CLIENT_SKILL_DATA_HANDLER = null;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing " + MOD_ID);

        registerC2SPackets();
        registerResetPacketReceiver();
        registerLoginSync();

        ModRegistries.registerAll();

        LOGGER.info(MOD_ID + " Initialized");
    }


    private void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_SKILL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            String skillId = buf.readString();

            LOGGER.info("[SERVER DEBUG] Received unlock request for skill '{}' from player '{}'", skillId, player.getName().getString());

            server.execute(() -> {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Optional<Skill> skillOpt = ModSkills.getSkillById(skillId);

                // --- SERVER-SIDE VALIDATION CHECKS ---

                // 1. Check if the skill is valid
                if (skillOpt.isEmpty()) {
                    LOGGER.warn("[SERVER DEBUG] Check failed: Skill '{}' does not exist.", skillId);
                    return;
                }

                Skill skillToUnlock = skillOpt.get();

                // 2. Check if player already has the skill
                if (skillData.hasSkill(skillId)) {
                    LOGGER.warn("[SERVER DEBUG] Check failed: Player '{}' already has skill '{}'.", player.getName().getString(), skillId);
                    return;
                }

                // 3. Check for ALL prerequisites
                List<Skill> requiredSkills = skillToUnlock.getRequiredSkills();
                for (Skill requiredSkill : requiredSkills) {
                    if (!skillData.hasSkill(requiredSkill.getId())) {
                        LOGGER.warn("[SERVER DEBUG] Check failed: Player '{}' is missing prerequisite '{}' for skill '{}'.",
                                player.getName().getString(), requiredSkill.getId(), skillId);
                        return;
                    }
                }

                // 4. Check for sufficient experience
                if (player.experienceLevel < skillToUnlock.getCost()) {
                    LOGGER.warn("[SERVER DEBUG] Check failed: Player '{}' needs {} levels for skill '{}' but only has {}.",
                            player.getName().getString(), skillToUnlock.getCost(), skillId, player.experienceLevel);
                    return;
                }

                // --- ALL CHECKS PASSED ---
                LOGGER.info("[SERVER DEBUG] All checks passed for player '{}'. Unlocking skill '{}'.", player.getName().getString(), skillId);

                player.addExperienceLevels(-skillToUnlock.getCost());
                skillData.unlockSkill(skillId);

                // Manually sync data back to the client
                syncSkillsToClient(player);

                player.sendMessage(Text.of("§aSkill Unlocked: " + skillToUnlock.getTitle()), false);
            });
        });
    }

    private void registerResetPacketReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(RESET_SKILLS_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            LOGGER.info("[SERVER DEBUG] Received skill reset request from player '{}'", player.getName().getString());

            server.execute(() -> {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Set<String> unlockedSkills = skillData.getUnlockedSkills();

                if (unlockedSkills.isEmpty()) {
                    LOGGER.warn("[SERVER DEBUG] Player '{}' tried to reset but has no skills.", player.getName().getString());
                    return;
                }

                // 1. Calculate the total XP cost to refund
                int totalRefundAmount = 0;
                for (String skillId : unlockedSkills) {
                    Optional<Skill> skillOpt = ModSkills.getSkillById(skillId);
                    if (skillOpt.isPresent()) {
                        totalRefundAmount += skillOpt.get().getCost();
                    }
                }

                // 2. Revoke all skills by setting the data to an empty set
                skillData.setUnlockedSkills(new HashSet<>());

                // 3. Refund the experience levels
                player.addExperienceLevels(totalRefundAmount);

                // 4. Sync the now-empty skill list back to the client
                syncSkillsToClient(player);

                // 5. Send a confirmation message
                player.sendMessage(Text.of("§eSkills have been reset. " + totalRefundAmount + " levels refunded."), false);
                LOGGER.info("[SERVER DEBUG] Player '{}' reset skills. Refunded {} levels.", player.getName().getString(), totalRefundAmount);
            });
        });
    }

    private void registerLoginSync() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Sync skills when a player joins
            LOGGER.info("[SERVER DEBUG] Player '{}' has joined. Syncing skill data.", handler.player.getName().getString());
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

        LOGGER.info("[SERVER DEBUG] Syncing {} skills to client '{}'.", skills.size(), player.getName().getString());
        ServerPlayNetworking.send(player, SKILL_SYNC_PACKET_ID, buf);
    }
}