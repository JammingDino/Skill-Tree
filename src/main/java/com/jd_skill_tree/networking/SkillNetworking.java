package com.jd_skill_tree.networking;

import com.google.gson.Gson;
import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillLoader;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.actions.SkillAction;
import com.jd_skill_tree.skills.actions.TriggerType;
import com.jd_skill_tree.utils.ExperienceUtils;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
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
    public static final Identifier UNLOCK_SKILL_PACKET_ID = Identifier.of(Jd_skill_tree.MOD_ID, "unlock_skill");
    public static final Identifier SKILL_SYNC_PACKET_ID = Identifier.of(Jd_skill_tree.MOD_ID, "skill_sync");
    public static final Identifier RESET_SKILLS_PACKET_ID = Identifier.of(Jd_skill_tree.MOD_ID, "reset_skills");
    public static final Identifier SAVE_SKILL_PACKET_ID = Identifier.of(Jd_skill_tree.MOD_ID, "save_skill");
    public static final Identifier SKILL_REGISTRY_SYNC_PACKET_ID = Identifier.of(Jd_skill_tree.MOD_ID, "skill_registry_sync");
    public static final Identifier TRIGGER_ACTIVE_SKILL_PACKET_ID = Identifier.of(Jd_skill_tree.MOD_ID, "trigger_active_skill");
    public static final Identifier COOLDOWN_PACKET_ID = Identifier.of(Jd_skill_tree.MOD_ID, "cooldown_sync");

    // 1.20.5+/1.21.x: Fabric networking v1 raw-buf receivers are gone; moved to payloads.
    // All payloads here are opaque buf blobs (decoded on the receiving side), so every
    // channel gets its own identifier but shares one byte-passthrough payload class.
    // Each channel id must be registered via PayloadTypeRegistry before receivers/senders use it.
    public record OpaquePayload(Identifier id, RegistryByteBuf data) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return new Id<>(id);
        }
    }

    private static void registerChannels() {
        for (Identifier id : new Identifier[]{
                UNLOCK_SKILL_PACKET_ID, SKILL_SYNC_PACKET_ID, RESET_SKILLS_PACKET_ID,
                SAVE_SKILL_PACKET_ID, SKILL_REGISTRY_SYNC_PACKET_ID,
                TRIGGER_ACTIVE_SKILL_PACKET_ID, COOLDOWN_PACKET_ID}) {
            CustomPayload.Id<OpaquePayload> typeId = new CustomPayload.Id<>(id);
            PayloadTypeRegistry.playC2S().register(typeId, codecFor(id));
            PayloadTypeRegistry.playS2C().register(typeId, codecFor(id));
        }
    }

    /**
     * Byte-passthrough codec for the opaque skill channels.
     *
     * The payload id itself is written by the vanilla dispatch codec
     * (CustomPayloadS2CPacket / CustomPayloadC2SPacket), which writes getId() and then
     * hands the rest of the frame to the codec registered here. So the codec must consume
     * exactly the payload region of the frame: 1.21's netty PacketDecoder throws
     * "was larger than I expected, found N bytes extra whilst reading packet" if any bytes
     * are left unread after decode.
     *
     * Round 5 shipped a decoder of `buf -> new OpaquePayload(id, buf)` which read nothing at
     * all, so every custom_payload skill packet was rejected with the whole payload counted
     * as extra bytes (observed: 29767 bytes extra on skill_registry_sync, client kicked on join).
     *
     * Fix: length-prefix the payload blob (writeByteArray/readByteArray) so decode consumes
     * the exact frame and rebuilds an independent buffer positioned at index 0.
     */
    private static PacketCodec<RegistryByteBuf, OpaquePayload> codecFor(Identifier id) {
        return PacketCodec.of(
                (payload, out) -> {
                    RegistryByteBuf src = payload.data();
                    int idx = src.readerIndex();
                    byte[] bytes = new byte[src.readableBytes()];
                    src.readBytes(bytes);
                    src.readerIndex(idx);          // keep the source buf reusable
                    out.writeByteArray(bytes);     // varint length + bytes = one exact frame
                },
                in -> {
                    byte[] bytes = in.readByteArray();   // consumes exactly the payload frame
                    return new OpaquePayload(id, SkillBufs.wrap(bytes, in.getRegistryManager()));
                }
        );
    }

    private static void ensureChannel(Identifier id) {
        // Sending side only needs the id type to be present in the registry; both
        // directions were registered above in registerChannels().
    }

    private static CustomPayload.Id<OpaquePayload> payloadId(Identifier id) {
        return new CustomPayload.Id<>(id);
    }

    private static void send(ServerPlayerEntity player, Identifier id, RegistryByteBuf buf) {
        ServerPlayNetworking.send(player, new OpaquePayload(id, buf));
    }

    private static void registerReceiver(Identifier id, ServerPlayNetworking.PlayPayloadHandler<OpaquePayload> handler) {
        ServerPlayNetworking.registerGlobalReceiver(payloadId(id), handler);
    }

    public static void register() {
        registerChannels();
        registerC2SPackets();
        registerServerEvents();
    }

    private static void registerC2SPackets() {
        registerReceiver(UNLOCK_SKILL_PACKET_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            var buf = payload.data();
            Identifier skillId = buf.readIdentifier();
            String skillIdString = skillId.toString();

            context.server().execute(() -> {
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

                for (SkillAction action : skillToUnlock.getActions()) {
                    if (action.getTrigger() == TriggerType.UNLOCK) {
                        action.run(player, player, player.getWorld(), player.getBlockPos());
                    }
                }
            });
        });

        registerReceiver(RESET_SKILLS_PACKET_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Set<String> unlockedSkills = skillData.getUnlockedSkills();
                if (unlockedSkills.isEmpty()) return;

                int totalRefundAmount = 0;
                for (String skillIdString : unlockedSkills) {
                    Optional<Skill> skillOpt = SkillManager.getSkill(Identifier.of(skillIdString));
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

        registerReceiver(SAVE_SKILL_PACKET_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            var buf = payload.data();
            // ... existing save logic
            String namespace = buf.readString();
            String fileName = buf.readString();
            String jsonContent = buf.readString();

            context.server().execute(() -> {
                if (!player.hasPermissionLevel(2)) return;
                try {
                    Path datapackDir = context.server().getSavePath(WorldSavePath.DATAPACKS);
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
                    context.server().getCommandManager().executeWithPrefix(context.server().getCommandSource(), "reload");
                } catch (IOException e) {
                    Jd_skill_tree.LOGGER.error("Failed export", e);
                }
            });
        });

        registerReceiver(TRIGGER_ACTIVE_SKILL_PACKET_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            var buf = payload.data();
            Identifier skillId = buf.readIdentifier();
            context.server().execute(() -> {
                com.jd_skill_tree.api.IUnlockedSkillsData data = (com.jd_skill_tree.api.IUnlockedSkillsData) player;

                // Verify ownership
                if (data.hasSkill(skillId.toString())) {
                    // FIXED: Call the specific method
                    com.jd_skill_tree.skills.actions.SkillActionHandler.triggerSpecificSkill(
                            player,
                            skillId,
                            com.jd_skill_tree.skills.actions.TriggerType.ACTIVATED,
                            player,
                            player.getWorld(),
                            player.getBlockPos()
                    );
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

        RegistryByteBuf buf = com.jd_skill_tree.networking.SkillBufs.create();
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

        send(player, SKILL_REGISTRY_SYNC_PACKET_ID, buf);
    }

    public static void syncSkillsToClient(ServerPlayerEntity player) {
        Set<String> skills = ((IUnlockedSkillsData) player).getUnlockedSkills();
        RegistryByteBuf buf = com.jd_skill_tree.networking.SkillBufs.create();
        buf.writeInt(skills.size());
        for (String skillId : skills) {
            buf.writeString(skillId);
        }
        send(player, SKILL_SYNC_PACKET_ID, buf);
    }

    public static void sendCooldownPacket(ServerPlayerEntity player, Identifier skillId, int ticks) {
        RegistryByteBuf buf = com.jd_skill_tree.networking.SkillBufs.create();
        buf.writeIdentifier(skillId);
        buf.writeInt(ticks);
        send(player, COOLDOWN_PACKET_ID, buf);
    }
}
