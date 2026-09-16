package com.jd_skill_tree.skills;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.skills.actions.SkillActionListAdapter;
import com.jd_skill_tree.skills.conditions.SkillConditionListAdapter;
import com.jd_skill_tree.skills.effects.SkillEffect;
import com.jd_skill_tree.skills.actions.SkillAction;
import com.jd_skill_tree.skills.effects.SkillEffectListAdapter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.21.4: JsonDataLoader became a generic Codec-based loader; the GSON-drive skills
 * format does not fit a Codec, so this uses SinglePreparationResourceReloader with a
 * manual ResourceFinder scan instead. Same behaviour, same directory ("skills").
 */
public class SkillLoader extends SinglePreparationResourceReloader<Map<Identifier, JsonElement>> implements IdentifiableResourceReloadListener {

    private static final ResourceFinder FINDER = ResourceFinder.json("skills");

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
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of(Jd_skill_tree.MOD_ID, "skills");
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, Profiler profiler) {
        Map<Identifier, JsonElement> map = new HashMap<>();
        for (Map.Entry<Identifier, List<net.minecraft.resource.Resource>> entry : FINDER.findAllResources(manager).entrySet()) {
            for (net.minecraft.resource.Resource resource : entry.getValue()) {
                try (Reader reader = resource.getReader()) {
                    map.put(FINDER.toResourceId(entry.getKey()), JsonParser.parseReader(reader));
                } catch (Exception e) {
                    Jd_skill_tree.LOGGER.error("Failed to read skill json: {}", entry.getKey(), e);
                }
            }
        }
        return map;
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
