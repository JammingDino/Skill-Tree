package com.jd_skill_tree.api;

import com.jd_skill_tree.skills.Skill;

/**
 * An interface to provide a bridge between common code (main) and client-only code (client).
 * The common code can call this, and the client will provide the actual implementation.
 */
@FunctionalInterface // This is a good practice for interfaces with one method
public interface IClientSkillData {
    boolean isSkillUnlocked(Skill skill);
}