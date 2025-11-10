package com.jd_skill_tree.skills;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Skill {
    private final String id;
    private final String title;
    private final String description;
    private final ItemStack icon;
    private final int tier;
    private final int cost;
    private final List<String> requiredSkills; // Changed to List

    // Constructor with multiple required skills
    public Skill(String id, String title, String description, ItemStack icon, int tier, int cost, List<String> requiredSkills) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.tier = tier;
        this.cost = cost;
        this.requiredSkills = requiredSkills != null ? new ArrayList<>(requiredSkills) : new ArrayList<>();
    }

    // Constructor with single required skill (for backward compatibility)
    public Skill(String id, String title, String description, ItemStack icon, int tier, int cost, String requiredSkill) {
        this(id, title, description, icon, tier, cost,
                requiredSkill != null ? List.of(requiredSkill) : new ArrayList<>());
    }

    // Constructor with no required skills
    public Skill(String id, String title, String description, ItemStack icon, int tier, int cost) {
        this(id, title, description, icon, tier, cost, new ArrayList<>());
    }

    public String getId() { return this.id; }
    public String getTitle() { return this.title; }
    public String getDescription() { return this.description; }
    public ItemStack getIcon() { return this.icon; }
    public int getTier() { return this.tier; }
    public int getCost() { return this.cost; }

    /**
     * @deprecated Use getRequiredSkills() instead for multiple prerequisites
     */
    @Deprecated
    public Optional<Skill> getRequiredSkill() {
        if (this.requiredSkills.isEmpty()) {
            return Optional.empty();
        }
        // Return the first required skill for backward compatibility
        return ModSkills.getSkillById(this.requiredSkills.get(0));
    }

    /**
     * Gets all required skills for this skill.
     * @return A list of all prerequisite skills
     */
    public List<Skill> getRequiredSkills() {
        List<Skill> skills = new ArrayList<>();
        for (String skillId : this.requiredSkills) {
            ModSkills.getSkillById(skillId).ifPresent(skills::add);
        }
        return skills;
    }

    /**
     * Gets the IDs of all required skills.
     * @return A list of all prerequisite skill IDs
     */
    public List<String> getRequiredSkillIds() {
        return new ArrayList<>(this.requiredSkills);
    }

    /**
     * Checks if this skill has any prerequisites.
     * @return True if there are required skills
     */
    public boolean hasRequirements() {
        return !this.requiredSkills.isEmpty();
    }
}