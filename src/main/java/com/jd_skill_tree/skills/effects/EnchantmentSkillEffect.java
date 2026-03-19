package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentSkillEffect implements SkillEffect {

    private final ResourceLocation enchantmentId;
    private final int levelAdded;
    private final EquipmentSlot targetSlot;
    private final boolean allowOverEnchanting;
    private final SkillCondition condition;

    public EnchantmentSkillEffect(ResourceLocation enchantmentId, int levelAdded, EquipmentSlot targetSlot, boolean allowOverEnchanting, SkillCondition condition) {
        this.enchantmentId = enchantmentId;
        this.levelAdded = levelAdded;
        this.targetSlot = targetSlot;
        this.allowOverEnchanting = allowOverEnchanting;
        this.condition = condition;
    }

    @Override
    public SkillCondition getCondition() {
        return this.condition;
    }

    // --- LOGIC HANDLER ---
    public static void updateEnchantments(Player player) {
        if (player.tickCount % 10 != 0) return;

        IUnlockedSkillsData playerData = (IUnlockedSkillsData) player;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            // 1. Clean up previous bonuses first
            cleanStack(stack);

            // 2. CHECK: Ensure the item is valid gear
            if (!isValidGear(stack)) {
                continue;
            }

            // 3. Maps to store calculated data
            Map<Enchantment, Integer> bonusesToApply = new HashMap<>();
            Map<Enchantment, Boolean> allowRuleBreaking = new HashMap<>();

            // 4. Aggregate bonuses
            SkillManager.getAllSkills().stream()
                    .filter(skill -> playerData.hasSkill(skill.getId().toString()))
                    .flatMap(skill -> skill.getEffects().stream())
                    .filter(effect -> effect instanceof EnchantmentSkillEffect)
                    .map(effect -> (EnchantmentSkillEffect) effect)
                    // Check slot match AND the specific Condition for this effect
                    .filter(effect -> effect.targetSlot == slot && effect.isActive(player))
                    .forEach(effect -> {
                        Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(effect.enchantmentId);
                        if (ench != null) {
                            if (!effect.allowOverEnchanting && !ench.canEnchant(stack)) {
                                return;
                            }
                            bonusesToApply.merge(ench, effect.levelAdded, Integer::sum);
                            if (effect.allowOverEnchanting) {
                                allowRuleBreaking.put(ench, true);
                            }
                        }
                    });

            // 5. Apply new bonuses
            if (!bonusesToApply.isEmpty()) {
                applyBonuses(stack, bonusesToApply, allowRuleBreaking);
            }
        }
    }

    private static boolean isValidGear(ItemStack stack) {
        Item item = stack.getItem();
        if (item.getMaxDamage(stack) > 0) return true;
        if (item instanceof TieredItem || item instanceof ArmorItem) return true;
        if (item instanceof ShieldItem) return true;
        if (item instanceof BowItem || item instanceof CrossbowItem) return true;
        if (item instanceof TridentItem) return true;
        if (item instanceof FishingRodItem) return true;
        if (item instanceof ShearsItem) return true;
        if (item instanceof FlintAndSteelItem) return true;
        if (item instanceof ElytraItem) return true;
        return false;
    }

    private static void cleanStack(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains("jd_skill_bonus")) return;
        CompoundTag bonusTag = stack.getTag().getCompound("jd_skill_bonus");
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);

        for (String key : bonusTag.getAllKeys()) {
            ResourceLocation enchId = new ResourceLocation(key);
            Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(enchId);
            if (ench != null && enchantments.containsKey(ench)) {
                int bonusLevel = bonusTag.getInt(key);
                int currentLevel = enchantments.get(ench);
                int originalLevel = Math.max(0, currentLevel - bonusLevel);

                if (originalLevel == 0) {
                    enchantments.remove(ench);
                } else {
                    enchantments.put(ench, originalLevel);
                }
            }
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
        stack.getTag().remove("jd_skill_bonus");
    }

    private static void applyBonuses(ItemStack stack, Map<Enchantment, Integer> bonuses, Map<Enchantment, Boolean> ruleBreakers) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        CompoundTag bonusTag = new CompoundTag();
        boolean changed = false;

        for (Map.Entry<Enchantment, Integer> entry : bonuses.entrySet()) {
            Enchantment ench = entry.getKey();
            int bonus = entry.getValue();
            boolean unlimited = ruleBreakers.getOrDefault(ench, false);

            int currentLevel = enchantments.getOrDefault(ench, 0);
            int newLevel = currentLevel + bonus;

            if (!unlimited) {
                int max = ench.getMaxLevel();
                if (newLevel > max) {
                    newLevel = max;
                    bonus = newLevel - currentLevel;
                }
            }

            if (bonus > 0) {
                enchantments.put(ench, newLevel);
                bonusTag.putInt(ForgeRegistries.ENCHANTMENTS.getKey(ench).toString(), bonus);
                changed = true;
            }
        }

        if (changed) {
            EnchantmentHelper.setEnchantments(enchantments, stack);
            stack.getOrCreateTag().put("jd_skill_bonus", bonusTag);
        }
    }

    public ResourceLocation getEnchantmentId() { return enchantmentId; }
    public int getLevelAdded() { return levelAdded; }
    public EquipmentSlot getTargetSlot() { return targetSlot; }
    public boolean isAllowOverEnchanting() { return allowOverEnchanting; }

    public static EnchantmentSkillEffect fromJson(JsonObject json) {
        ResourceLocation enchId = new ResourceLocation(GsonHelper.getAsString(json, "enchantment"));
        int level = GsonHelper.getAsInt(json, "level_added", 1);
        String slotName = GsonHelper.getAsString(json, "slot", "mainhand").toLowerCase();
        boolean over = GsonHelper.getAsBoolean(json, "over_enchant", false);

        EquipmentSlot slot = switch (slotName) {
            case "offhand" -> EquipmentSlot.OFFHAND;
            case "feet", "boots" -> EquipmentSlot.FEET;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };

        SkillCondition cond = null;
        if (json.has("condition")) {
            cond = SkillConditionType.create(json.getAsJsonObject("condition"));
        }

        return new EnchantmentSkillEffect(enchId, level, slot, over, cond);
    }
}