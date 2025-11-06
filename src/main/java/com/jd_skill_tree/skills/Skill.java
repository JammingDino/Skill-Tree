package com.jd_skill_tree.skills;

import net.minecraft.item.ItemStack;

import java.util.Optional;

public class Skill {
    private final String id;
    private final String title;
    private final String description;
    private final ItemStack icon;
    private final int tier;
    private final int cost;
    private final String requiredSkill;

    public Skill(String id, String title, String description, ItemStack icon, int tier, int cost, String requiredSkill) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.tier = tier;
        this.cost = cost;
        this.requiredSkill = requiredSkill;
    }

    public Skill(String id, String title, String description, ItemStack icon, int tier, int cost) {
        // Calls the main constructor, passing null for the requiredSkill
        this(id, title, description, icon, tier, cost, null);
    }

    public String getId() { return this.id; }
    public String getTitle() { return this.title; }
    public String getDescription() { return this.description; }
    public ItemStack getIcon() { return this.icon; }
    public int getTier() { return this.tier; }
    public int getCost() { return this.cost; }

    public Optional<Skill> getRequiredSkill() {
        if (this.requiredSkill == null) {
            return Optional.empty(); // No prerequisite defined.
        }
        // Use the registry to find the skill object.
        return ModSkills.getSkillById(this.requiredSkill);
    }

}
