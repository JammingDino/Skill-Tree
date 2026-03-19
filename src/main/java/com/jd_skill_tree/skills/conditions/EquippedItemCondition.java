package com.jd_skill_tree.skills.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class EquippedItemCondition implements SkillCondition {

    private final Item targetItem;
    private final EquipmentSlot slot;
    private final CompoundTag nbt;

    public EquippedItemCondition(Item targetItem, EquipmentSlot slot, CompoundTag nbt) {
        this.targetItem = targetItem;
        this.slot = slot;
        this.nbt = nbt;
    }

    @Override
    public boolean test(Player player) {
        ItemStack stack = player.getItemBySlot(this.slot);

        if (!stack.isOf(this.targetItem)) return false;

        if (this.nbt != null) {
            if (!stack.hasTag()) return false;
            return NbtUtils.matches(this.nbt, stack.getTag(), true);
        }

        return true;
    }

    public Item getTargetItem() { return targetItem; }
    public EquipmentSlot getSlot() { return slot; }
    public CompoundTag getNbt() { return nbt; }

    public static EquippedItemCondition fromJson(JsonObject json) {
        ResourceLocation itemId = new ResourceLocation(GsonHelper.getString(json, "item"));
        Item item = ForgeRegistries.ITEMS.getValue(itemId);

        String slotStr = GsonHelper.getString(json, "slot", "head").toLowerCase();
        EquipmentSlot slot = switch (slotStr) {
            case "feet", "boots" -> EquipmentSlot.FEET;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            default -> EquipmentSlot.HEAD;
        };

        CompoundTag nbt = null;
        if (json.has("nbt")) {
            try {
                nbt = TagParser.parseTag(GsonHelper.getString(json, "nbt"));
            } catch (Exception e) {
                // Log error
            }
        }

        return new EquippedItemCondition(item, slot, nbt);
    }
}