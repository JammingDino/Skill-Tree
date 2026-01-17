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

                // Nested effect parsing
                SkillActionEffect effect = SkillActionEffectType.create(obj.getAsJsonObject("effect"));

                // Parse Condition
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

            // --- FIXED: Use helper to write Effect (Recursion Safe) ---
            obj.add("effect", serializeEffect(action.getEffect(), context));

            // Write Condition
            if (action.getCondition() != null) {
                obj.add("condition", SkillConditionListAdapter.serializeCondition(action.getCondition(), context));
            }

            array.add(obj);
        }
        return array;
    }

    // --- HELPER: Handles all effect types including new ones ---
    private static JsonObject serializeEffect(SkillActionEffect effect, JsonSerializationContext context) {
        JsonObject json = new JsonObject();

        if (effect instanceof CommandActionEffect cmd) {
            json.addProperty("type", "jd_skill_tree:command");
            json.addProperty("command", cmd.getCommand());
        }
        else if (effect instanceof BurnActionEffect burn) {
            json.addProperty("type", "jd_skill_tree:burn");
            json.addProperty("duration", burn.getDuration());
            json.addProperty("ignore_armor", burn.isIgnoreArmor());
        }
        else if (effect instanceof HealActionEffect heal) {
            json.addProperty("type", "jd_skill_tree:heal");
            json.addProperty("amount", heal.getAmount());
            json.addProperty("is_hunger", heal.isHunger());
        }
        else if (effect instanceof LaunchActionEffect launch) {
            json.addProperty("type", "jd_skill_tree:launch");
            json.addProperty("strength", launch.getStrength());
            json.addProperty("vertical", launch.getVertical());
        }
        else if (effect instanceof DelayedActionEffect delayed) {
            json.addProperty("type", "jd_skill_tree:delayed");
            json.addProperty("delay", delayed.getDelay());

            // RECURSION: Serialize the inner effect using this same helper
            json.add("effect", serializeEffect(delayed.getNextEffect(), context));

            if (delayed.getNextCondition() != null) {
                json.add("condition", SkillConditionListAdapter.serializeCondition(delayed.getNextCondition(), context));
            }
        } else if (effect instanceof RaycastActionEffect ray) {
            json.addProperty("type", "jd_skill_tree:raycast");
            json.addProperty("length", ray.getLength());
            json.addProperty("fluids", ray.isStopOnFluids());
            json.addProperty("entities", ray.isHitEntities());

            // Recursive serialization
            json.add("effect", serializeEffect(ray.getChildEffect(), context));
        }

        return json;
    }
}