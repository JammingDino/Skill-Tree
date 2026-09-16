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

        // 3. Check item data (if specified)
        // 1.20.5+: item NBT became data components. Extra custom data lives in the
        // custom_data component now; a plain component map check would need codec work,
        // so we compare against custom_data only — vanilla-strict component predicates
        // are NOT supported in this legacy-style condition (reported to Levi).
        if (this.nbt != null) {
            net.minecraft.component.type.NbtComponent custom = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
            if (custom == null) return false;
            return NbtHelper.matches(this.nbt, custom.copyNbt(), true);
        }

        return true;
    }

    public Item getTargetItem() { return targetItem; }
    public int getMinCount() { return minCount; }
    public HandSlot getSlot() { return slot; }
    public NbtCompound getNbt() { return nbt; }

    public static HandItemCondition fromJson(JsonObject json) {
        Identifier itemId = Identifier.of(JsonHelper.getString(json, "item"));
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