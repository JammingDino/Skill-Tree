package com.jd_skill_tree.skills;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.skills.actions.SkillAction;
import com.jd_skill_tree.skills.actions.SkillActionListAdapter;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionListAdapter;
import com.jd_skill_tree.skills.effects.SkillEffect;
import com.jd_skill_tree.skills.effects.SkillEffectListAdapter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class SkillLoader extends SimpleJsonResourceReloadListener {

    private static final Type SKILL_EFFECT_LIST_TYPE = new TypeToken<List<SkillEffect>>() {}.getType();
    private static final Type SKILL_ACTION_LIST_TYPE = new TypeToken<List<SkillAction>>() {}.getType();
    private static final Type SKILL_CONDITION_LIST_TYPE = new TypeToken<List<SkillCondition>>() {}.getType();

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ResourceLocation.class, new IdentifierAdapter())
            .registerTypeAdapter(SKILL_EFFECT_LIST_TYPE, new SkillEffectListAdapter())
            .registerTypeAdapter(SKILL_ACTION_LIST_TYPE, new SkillActionListAdapter())
            .registerTypeAdapter(SKILL_CONDITION_LIST_TYPE, new SkillConditionListAdapter())
            .create();

    public static final SkillLoader INSTANCE = new SkillLoader();

    public SkillLoader() {
        super(GSON, "skills");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        Jd_skill_tree.LOGGER.info("Loading JD Skill Tree skills...");
        SkillManager.clearSkills();
        prepared.forEach((id, jsonElement) -> {
            try {
                Skill skill = GSON.fromJson(jsonElement, Skill.class);
                skill.setId(id);
                SkillManager.addSkill(skill);
            } catch (Exception e) {
                Jd_skill_tree.LOGGER.error("Failed to parse skill: {}", id, e);
            }
        });
        Jd_skill_tree.LOGGER.info("Finished loading {} skills.", SkillManager.getAllSkills().size());
        ModSkills.updateBonusMaps();
    }
}
