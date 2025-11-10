package com.jd_skill_tree.client;

import com.jd_skill_tree.blocks.SkillAltar;
import com.jd_skill_tree.screens.AltarScreen;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class ClientBlockInteractionHandler {

    public static void register() {
        // Register client-side block use callback
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Only on client side
            if (world.isClient) {
                var blockState = world.getBlockState(hitResult.getBlockPos());

                if (!player.isSneaking()) {
                    // Check if it's a SkillAltar block
                    if (blockState.getBlock() instanceof SkillAltar altar) {
                        // Open the GUI using our helper method
                        openSkillTreeGui(altar.getTier());
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    public static void openSkillTreeGui(int tier) {
        MinecraftClient client = MinecraftClient.getInstance();
        System.out.println("Skill Altar (Tier " + tier + ")");
        MinecraftClient.getInstance().setScreen(
                new AltarScreen(Text.empty(), tier)
        );
    }

    // Helper method to check if player is looking at a SkillAltar
    // (This can be useful if you want to manually trigger GUI opening elsewhere)
    public static boolean tryOpenSkillAltar() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget != null &&
                client.crosshairTarget.getType() == HitResult.Type.BLOCK) {

            BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
            var blockState = client.world.getBlockState(blockHit.getBlockPos());

            if (blockState.getBlock() instanceof SkillAltar altar) {
                openSkillTreeGui(altar.getTier());
                return true;
            }
        }
        return false;
    }
}