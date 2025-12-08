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
        // Only trigger if the placement was successful (SUCCESS or CONSUME)
        if (cir.getReturnValue().isAccepted()) {
            if (context.getPlayer() != null && !context.getWorld().isClient) {
                SkillActionHandler.triggerActions(
                        context.getPlayer(),
                        context.getWorld(),
                        context.getBlockPos(), // This gets the pos of the newly placed block
                        TriggerType.BLOCK_PLACE
                );
            }
        }
    }
}