package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class EquippedItemCondition implements SkillCondition {

    private final Item targetItem;
    private final EquipmentSlot slot;
    private final NbtCompound nbt;

    public EquippedItemCondition(Item targetItem, EquipmentSlot slot, NbtCompound nbt) {
        this.targetItem = targetItem;
        this.slot = slot;
        this.nbt = nbt;
    }

    @Override
    public boolean test(PlayerEntity player) {
        ItemStack stack = player.getEquippedStack(this.slot);

        if (!stack.isOf(this.targetItem)) return false;

        if (this.nbt != null) {
            if (!stack.hasNbt()) return false;
            return NbtHelper.matches(this.nbt, stack.getNbt(), true);
        }

        return true;
    }

    public Item getTargetItem() { return targetItem; }
    public EquipmentSlot getSlot() { return slot; }
    public NbtCompound getNbt() { return nbt; }

    public static EquippedItemCondition fromJson(JsonObject json) {
        Identifier itemId = new Identifier(JsonHelper.getString(json, "item"));
        Item item = Registries.ITEM.get(itemId);

        String slotStr = JsonHelper.getString(json, "slot", "head").toLowerCase();
        EquipmentSlot slot = switch (slotStr) {
            case "feet", "boots" -> EquipmentSlot.FEET;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            default -> EquipmentSlot.HEAD;
        };

        NbtCompound nbt = null;
        if (json.has("nbt")) {
            try {
                nbt = StringNbtReader.parse(JsonHelper.getString(json, "nbt"));
            } catch (Exception e) {
                // Log error
            }
        }

        return new EquippedItemCondition(item, slot, nbt);
    }
}