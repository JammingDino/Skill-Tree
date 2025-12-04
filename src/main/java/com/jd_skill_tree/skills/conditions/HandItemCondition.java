package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class HandItemCondition implements SkillCondition {

    public enum HandSlot { MAINHAND, OFFHAND }

    private final Item targetItem;
    private final int minCount;
    private final HandSlot slot;

    public HandItemCondition(Item targetItem, int minCount, HandSlot slot) {
        this.targetItem = targetItem;
        this.minCount = minCount;
        this.slot = slot;
    }

    @Override
    public boolean test(PlayerEntity player) {
        ItemStack stack = (this.slot == HandSlot.MAINHAND) ? player.getMainHandStack() : player.getOffHandStack();

        // Check if item matches and count is sufficient
        return stack.isOf(this.targetItem) && stack.getCount() >= this.minCount;
    }

    // Getters for Editor (later)
    public Item getTargetItem() { return targetItem; }
    public int getMinCount() { return minCount; }
    public HandSlot getSlot() { return slot; }

    public static HandItemCondition fromJson(JsonObject json) {
        Identifier itemId = new Identifier(JsonHelper.getString(json, "item"));
        Item item = Registries.ITEM.get(itemId);

        int count = JsonHelper.getInt(json, "count", 1);

        String slotStr = JsonHelper.getString(json, "slot", "mainhand").toUpperCase();
        HandSlot slot = "OFFHAND".equals(slotStr) ? HandSlot.OFFHAND : HandSlot.MAINHAND;

        return new HandItemCondition(item, count, slot);
    }
}