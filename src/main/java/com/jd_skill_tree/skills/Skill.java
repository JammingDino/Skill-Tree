package com.jd_skill_tree.skills;

import com.google.gson.annotations.SerializedName;
import com.jd_skill_tree.skills.actions.SkillAction;
import com.jd_skill_tree.skills.effects.SkillEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a skill that has been loaded from a JSON file.
 * This is a Plain Old Java Object (POJO) used by GSON for deserialization.
 */
public class Skill {
    // --- Fields that will be loaded directly from JSON ---
    private String name;
    private String description;
    private Identifier icon;
    private int tier;
    private int cost;
    @SerializedName("prerequisites") // Tells GSON to map the "prerequisites" json key to this field
    private List<Identifier> prerequisiteIds = new ArrayList<>();

    @SerializedName("actions")
    private List<SkillAction> actions = new ArrayList<>();

    // --- Fields that are NOT in the JSON, but are managed by our code ---
    private transient Identifier id; // 'transient' means GSON will ignore this field
    private transient ItemStack iconStackCache; // A cache for the item stack to avoid creating it repeatedly

    // A default constructor is good practice for deserialization
    public Skill() {}

    // --- Getters for accessing the skill's data ---

    public Identifier getId() { return this.id; }
    public String getName() { return this.name; }
    public String getDescription() { return this.description; }
    public int getTier() { return this.tier; }
    public int getCost() { return this.cost; }

    /**
     * Gets the icon for this skill as an ItemStack.
     * The result is cached for performance.
     */
    public ItemStack getIcon() {
        if (this.iconStackCache == null) {
            Optional<Item> item = Registries.ITEM.getOrEmpty(this.icon);
            this.iconStackCache = new ItemStack(item.orElse(net.minecraft.item.Items.BARRIER));
        }
        return this.iconStackCache;
    }

    public List<Identifier> getPrerequisiteIds() {
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
        for (Identifier skillId : this.prerequisiteIds) {
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
    public void setId(Identifier id) {
        this.id = id;
    }
}