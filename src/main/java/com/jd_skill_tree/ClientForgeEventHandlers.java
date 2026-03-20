package com.jd_skill_tree;

import com.jd_skill_tree.blocks.SkillAltar;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.screens.AltarScreen;
import com.jd_skill_tree.screens.DeveloperEditorScreen;
import com.jd_skill_tree.screens.RadialSkillScreen;
import com.jd_skill_tree.skills.ClientSkillData;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.actions.TriggerType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Jd_skill_tree.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEventHandlers {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        while (ClientEventHandlers.ACTIVATE_KEY.consumeClick()) {
            openRadialMenu(mc);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getEntity().isShiftKeyDown()) return;
        var blockState = event.getLevel().getBlockState(event.getPos());
        if (blockState.getBlock() instanceof SkillAltar altar) {
            Minecraft mc = Minecraft.getInstance();
            if (altar.getTier() == 99) {
                mc.execute(() -> mc.setScreen(new DeveloperEditorScreen()));
            } else {
                mc.execute(() -> mc.setScreen(new AltarScreen(Component.empty(), altar.getTier())));
            }
            event.setCanceled(true);
        }
    }

    private static void openRadialMenu(Minecraft mc) {
        List<Skill> activeSkills = new ArrayList<>();
        ClientSkillData.getUnlockedSkills().forEach(id ->
                SkillManager.getSkill(new ResourceLocation(id)).ifPresent(skill -> {
                    boolean hasActive = skill.getActions().stream()
                            .anyMatch(a -> a.getTrigger() == TriggerType.ACTIVATED);
                    if (hasActive) activeSkills.add(skill);
                }));
        if (!activeSkills.isEmpty()) {
            mc.setScreen(new RadialSkillScreen(activeSkills, ClientEventHandlers.ACTIVATE_KEY));
        } else if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§cNo active skills unlocked!"), true);
        }
    }
}
