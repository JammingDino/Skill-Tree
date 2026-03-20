package com.jd_skill_tree.skills;

import com.google.gson.annotations.SerializedName;
import com.jd_skill_tree.skills.actions.SkillAction;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.effects.SkillEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a skill that has been loaded from a JSON file.
 * This is a Plain Old Java Object (POJO) used by GSON for deserialization.
 */
public class Skill {
    // --- Fields that will be loaded directly from JSON ---
    private String name;
    private String description;
    private ResourceLocation icon;
    @SerializedName("icon_nbt") // Matches JSON key "icon_nbt"
    private String iconNbt;     // Stores the raw NBT string (e.g. "{Enchantments:[...]}")

    private int tier;
    private int cost;
    @SerializedName("prerequisites") // Tells GSON to map the "prerequisites" json key to this field
    private List<ResourceLocation> prerequisiteIds = new ArrayList<>();

    @SerializedName("actions")
    private List<SkillAction> actions = new ArrayList<>();

    @SerializedName("cooldown")
    private int cooldown = 0; // In Ticks (20 = 1 second)

    // --- Fields that are NOT in the JSON, but are managed by our code ---
    private transient ResourceLocation id; // 'transient' means GSON will ignore this field
    private transient ItemStack iconStackCache; // A cache for the item stack to avoid creating it repeatedly

    // A default constructor is good practice for deserialization
    public Skill() {}

    // --- Getters for accessing the skill's data ---

    public ResourceLocation getId() { return this.id; }
    public String getName() { return this.name; }
    public String getDescription() { return this.description; }
    public int getTier() { return this.tier; }
    public int getCost() { return this.cost; }
    public int getCooldown() { return cooldown; }

    /**
     * Gets the icon for this skill as an ItemStack.
     * The result is cached for performance.
     */
    public ItemStack getIcon() {
        if (this.iconStackCache == null) {
            Item item = ForgeRegistries.ITEMS.getValue(this.icon);
            this.iconStackCache = new ItemStack(item != null ? item : net.minecraft.world.item.Items.BARRIER);

            // Apply NBT if present
            if (this.iconNbt != null && !this.iconNbt.isEmpty()) {
                try {
                    CompoundTag tag = TagParser.parseTag(this.iconNbt);
                    this.iconStackCache.setTag(tag);
                } catch (Exception e) {
                    System.err.println("Failed to parse NBT for skill icon: " + this.iconNbt);
                }
            }
        }
        return this.iconStackCache;
    }

    public String getIconNbt() { return iconNbt; }

    public List<ResourceLocation> getPrerequisiteIds() {
        return this.prerequisiteIds;
    }

    // --- Methods used by our loader ---

    /**
     * Helper method to resolve prerequisite IDs into actual Skill objects.
     * This is used by the GUI to easily find parent skills.
     * @return A list of all prerequisite skills
     */
    public List<Skill> getRequiredSkills() {
        List<Skill> skills = new ArrayList<>();
        for (ResourceLocation skillId : this.prerequisiteIds) {
            // We use the new SkillManager to look up the skill by its ID
            SkillManager.getSkill(skillId).ifPresent(skills::add);
        }
        return skills;
    }

    private List<SkillEffect> effects = new ArrayList<>(); // <-- ADD THIS

    public List<SkillEffect> getEffects() { // <-- ADD THIS GETTER
        return this.effects;
    }
    public List<SkillAction> getActions() { return this.actions; }

    /**
     * This is called by our SkillLoader after a skill is created from JSON.
     * It sets the ID based on the file's path and namespace.
     */
    public void setId(ResourceLocation id) {
        this.id = id;
    }
}