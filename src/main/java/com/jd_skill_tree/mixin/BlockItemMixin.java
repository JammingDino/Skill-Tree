package com.jd_skill_tree.mixin;

import com.jd_skill_tree.skills.actions.SkillActionHandler;
import com.jd_skill_tree.skills.actions.TriggerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("RETURN"))
    private void onPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue().isAccepted()) {
            if (context.getPlayer() != null && !context.getWorld().isClient) {
                // FIXED: Now passes 5 arguments: owner, trigger, target, world, pos
                SkillActionHandler.triggerActions(
                        context.getPlayer(),
                        TriggerType.BLOCK_PLACE,
                        context.getPlayer(), // Target is the player placing the block
                        context.getWorld(),
                        context.getBlockPos()
                );
            }
        }
    }
}