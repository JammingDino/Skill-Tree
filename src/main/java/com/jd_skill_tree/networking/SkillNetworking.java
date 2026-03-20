package com.jd_skill_tree.networking;

import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillLoader;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.ModSkills;
import com.jd_skill_tree.skills.actions.SkillAction;
import com.jd_skill_tree.skills.actions.SkillActionHandler;
import com.jd_skill_tree.skills.actions.TriggerType;
import com.jd_skill_tree.utils.ExperienceUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Supplier;

public class SkillNetworking {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Jd_skill_tree.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, UnlockSkillMessage.class,
                UnlockSkillMessage::encode, UnlockSkillMessage::decode,
                UnlockSkillMessage::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ResetSkillsMessage.class,
                ResetSkillsMessage::encode, ResetSkillsMessage::decode,
                ResetSkillsMessage::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, SaveSkillMessage.class,
                SaveSkillMessage::encode, SaveSkillMessage::decode,
                SaveSkillMessage::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, TriggerActiveSkillMessage.class,
                TriggerActiveSkillMessage::encode, TriggerActiveSkillMessage::decode,
                TriggerActiveSkillMessage::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, SyncSkillsMessage.class,
                SyncSkillsMessage::encode, SyncSkillsMessage::decode,
                SyncSkillsMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, SyncSkillRegistryMessage.class,
                SyncSkillRegistryMessage::encode, SyncSkillRegistryMessage::decode,
                SyncSkillRegistryMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, CooldownSyncMessage.class,
                CooldownSyncMessage::encode, CooldownSyncMessage::decode,
                CooldownSyncMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    // ── SERVER→CLIENT ─────────────────────────────────────────────────────────

    public static void syncSkillsToClient(ServerPlayer player) {
        Set<String> skills = ((IUnlockedSkillsData) player).getUnlockedSkills();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSkillsMessage(new HashSet<>(skills)));
    }

    public static void syncSkillRegistry(ServerPlayer player) {
        Collection<Skill> allSkills = SkillManager.getAllSkills();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSkillRegistryMessage(allSkills));
    }

    public static void sendCooldownPacket(ServerPlayer player, ResourceLocation skillId, int ticks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CooldownSyncMessage(skillId, ticks));
    }

    // ── MESSAGES ──────────────────────────────────────────────────────────────

    // CLIENT→SERVER: Unlock a skill
    public static class UnlockSkillMessage {
        private final ResourceLocation skillId;
        public UnlockSkillMessage(ResourceLocation skillId) { this.skillId = skillId; }
        public static void encode(UnlockSkillMessage msg, FriendlyByteBuf buf) { buf.writeResourceLocation(msg.skillId); }
        public static UnlockSkillMessage decode(FriendlyByteBuf buf) { return new UnlockSkillMessage(buf.readResourceLocation()); }
        public static void handle(UnlockSkillMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                String skillIdString = msg.skillId.toString();
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Optional<Skill> skillOpt = SkillManager.getSkill(msg.skillId);
                if (skillOpt.isEmpty()) return;
                Skill skillToUnlock = skillOpt.get();
                if (skillData.hasSkill(skillIdString)) return;
                for (Skill req : skillToUnlock.getRequiredSkills()) {
                    if (!skillData.hasSkill(req.getId().toString())) return;
                }
                int cost = skillToUnlock.getCost();
                if (ExperienceUtils.getPlayerTotalXp(player) < cost) {
                    player.displayClientMessage(Component.literal("§cNot enough XP! Need: " + cost + " Points."), true);
                    return;
                }
                player.giveExperiencePoints(-cost);
                skillData.unlockSkill(skillIdString);
                syncSkillsToClient(player);
                player.sendSystemMessage(Component.literal("§aSkill unlocked: §f" + skillToUnlock.getName()));
                for (SkillAction action : skillToUnlock.getActions()) {
                    if (action.getTrigger() == TriggerType.UNLOCK) {
                        action.run(player, player, player.level(), player.blockPosition());
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    // CLIENT→SERVER: Reset all skills
    public static class ResetSkillsMessage {
        public static void encode(ResetSkillsMessage msg, FriendlyByteBuf buf) {}
        public static ResetSkillsMessage decode(FriendlyByteBuf buf) { return new ResetSkillsMessage(); }
        public static void handle(ResetSkillsMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                IUnlockedSkillsData skillData = (IUnlockedSkillsData) player;
                Set<String> unlockedSkills = skillData.getUnlockedSkills();
                if (unlockedSkills.isEmpty()) return;
                int totalRefund = 0;
                for (String sid : unlockedSkills) {
                    Optional<Skill> opt = SkillManager.getSkill(new ResourceLocation(sid));
                    if (opt.isPresent()) totalRefund += opt.get().getCost();
                }
                skillData.setUnlockedSkills(new HashSet<>());
                player.giveExperiencePoints(totalRefund);
                syncSkillsToClient(player);
                player.sendSystemMessage(Component.literal("§eSkills reset. XP refunded: §f" + totalRefund));
            });
            ctx.setPacketHandled(true);
        }
    }

    // CLIENT→SERVER: Save a skill JSON to the server's datapack
    public static class SaveSkillMessage {
        private final String namespace, fileName, jsonContent;
        public SaveSkillMessage(String namespace, String fileName, String jsonContent) {
            this.namespace = namespace; this.fileName = fileName; this.jsonContent = jsonContent;
        }
        public static void encode(SaveSkillMessage msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.namespace); buf.writeUtf(msg.fileName); buf.writeUtf(msg.jsonContent, 262144);
        }
        public static SaveSkillMessage decode(FriendlyByteBuf buf) {
            return new SaveSkillMessage(buf.readUtf(), buf.readUtf(), buf.readUtf(262144));
        }
        public static void handle(SaveSkillMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null || !player.hasPermissions(2)) return;
                try {
                    Path datapackDir = player.getServer().getWorldPath(LevelResource.DATAPACK_DIR);
                    String datapackName = msg.namespace + "_skills_datapack";
                    Path skillsPath = datapackDir.resolve(datapackName).resolve("data").resolve(msg.namespace).resolve("skills");
                    Files.createDirectories(skillsPath);
                    Path packMeta = datapackDir.resolve(datapackName).resolve("pack.mcmeta");
                    if (!Files.exists(packMeta)) {
                        Files.writeString(packMeta, "{\"pack\":{\"pack_format\":15,\"description\":\"Skills datapack\"}}",
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    }
                    String cleanName = msg.fileName.replaceAll("[^a-zA-Z0-9_\\-.]", "");
                    if (!cleanName.endsWith(".json")) cleanName += ".json";
                    Files.writeString(skillsPath.resolve(cleanName), msg.jsonContent,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    player.getServer().getCommands().performPrefixedCommand(
                            player.getServer().createCommandSourceStack(), "reload");
                } catch (IOException e) { Jd_skill_tree.LOGGER.error("Failed to save skill", e); }
            });
            ctx.setPacketHandled(true);
        }
    }

    // CLIENT→SERVER: Trigger an active skill
    public static class TriggerActiveSkillMessage {
        private final ResourceLocation skillId;
        public TriggerActiveSkillMessage(ResourceLocation skillId) { this.skillId = skillId; }
        public static void encode(TriggerActiveSkillMessage msg, FriendlyByteBuf buf) { buf.writeResourceLocation(msg.skillId); }
        public static TriggerActiveSkillMessage decode(FriendlyByteBuf buf) { return new TriggerActiveSkillMessage(buf.readResourceLocation()); }
        public static void handle(TriggerActiveSkillMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                IUnlockedSkillsData data = (IUnlockedSkillsData) player;
                if (data.hasSkill(msg.skillId.toString())) {
                    SkillActionHandler.triggerSpecificSkill(player, msg.skillId, TriggerType.ACTIVATED,
                            player, player.level(), player.blockPosition());
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    // SERVER→CLIENT: Sync unlocked skill IDs
    public static class SyncSkillsMessage {
        private final Set<String> skills;
        public SyncSkillsMessage(Set<String> skills) { this.skills = skills; }
        public static void encode(SyncSkillsMessage msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.skills.size());
            for (String s : msg.skills) buf.writeUtf(s);
        }
        public static SyncSkillsMessage decode(FriendlyByteBuf buf) {
            int size = buf.readInt();
            Set<String> skills = new HashSet<>();
            for (int i = 0; i < size; i++) skills.add(buf.readUtf());
            return new SyncSkillsMessage(skills);
        }
        public static void handle(SyncSkillsMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                com.jd_skill_tree.skills.ClientSkillData.setUnlockedSkills(msg.skills);
                if (mc.player != null) ((IUnlockedSkillsData) mc.player).setUnlockedSkills(msg.skills);
            });
            ctx.setPacketHandled(true);
        }
    }

    // SERVER→CLIENT: Sync skill registry (definitions)
    public static class SyncSkillRegistryMessage {
        private final List<SkillEntry> entries;
        private record SkillEntry(ResourceLocation id, String json) {}
        public SyncSkillRegistryMessage(Collection<Skill> skills) {
            this.entries = new ArrayList<>();
            for (Skill s : skills) entries.add(new SkillEntry(s.getId(), SkillLoader.GSON.toJson(s)));
        }
        private SyncSkillRegistryMessage(List<SkillEntry> entries) { this.entries = entries; }
        public static void encode(SyncSkillRegistryMessage msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.entries.size());
            for (SkillEntry e : msg.entries) { buf.writeResourceLocation(e.id()); buf.writeUtf(e.json()); }
        }
        public static SyncSkillRegistryMessage decode(FriendlyByteBuf buf) {
            int count = buf.readInt();
            List<SkillEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) entries.add(new SkillEntry(buf.readResourceLocation(), buf.readUtf()));
            return new SyncSkillRegistryMessage(entries);
        }
        public static void handle(SyncSkillRegistryMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                SkillManager.clearSkills();
                for (SkillEntry e : msg.entries) {
                    try {
                        Skill skill = SkillLoader.GSON.fromJson(e.json(), Skill.class);
                        skill.setId(e.id());
                        SkillManager.addSkill(skill);
                    } catch (Exception ex) { Jd_skill_tree.LOGGER.error("Failed to deserialize skill {}", e.id(), ex); }
                }
                ModSkills.updateBonusMaps();
            });
            ctx.setPacketHandled(true);
        }
    }

    // SERVER→CLIENT: Sync a cooldown
    public static class CooldownSyncMessage {
        private final ResourceLocation skillId;
        private final int ticks;
        public CooldownSyncMessage(ResourceLocation skillId, int ticks) { this.skillId = skillId; this.ticks = ticks; }
        public static void encode(CooldownSyncMessage msg, FriendlyByteBuf buf) { buf.writeResourceLocation(msg.skillId); buf.writeInt(msg.ticks); }
        public static CooldownSyncMessage decode(FriendlyByteBuf buf) { return new CooldownSyncMessage(buf.readResourceLocation(), buf.readInt()); }
        public static void handle(CooldownSyncMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) ((IUnlockedSkillsData) mc.player).setSkillCooldown(msg.skillId, msg.ticks);
            });
            ctx.setPacketHandled(true);
        }
    }
}
