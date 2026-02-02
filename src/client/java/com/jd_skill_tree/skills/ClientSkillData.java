package com.jd_skill_tree.skills;

import net.minecraft.util.Identifier;
import java.util.HashSet;
import java.util.Set;

/**
 * A client-side cache for the player's unlocked skills.
 * This class should be updated by a networking packet from the server.
 */
public class ClientSkillData {
    // A Set is used for fast lookups (O(1) average time complexity)
    private static final Set<String> unlockedSkills = new HashSet<>();

    /**
     * This method should be called when the client receives the skill data from the server.
     * @param newUnlockedSkills The complete set of skill IDs the player has unlocked.
     */
    public static void setUnlockedSkills(Set<String> newUnlockedSkills) {
        unlockedSkills.clear();
        unlockedSkills.addAll(newUnlockedSkills);
    }

    /**
     * Checks if the player has a specific skill unlocked.
     * @param skill The skill to check.
     * @return True if the skill is in the set of unlocked skills.
     */
    public static boolean isSkillUnlocked(Skill skill) {
        if (skill == null || skill.getId() == null) return false;
        // CORRECTED LINE: Convert the skill's Identifier to a string before checking.
        return unlockedSkills.contains(skill.getId().toString());
    }

    public static Set<String> getUnlockedSkills() {
        return unlockedSkills;
    }
}