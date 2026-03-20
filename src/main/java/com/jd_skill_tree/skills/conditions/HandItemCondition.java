package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class HandItemCondition implements SkillCondition {

    public enum HandSlot { MAINHAND, OFFHAND }

    private final Item targetItem;
    private final int minCount;
    private final HandSlot slot;
    private final CompoundTag nbt; // The required NBT data

    public HandItemCondition(Item targetItem, int minCount, HandSlot slot, CompoundTag nbt) {
        this.targetItem = targetItem;
        this.minCount = minCount;
        this.slot = slot;
        this.nbt = nbt;
    }

    @Override
    public boolean test(Player player) {
        ItemStack stack = (this.slot == HandSlot.MAINHAND) ? player.getMainHandItem() : player.getOffhandItem();

        // 1. Check Item
        if (!stack.isOf(this.targetItem)) return false;

        // 2. Check Count
        if (stack.getCount() < this.minCount) return false;

        // 3. Check NBT (if specified)
        if (this.nbt != null) {
            if (!stack.hasTag()) return false;
            // 'true' means strict matching for list order? actually in NbtUtils.matches:
            // The boolean is "ignoreExtra" for lists? No, NbtUtils.matches(required, current, ignoreExtra)
            // usually you want to ensure the item has the required tags.
            return NbtUtils.matches(this.nbt, stack.getTag(), true);
        }

        return true;
    }

    public Item getTargetItem() { return targetItem; }
    public int getMinCount() { return minCount; }
    public HandSlot getSlot() { return slot; }
    public CompoundTag getNbt() { return nbt; }

    public static HandItemCondition fromJson(JsonObject json) {
        ResourceLocation itemId = new ResourceLocation(GsonHelper.getString(json, "item"));
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        int count = GsonHelper.getInt(json, "count", 1);
        String slotStr = GsonHelper.getString(json, "slot", "mainhand").toUpperCase();
        HandSlot slot = "OFFHAND".equals(slotStr) ? HandSlot.OFFHAND : HandSlot.MAINHAND;

        // Load NBT string
        CompoundTag nbt = null;
        if (json.has("nbt")) {
            try {
                nbt = TagParser.parseTag(GsonHelper.getString(json, "nbt"));
            } catch (Exception e) {
                // Log error
            }
        }

        return new HandItemCondition(item, count, slot, nbt);
    }
}