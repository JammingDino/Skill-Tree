package com.jd_skill_tree.skills.actions;

import com.google.gson.*;
import com.jd_skill_tree.skills.conditions.SkillCondition;
import com.jd_skill_tree.skills.conditions.SkillConditionListAdapter;
import com.jd_skill_tree.skills.conditions.SkillConditionType;
import net.minecraft.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SkillActionListAdapter implements JsonDeserializer<List<SkillAction>>, JsonSerializer<List<SkillAction>> {

    @Override
    public List<SkillAction> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        List<SkillAction> actions = new ArrayList<>();
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                JsonObject obj = element.getAsJsonObject();
                TriggerType trigger = TriggerType.valueOf(JsonHelper.getString(obj, "trigger").toUpperCase());
                int interval = JsonHelper.getInt(obj, "interval", 20);

                SkillActionEffect effect = SkillActionEffectType.create(obj.getAsJsonObject("effect"));

                SkillCondition condition = null;
                if (obj.has("condition")) {
                    condition = SkillConditionType.create(obj.getAsJsonObject("condition"));
                }

                actions.add(new SkillAction(trigger, effect, interval, condition));
            }
        }
        return actions;
    }

    @Override
    public JsonElement serialize(List<SkillAction> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        for (SkillAction action : src) {
            JsonObject obj = new JsonObject();
            obj.addProperty("trigger", action.getTrigger().name());
            obj.addProperty("interval", action.getInterval());

            JsonObject effectObj = new JsonObject();
            SkillActionEffect effect = action.getEffect();
            if (effect instanceof CommandActionEffect cmd) {
                effectObj.addProperty("type", "jd_skill_tree:command");
                effectObj.addProperty("command", cmd.getCommand());
            } else if (effect instanceof BurnActionEffect burn) {
                effectObj.addProperty("type", "jd_skill_tree:burn");
                effectObj.addProperty("duration", burn.getDuration());
                effectObj.addProperty("ignore_armor", burn.isIgnoreArmor());
            }
            obj.add("effect", effectObj);

            // --- FIXED: Use static helper ---
            if (action.getCondition() != null) {
                obj.add("condition", SkillConditionListAdapter.serializeCondition(action.getCondition(), context));
            }
            // --------------------------------

            array.add(obj);
        }
        return array;
    }
}