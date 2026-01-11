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
            } else if (effect instanceof DelayedActionEffect delayed) {
                effectObj.addProperty("type", "jd_skill_tree:delayed");
                effectObj.addProperty("delay", delayed.getDelay());

                // Recursively serialize the inner effect
                // We create a temporary list wrapper to reuse this adapter's logic for the inner action?
                // No, we need to serialize the EFFECT specifically.
                // We can't reuse SkillActionListAdapter because that serializes an Action (Trigger+Effect),
                // we only want the Effect part here.

                // Manual serialization of the inner effect:
                JsonObject innerEffectObj = new JsonObject();
                SkillActionEffect inner = delayed.getNextEffect();

                if (inner instanceof CommandActionEffect c) {
                    innerEffectObj.addProperty("type", "jd_skill_tree:command");
                    innerEffectObj.addProperty("command", c.getCommand());
                } else if (inner instanceof BurnActionEffect b) {
                    innerEffectObj.addProperty("type", "jd_skill_tree:burn");
                    innerEffectObj.addProperty("duration", b.getDuration());
                    innerEffectObj.addProperty("ignore_armor", b.isIgnoreArmor());
                }
                effectObj.add("effect", innerEffectObj);

                // Serialize the inner condition
                if (delayed.getNextCondition() != null) {
                    effectObj.add("condition",
                            com.jd_skill_tree.skills.conditions.SkillConditionListAdapter
                                    .serializeCondition(delayed.getNextCondition(), context));
                }
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