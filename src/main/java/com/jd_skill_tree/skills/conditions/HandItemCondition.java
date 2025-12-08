package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class HandItemCondition implements SkillCondition {

    public enum HandSlot { MAINHAND, OFFHAND }

    private final Item targetItem;
    private final int minCount;
    private final HandSlot slot;
    private final NbtCompound nbt; // The required NBT data

    public HandItemCondition(Item targetItem, int minCount, HandSlot slot, NbtCompound nbt) {
        this.targetItem = targetItem;
        this.minCount = minCount;
        this.slot = slot;
        this.nbt = nbt;
    }

    @Override
    public boolean test(PlayerEntity player) {
        ItemStack stack = (this.slot == HandSlot.MAINHAND) ? player.getMainHandStack() : player.getOffHandStack();

        // 1. Check Item
        if (!stack.isOf(this.targetItem)) return false;

        // 2. Check Count
        if (stack.getCount() < this.minCount) return false;

        // 3. Check NBT (if specified)
        if (this.nbt != null) {
            if (!stack.hasNbt()) return false;
            // 'true' means strict matching for list order? actually in NbtHelper.matches:
            // The boolean is "ignoreExtra" for lists? No, NbtHelper.matches(required, current, ignoreExtra)
            // usually you want to ensure the item has the required tags.
            return NbtHelper.matches(this.nbt, stack.getNbt(), true);
        }

        return true;
    }

    public Item getTargetItem() { return targetItem; }
    public int getMinCount() { return minCount; }
    public HandSlot getSlot() { return slot; }
    public NbtCompound getNbt() { return nbt; }

    public static HandItemCondition fromJson(JsonObject json) {
        Identifier itemId = new Identifier(JsonHelper.getString(json, "item"));
        Item item = Registries.ITEM.get(itemId);
        int count = JsonHelper.getInt(json, "count", 1);
        String slotStr = JsonHelper.getString(json, "slot", "mainhand").toUpperCase();
        HandSlot slot = "OFFHAND".equals(slotStr) ? HandSlot.OFFHAND : HandSlot.MAINHAND;

        // Load NBT string
        NbtCompound nbt = null;
        if (json.has("nbt")) {
            try {
                nbt = StringNbtReader.parse(JsonHelper.getString(json, "nbt"));
            } catch (Exception e) {
                // Log error
            }
        }

        return new HandItemCondition(item, count, slot, nbt);
    }
}