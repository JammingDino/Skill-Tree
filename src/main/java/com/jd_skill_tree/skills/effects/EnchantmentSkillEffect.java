package com.jd_skill_tree.skills.effects;

import com.google.gson.JsonObject;
import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    // --- HELPERS (1.20.5+ data-component port) ---

    // --- HELPERS (1.20.5+ data-component port) ---

    private static RegistryEntry<Enchantment> lookupEnchantment(net.minecraft.world.World world, Identifier id) {
        // 1.21: enchantments are data-driven; access them via the world's dynamic registry
        // manager instead of the (removed) static Registries.ENCHANTMENT.
        return world.getRegistryManager()
                .getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                .getEntry(id)
                .orElse(null);
    }

    private static Identifier enchantmentId(RegistryEntry<Enchantment> entry) {
        return entry.getKey().isPresent()
                ? entry.getKey().get().getValue()
                : null;
    }

    /**
     * Appends a bonus record ("jd_skill_bonus") to the stack's custom_data component,
     * replacing the 1.20 root-tag trick (which cannot exist under 1.20.5+ components).
     */
    private static NbtCompound getBonusTag(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return new NbtCompound();
        NbtCompound nbt = custom.copyNbt();
        return nbt.getCompound("jd_skill_bonus");
    }

    private static void putBonusTag(ItemStack stack, NbtCompound bonusTag) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = custom != null ? custom.copyNbt() : new NbtCompound();
        nbt.put("jd_skill_bonus", bonusTag);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private static void removeBonusTag(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return;
        NbtCompound nbt = custom.copyNbt();
        if (!nbt.contains("jd_skill_bonus")) return;
        nbt.remove("jd_skill_bonus");
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    // --- LOGIC HANDLER ---
    public static void updateEnchantments(PlayerEntity player) {
        if (player.age % 10 != 0) return;

        IUnlockedSkillsData playerData = (IUnlockedSkillsData) player;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            // 1. Clean up previous bonuses first
            cleanStack(player, stack);

            // 2. CHECK: Ensure the item is valid gear
            if (!isValidGear(stack)) {
                continue;
            }

            // 3. Maps to store calculated data
            Map<RegistryEntry<Enchantment>, Integer> bonusesToApply = new HashMap<>();
            Map<RegistryEntry<Enchantment>, Boolean> allowRuleBreaking = new HashMap<>();

            // 4. Aggregate bonuses
            SkillManager.getAllSkills().stream()
                    .filter(skill -> playerData.hasSkill(skill.getId().toString()))
                    .flatMap(skill -> skill.getEffects().stream())
                    .filter(effect -> effect instanceof EnchantmentSkillEffect)
                    .map(effect -> (EnchantmentSkillEffect) effect)
                    // Check slot match AND the specific Condition for this effect
                    .filter(effect -> effect.targetSlot == slot && effect.isActive(player))
                    .forEach(effect -> {
                        RegistryEntry<Enchantment> ench = lookupEnchantment(player.getWorld(), effect.enchantmentId);
                        if (ench != null) {
                            if (!effect.allowOverEnchanting && !ench.value().isAcceptableItem(stack)) {
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
        if (stack.getMaxDamage() > 0) return true;
        if (item instanceof MiningToolItem || item instanceof ArmorItem) return true;
        if (item instanceof ShieldItem) return true;
        if (item instanceof BowItem || item instanceof CrossbowItem) return true;
        if (item instanceof TridentItem) return true;
        if (item instanceof FishingRodItem) return true;
        if (item instanceof ShearsItem) return true;
        if (item instanceof FlintAndSteelItem) return true;
        // 1.21.2+: ElytraItem was removed; equippable-with-elytra items carry an EQUIPPABLE component.
        if (stack.contains(DataComponentTypes.EQUIPPABLE) || item instanceof MaceItem) return true;
        return false;
    }

    private static void cleanStack(net.minecraft.entity.player.PlayerEntity player, ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return;
        NbtCompound bonusTag = custom.copyNbt().getCompound("jd_skill_bonus");
        if (bonusTag.getSize() == 0) return;

        ItemEnchantmentsComponent current = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(current);

        for (String key : bonusTag.getKeys()) {
            Identifier enchId = Identifier.of(key);
            RegistryEntry<Enchantment> ench = lookupEnchantment(player.getWorld(), enchId);
            if (ench != null) {
                int bonusLevel = bonusTag.getInt(key);
                int at = current.getLevel(ench);
                int originalLevel = Math.max(0, at - bonusLevel);

                if (originalLevel == 0) {
                    builder.set(ench, 0);
                    builder.remove(e -> e.matches(ench));
                } else {
                    builder.set(ench, originalLevel);
                }
            }
        }
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        removeBonusTag(stack);
    }

    private static void applyBonuses(ItemStack stack, Map<RegistryEntry<Enchantment>, Integer> bonuses, Map<RegistryEntry<Enchantment>, Boolean> ruleBreakers) {
        ItemEnchantmentsComponent current = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(current);
        NbtCompound bonusTag = new NbtCompound();
        boolean changed = false;

        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : bonuses.entrySet()) {
            RegistryEntry<Enchantment> ench = entry.getKey();
            int bonus = entry.getValue();
            boolean unlimited = ruleBreakers.getOrDefault(ench, false);

            int currentLevel = current.getLevel(ench);
            int newLevel = currentLevel + bonus;

            if (!unlimited) {
                int max = ench.value().getMaxLevel();
                if (newLevel > max) {
                    newLevel = max;
                    bonus = newLevel - currentLevel;
                }
            }

            if (bonus > 0) {
                builder.set(ench, newLevel);
                Identifier id = enchantmentId(ench);
                if (id != null) {
                    bonusTag.putInt(id.toString(), bonus);
                    changed = true;
                }
            }
        }

        if (changed) {
            stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
            putBonusTag(stack, bonusTag);
        }
    }

    public Identifier getEnchantmentId() { return enchantmentId; }
    public int getLevelAdded() { return levelAdded; }
    public EquipmentSlot getTargetSlot() { return targetSlot; }
    public boolean isAllowOverEnchanting() { return allowOverEnchanting; }

    public static EnchantmentSkillEffect fromJson(JsonObject json) {
        Identifier enchId = Identifier.of(JsonHelper.getString(json, "enchantment"));
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
