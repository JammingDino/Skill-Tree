package com.jd_skill_tree;

import com.jd_skill_tree.command.SkillCommand;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.skills.SkillLoader;
import com.jd_skill_tree.skills.actions.SkillActionHandler;
import com.jd_skill_tree.utils.ActionScheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = Jd_skill_tree.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventHandlers {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SkillCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(SkillLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SkillNetworking.syncSkillRegistry(serverPlayer);
            SkillNetworking.syncSkillsToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) ActionScheduler.tick(server);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer)) return;
        SkillActionHandler.handleTimerActions(event.player);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getPlayer() != null) {
            SkillActionHandler.triggerActions(
                    event.getPlayer(),
                    com.jd_skill_tree.skills.actions.TriggerType.BLOCK_BREAK,
                    event.getPlayer(),
                    (net.minecraft.world.level.Level) event.getLevel(),
                    event.getPos()
            );
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        var player = event.getEntity();
        var target = event.getTarget();
        SkillActionHandler.triggerActions(
                player,
                com.jd_skill_tree.skills.actions.TriggerType.ATTACK_TARGET,
                target,
                player.level(),
                target.blockPosition()
        );
        SkillActionHandler.triggerActions(
                player,
                com.jd_skill_tree.skills.actions.TriggerType.ATTACK_SELF,
                player,
                player.level(),
                player.blockPosition()
        );
    }
}
