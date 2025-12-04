package com.jd_skill_tree.skills;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.skills.actions.SkillActionListAdapter;
import com.jd_skill_tree.skills.conditions.SkillConditionListAdapter;
import com.jd_skill_tree.skills.effects.SkillEffect;
import com.jd_skill_tree.skills.actions.SkillAction;
import com.jd_skill_tree.skills.effects.SkillEffectListAdapter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class SkillLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {

    private static final Type SKILL_EFFECT_LIST_TYPE = new TypeToken<List<SkillEffect>>() {}.getType();
    private static final Type SKILL_ACTION_LIST_TYPE = new TypeToken<List<SkillAction>>() {}.getType();
    private static final Type SKILL_CONDITION_LIST_TYPE = new TypeToken<List<com.jd_skill_tree.skills.conditions.SkillCondition>>() {}.getType();

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
            .registerTypeAdapter(SKILL_EFFECT_LIST_TYPE, new SkillEffectListAdapter())
            .registerTypeAdapter(SKILL_ACTION_LIST_TYPE, new SkillActionListAdapter())
            .registerTypeAdapter(SKILL_CONDITION_LIST_TYPE, new SkillConditionListAdapter())
            .create();
    public static final SkillLoader INSTANCE = new SkillLoader();

    public SkillLoader() {
        super(GSON, "skills");
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier(Jd_skill_tree.MOD_ID, "skills");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        Jd_skill_tree.LOGGER.info("Loading JD Skill Tree skills...");

        // 1. Clear all old skills to prepare for reload
        SkillManager.clearSkills();

        prepared.forEach((id, jsonElement) -> {
            try {
                // 2. Use GSON to automatically convert the JsonElement into our Skill object
                Skill skill = GSON.fromJson(jsonElement, Skill.class);

                // 3. The ID is from the filename, so we set it on the object after parsing
                skill.setId(id);

                // 4. Add the successfully parsed skill to our manager
                SkillManager.addSkill(skill);

            } catch (Exception e) {
                // This is crucial for helping datapack makers!
                Jd_skill_tree.LOGGER.error("Failed to parse skill: {}", id, e);
            }
        });

        Jd_skill_tree.LOGGER.info("Finished loading {} skills.", SkillManager.getAllSkills().size());

        ModSkills.updateBonusMaps();
    }
}