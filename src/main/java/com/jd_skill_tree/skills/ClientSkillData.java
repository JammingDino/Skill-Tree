package com.jd_skill_tree.skills;

import java.util.HashSet;
import java.util.Set;

public class ClientSkillData {
    private static final Set<String> unlockedSkills = new HashSet<>();

    public static void setUnlockedSkills(Set<String> newUnlockedSkills) {
        unlockedSkills.clear();
        unlockedSkills.addAll(newUnlockedSkills);
    }

    public static boolean isSkillUnlocked(Skill skill) {
        if (skill == null || skill.getId() == null) return false;
        return unlockedSkills.contains(skill.getId().toString());
    }

    public static Set<String> getUnlockedSkills() {
        return unlockedSkills;
    }
}
