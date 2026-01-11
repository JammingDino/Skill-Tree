package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentSkillEffect implements SkillEffect {

    private final Identifier enchantmentId;
    private final int levelAdded;
    private final EquipmentSlot targetSlot;
    private final boolean allowOverEnchanting;
    private final SkillCondition condition;

    public EnchantmentSkillEffect(Identifier enchantmentId, int levelAdded, EquipmentSlot targetSlot, boolean allowOverEnchanting, SkillCondition condition) {
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
    public static void updateEnchantments(PlayerEntity player) {
        if (player.age % 10 != 0) return;

        IUnlockedSkillsData playerData = (IUnlockedSkillsData) player;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
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
                        Enchantment ench = Registries.ENCHANTMENT.get(effect.enchantmentId);
                        if (ench != null) {
                            if (!effect.allowOverEnchanting && !ench.isAcceptableItem(stack)) {
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
        if (item.getMaxDamage() > 0) return true;
        if (item instanceof ToolItem || item instanceof ArmorItem) return true;
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
        if (!stack.hasNbt() || !stack.getNbt().contains("jd_skill_bonus")) return;
        NbtCompound bonusTag = stack.getNbt().getCompound("jd_skill_bonus");
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);

        for (String key : bonusTag.getKeys()) {
            Identifier enchId = new Identifier(key);
            Enchantment ench = Registries.ENCHANTMENT.get(enchId);
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
        EnchantmentHelper.set(enchantments, stack);
        stack.getNbt().remove("jd_skill_bonus");
    }

    private static void applyBonuses(ItemStack stack, Map<Enchantment, Integer> bonuses, Map<Enchantment, Boolean> ruleBreakers) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        NbtCompound bonusTag = new NbtCompound();
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
                bonusTag.putInt(Registries.ENCHANTMENT.getId(ench).toString(), bonus);
                changed = true;
            }
        }

        if (changed) {
            EnchantmentHelper.set(enchantments, stack);
            stack.getOrCreateNbt().put("jd_skill_bonus", bonusTag);
        }
    }

    public Identifier getEnchantmentId() { return enchantmentId; }
    public int getLevelAdded() { return levelAdded; }
    public EquipmentSlot getTargetSlot() { return targetSlot; }
    public boolean isAllowOverEnchanting() { return allowOverEnchanting; }

    public static EnchantmentSkillEffect fromJson(JsonObject json) {
        Identifier enchId = new Identifier(JsonHelper.getString(json, "enchantment"));
        int level = JsonHelper.getInt(json, "level_added", 1);
        String slotName = JsonHelper.getString(json, "slot", "mainhand").toLowerCase();
        boolean over = JsonHelper.getBoolean(json, "over_enchant", false);

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