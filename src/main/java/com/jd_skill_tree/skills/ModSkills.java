package com.jd_skill_tree.skills;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.*;

public class ModSkills {
    private static final Map<String, Skill> SKILLS = new HashMap<String, Skill>();
    private static final List<Skill> SKILL_TREE = new ArrayList<Skill>();

    // Effects of the different skills
    private static final Map<String, Float> MINING_SPEED_BONUSES = new HashMap<>();

    public static void registerSkills() {
        // Register Skills

        registerSkill(new Skill("reset_skills","Reset Skills","Revokes all unlocked skills and refunds all spent experience levels.",new ItemStack(Items.BARRIER), 1, 0));

        registerSkill(new Skill("mining_speed_1", "Mining Speed I", "Increases mining speed by 20%", new ItemStack(Items.WOODEN_PICKAXE), 1, 5));
        registerSkill(new Skill("mining_speed_2", "Mining Speed II", "Increases mining speed by 20%", new ItemStack(Items.STONE_PICKAXE), 2, 5, "mining_speed_1"));
        registerSkill(new Skill("mining_speed_3", "Mining Speed III", "Increases mining speed by 20%", new ItemStack(Items.IRON_PICKAXE), 3, 5, "mining_speed_2"));
        registerSkill(new Skill("mining_speed_4", "Mining Speed IV", "Increases mining speed by 20%", new ItemStack(Items.GOLDEN_PICKAXE), 4, 5, "mining_speed_3"));
        registerSkill(new Skill("mining_speed_5", "Mining Speed V", "Increases mining speed by 20%", new ItemStack(Items.DIAMOND_PICKAXE), 5, 50, "mining_speed_4"));

        // Register Skill Effects
        MINING_SPEED_BONUSES.put("mining_speed_1", 0.50F);
        MINING_SPEED_BONUSES.put("mining_speed_2", 0.50f);
        MINING_SPEED_BONUSES.put("mining_speed_3", 0.50f);
        MINING_SPEED_BONUSES.put("mining_speed_4", 0.50f);
        MINING_SPEED_BONUSES.put("mining_speed_5", 0.50f);
    }

    private static void registerSkill(Skill skill) {
        SKILLS.put(skill.getId(), skill);
        SKILL_TREE.add(skill);
    }

    public static Skill getSkill(String id) {
        return SKILLS.get(id);
    }

    public static List<Skill> getSkillsForTier(int tier) {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : SKILL_TREE) {
            if (skill.getTier() <= tier) {
                skills.add(skill);
            }
        }
        return skills;
    }

    public static Optional<Skill> getSkillById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(SKILLS.get(id));
    }

    public static List<Skill> getAllSkills() {
        return new ArrayList<>(SKILL_TREE);
    }

    public static Map<String, Float> getMiningSpeedBonuses() {
        return MINING_SPEED_BONUSES;
    }
}
