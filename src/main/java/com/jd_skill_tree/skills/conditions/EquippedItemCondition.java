package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class EquippedItemCondition implements SkillCondition {

    private final Item targetItem;
    private final EquipmentSlot slot;

    public EquippedItemCondition(Item targetItem, EquipmentSlot slot) {
        this.targetItem = targetItem;
        this.slot = slot;
    }

    @Override
    public boolean test(PlayerEntity player) {
        ItemStack stack = player.getEquippedStack(this.slot);
        return stack.isOf(this.targetItem);
    }

    public Item getTargetItem() { return targetItem; }
    public EquipmentSlot getSlot() { return slot; }

    public static EquippedItemCondition fromJson(JsonObject json) {
        Identifier itemId = new Identifier(JsonHelper.getString(json, "item"));
        Item item = Registries.ITEM.get(itemId);

        String slotStr = JsonHelper.getString(json, "slot", "head").toLowerCase();

        EquipmentSlot slot = switch (slotStr) {
            case "feet", "boots" -> EquipmentSlot.FEET;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            default -> EquipmentSlot.HEAD; // Default to helmet if unknown
        };

        return new EquippedItemCondition(item, slot);
    }
}