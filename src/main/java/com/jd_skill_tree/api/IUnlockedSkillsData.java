package com.jd_skill_tree.api;

import java.util.Set;

public interface IUnlockedSkillsData {
    /**
     * @return A copy of the set of unlocked skill IDs.
     */
    Set<String> getUnlockedSkills();

    /**
     * Unlocks a new skill for the player.
     * @param skillId The ID of the skill to unlock.
     */
    void unlockSkill(String skillId);

    /**
     * Checks if the player has a specific skill unlocked.
     * @param skillId The ID of the skill to check.
     * @return True if the skill has been unlocked.
     */
    boolean hasSkill(String skillId);

    /**
     * Used for syncing data from the server or loading from NBT.
     * @param skills The complete set of skills to assign to the player.
     */
    void setUnlockedSkills(Set<String> skills);

    /**
     * Revokes a specific skill from the player.
     * @param skillId The ID of the skill to revoke.
     */
    void revokeSkill(String skillId);
}