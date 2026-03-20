package com.jd_skill_tree.screens;

import com.google.gson.*;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class DeveloperEditorScreen extends Screen {

    // --- Data Classes ---
    private static class EffectData {
        String type = "Attribute";
        String attr = "minecraft:generic.max_health";
        String op = "ADDITION";
        String val = "1.0";
        String effectId = "minecraft:speed";
        String amplifier = "0";
        boolean hideParticles = false;
        String enchId = "minecraft:sharpness";
        String enchLevel = "1";
        String enchSlot = "mainhand";
        boolean overEnchant = false;
        String knockbackValue = "0.5";
        String xpValue = "0.5";
        String swimValue = "0.5";
        String lavaValue = "0.5";
        String immuneEffectId = "minecraft:poison";
        ConditionData condition = null;
    }

    private static class EffectConfig {
        String type = "Command";
        String command = "/say Hi";
        String duration = "100";
        boolean bool1 = false;
        boolean bool2 = false;
        String value1 = "1.0";
        String value2 = "0.5";
        EffectConfig childEffect = null;
        ConditionData childCondition = null;
    }

    private static class ActionData {
        String trigger = "BLOCK_BREAK";
        String interval = "20";
        EffectConfig rootEffect = new EffectConfig();
        ConditionData condition = null;
    }

    private static class ConditionData {
        String type = "Hand Item";
        List<ConditionData> children = new ArrayList<>();
        ConditionData child = null;
        String item = "minecraft:stick";
        String count = "1";
        String slot = "MAINHAND";
        String nbt = "";
        String yComparison = "GREATER_THAN";
        String yValue = "64";
        String healthComparison = "GREATER_THAN";
        String healthValue = "10.0";
        String hungerComparison = "GREATER_THAN";
        String hungerValue = "10";
        String armorComparison = "GREATER_THAN";
        String armorValue = "10";
        String timeMin = "0";
        String timeMax = "24000";
        String dimension = "minecraft:overworld";
        String walkingBlock = "minecraft:grass_block";
        boolean isLogic() { return type.equals("AND") || type.equals("OR") || type.equals("NOT"); }
    }

    private final List<EffectData> effects = new ArrayList<>();
    private final List<ActionData> actions = new ArrayList<>();
    private final List<String> parentIds = new ArrayList<>();
    private List<String> availableSkills = new ArrayList<>();

    // Skill fields
    private String name = "New Skill";
    private String description = "A new skill.";
    private String icon = "minecraft:diamond";
    private String iconNbt = "";
    private int tier = 1;
    private String cost = "10";
    private String cooldown = "0";
    private String exportNamespace = "my_skills";
    private String exportFileName = "new_skill";

    // EditBox widgets
    private EditBox nameField, descField, iconField, iconNbtField, costField, cooldownField;
    private EditBox namespaceField, fileNameField;
    private Button exportButton;

    // Scroll
    private int effectScrollOffset = 0;
    private int actionScrollOffset = 0;

    // Currently selected indices for editing
    private int selectedEffectIdx = -1;
    private int selectedActionIdx = -1;

    // Sub-editor mode: null = main view, "effect", "action"
    private String subEditorMode = null;

    // Editor sub-fields (for effect editing)
    private EditBox[] subFields = new EditBox[6];

    public DeveloperEditorScreen() {
        super(Component.literal("§5§lSkill Developer Editor"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        loadAvailableSkills();
        int w = this.width;
        int h = this.height;

        // LEFT PANEL: Basic Skill Info
        int panelX = 5;
        int fieldW = 110;
        int fieldH = 16;
        int labelX = panelX;
        int editX = panelX;
        int row = 20;

        nameField = addEditBox(editX, row, fieldW, fieldH, "Name", name, v -> name = v); row += 20;
        descField = addEditBox(editX, row, fieldW, fieldH, "Description", description, v -> description = v); row += 20;
        iconField = addEditBox(editX, row, fieldW, fieldH, "Icon (item ID)", icon, v -> icon = v); row += 20;
        iconNbtField = addEditBox(editX, row, fieldW, fieldH, "Icon NBT", iconNbt, v -> iconNbt = v); row += 20;
        costField = addEditBox(editX, row, 50, fieldH, "Cost", cost, v -> cost = v);
        cooldownField = addEditBox(editX + 55, row, 50, fieldH, "CD", cooldown, v -> cooldown = v); row += 20;

        // Tier buttons
        this.addRenderableWidget(Button.builder(Component.literal("Tier: " + tier), b -> {
            tier = (tier % 5) + 1;
            b.setMessage(Component.literal("Tier: " + tier));
        }).bounds(editX, row, fieldW, 14).build()); row += 18;

        // Namespace/Filename
        namespaceField = addEditBox(editX, row, fieldW, fieldH, "Namespace", exportNamespace, v -> exportNamespace = v); row += 20;
        fileNameField = addEditBox(editX, row, fieldW, fieldH, "File name", exportFileName, v -> exportFileName = v); row += 20;

        // Export button
        exportButton = this.addRenderableWidget(Button.builder(Component.literal("Export to Server"), b -> {
            sendExportPacket();
        }).bounds(editX, row, fieldW, 14).build()); row += 18;

        // Save locally button
        this.addRenderableWidget(Button.builder(Component.literal("Save Local"), b -> {
            saveLocally();
        }).bounds(editX, row, fieldW, 14).build()); row += 18;

        // MIDDLE PANEL: Effects
        int midX = 130;
        this.addRenderableWidget(Button.builder(Component.literal("+ Effect"), b -> {
            effects.add(new EffectData());
            selectedEffectIdx = effects.size() - 1;
        }).bounds(midX, 20, 60, 12).build());
        this.addRenderableWidget(Button.builder(Component.literal("- Effect"), b -> {
            if (selectedEffectIdx >= 0 && selectedEffectIdx < effects.size()) {
                effects.remove(selectedEffectIdx);
                selectedEffectIdx = Math.min(selectedEffectIdx, effects.size() - 1);
            }
        }).bounds(midX + 62, 20, 60, 12).build());

        // RIGHT PANEL: Actions
        int rightX = w / 2 + 5;
        this.addRenderableWidget(Button.builder(Component.literal("+ Action"), b -> {
            actions.add(new ActionData());
            selectedActionIdx = actions.size() - 1;
        }).bounds(rightX, 20, 60, 12).build());
        this.addRenderableWidget(Button.builder(Component.literal("- Action"), b -> {
            if (selectedActionIdx >= 0 && selectedActionIdx < actions.size()) {
                actions.remove(selectedActionIdx);
                selectedActionIdx = Math.min(selectedActionIdx, actions.size() - 1);
            }
        }).bounds(rightX + 62, 20, 60, 12).build());

        // Parent prerequisite buttons
        this.addRenderableWidget(Button.builder(Component.literal("+ Parent"), b -> {
            if (!availableSkills.isEmpty()) parentIds.add(availableSkills.get(0));
        }).bounds(editX, h - 40, 55, 12).build());
        this.addRenderableWidget(Button.builder(Component.literal("- Parent"), b -> {
            if (!parentIds.isEmpty()) parentIds.remove(parentIds.size() - 1);
        }).bounds(editX + 57, h - 40, 55, 12).build());
    }

    private EditBox addEditBox(int x, int y, int w, int h, String hint, String value,
                                java.util.function.Consumer<String> responder) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.literal(hint));
        box.setValue(value);
        box.setMaxLength(256);
        box.setResponder(responder);
        this.addRenderableWidget(box);
        return box;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, delta);

        int w = this.width;
        int h = this.height;

        // Left panel labels
        g.fill(0, 0, 125, h, 0xCC101010);
        g.drawString(font, "§e§lSkill Info", 5, 10, -1, true);

        // Middle panel: Effects list
        int midX = 130;
        int midW = (w / 2 - 5) - midX;
        g.fill(midX - 2, 0, midX + midW, h, 0xCC151520);
        g.drawString(font, "§b§lEffects", midX, 10, -1, true);

        int effY = 35;
        for (int i = 0; i < effects.size(); i++) {
            EffectData e = effects.get(i);
            int eY = effY + (i - effectScrollOffset) * 18;
            if (eY < 35 || eY > h - 30) continue;
            boolean sel = (i == selectedEffectIdx);
            g.fill(midX, eY, midX + midW - 2, eY + 16, sel ? 0xFF204020 : 0xFF202020);
            g.drawString(font, (i + 1) + ". " + e.type + " (" + getEffectSummary(e) + ")", midX + 2, eY + 4, sel ? 0xFFFF55 : 0xAAAAAA, false);
            if (this.minecraft != null && mouseX >= midX && mouseX < midX + midW - 2 && mouseY >= eY && mouseY < eY + 16) {
                renderEffectEditor(g, e, midX, h - 80, midW - 2);
            }
        }
        Button selEffBtn = addClickRegion(g, midX, effY + effects.size() * 18, midW - 2, 0);
        renderEffectEditorIfSelected(g, midX, midW - 2);

        // Right panel: Actions list
        int rightX = w / 2 + 5;
        int rightW = w - rightX - 5;
        g.fill(rightX - 2, 0, rightX + rightW, h, 0xCC201515);
        g.drawString(font, "§c§lActions", rightX, 10, -1, true);

        int actY = 35;
        for (int i = 0; i < actions.size(); i++) {
            ActionData a = actions.get(i);
            int aY = actY + (i - actionScrollOffset) * 18;
            if (aY < 35 || aY > h - 30) continue;
            boolean sel = (i == selectedActionIdx);
            g.fill(rightX, aY, rightX + rightW - 2, aY + 16, sel ? 0xFF402020 : 0xFF202020);
            g.drawString(font, (i + 1) + ". " + a.trigger + " → " + a.rootEffect.type, rightX + 2, aY + 4, sel ? 0xFF5555 : 0xAAAAAA, false);
        }
        renderActionEditorIfSelected(g, rightX, rightW - 2);

        // Bottom: JSON preview
        int jsonY = h - 28;
        g.fill(0, jsonY - 2, w, h, 0xCC000000);
        String json = generateJson();
        String preview = json.length() > w / 5 ? json.substring(0, w / 5) + "..." : json;
        g.drawString(font, "§7" + preview, 5, jsonY + 2, -1, false);

        // Parents
        g.drawString(font, "§7Parents: " + String.join(", ", parentIds), 5, h - 25, 0xAAAAAA, false);
    }

    // Dummy placeholder - actual click is handled in mouseClicked
    private Button addClickRegion(GuiGraphics g, int x, int y, int w, int h) { return null; }

    private String getEffectSummary(EffectData e) {
        return switch (e.type) {
            case "Attribute" -> e.attr.replace("minecraft:generic.", "") + " " + e.op + " " + e.val;
            case "Potion" -> e.effectId.replace("minecraft:", "") + " " + e.amplifier;
            case "Enchantment" -> e.enchId.replace("minecraft:", "") + " " + e.enchLevel;
            default -> e.type;
        };
    }

    private void renderEffectEditorIfSelected(GuiGraphics g, int x, int w) {
        if (selectedEffectIdx < 0 || selectedEffectIdx >= effects.size()) return;
        EffectData e = effects.get(selectedEffectIdx);
        int y = this.height - 90;
        g.fill(x, y, x + w, this.height - 30, 0xEE303030);
        g.drawString(font, "§eEdit Effect #" + (selectedEffectIdx + 1), x + 2, y + 2, -1, false);
        g.drawString(font, "Type: " + e.type + " (press T to cycle)", x + 2, y + 12, 0xAAAAAA, false);
        g.drawString(font, "Val: " + getEffectEditInfo(e), x + 2, y + 22, 0xFFFFFF, false);
    }

    private String getEffectEditInfo(EffectData e) {
        return switch (e.type) {
            case "Attribute" -> e.attr + " " + e.op + " =" + e.val;
            case "Potion" -> e.effectId + " amp=" + e.amplifier + " hide=" + e.hideParticles;
            case "Enchantment" -> e.enchId + " lv=" + e.enchLevel + " slot=" + e.enchSlot;
            case "Attack Knockback" -> "val=" + e.knockbackValue;
            case "Experience" -> "val=" + e.xpValue;
            case "Swim Speed" -> "val=" + e.swimValue;
            case "Lava Speed" -> "val=" + e.lavaValue;
            case "Effect Immunity" -> e.immuneEffectId;
            default -> "";
        };
    }

    private void renderEffectEditor(GuiGraphics g, EffectData e, int x, int y, int w) {}

    private void renderActionEditorIfSelected(GuiGraphics g, int x, int w) {
        if (selectedActionIdx < 0 || selectedActionIdx >= actions.size()) return;
        ActionData a = actions.get(selectedActionIdx);
        int y = this.height - 90;
        g.fill(x, y, x + w, this.height - 30, 0xEE303030);
        g.drawString(font, "§cEdit Action #" + (selectedActionIdx + 1), x + 2, y + 2, -1, false);
        g.drawString(font, "Trigger: " + a.trigger + " (T to cycle)", x + 2, y + 12, 0xAAAAAA, false);
        g.drawString(font, "Effect: " + a.rootEffect.type + " (E to cycle)", x + 2, y + 22, 0xAAAAAA, false);
        g.drawString(font, "Cmd: " + a.rootEffect.command, x + 2, y + 32, 0xFFFFFF, false);
        if (a.trigger.equals("TIMER")) g.drawString(font, "Interval: " + a.interval, x + 2, y + 42, 0xAAAAAA, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int w = this.width;
        int h = this.height;

        // Click on effect list items
        int midX = 130;
        int midW = (w / 2 - 5) - midX;
        int effY = 35;
        for (int i = 0; i < effects.size(); i++) {
            int eY = effY + (i - effectScrollOffset) * 18;
            if (mouseX >= midX && mouseX < midX + midW - 2 && mouseY >= eY && mouseY < eY + 16) {
                selectedEffectIdx = i;
                return true;
            }
        }

        // Click on action list items
        int rightX = w / 2 + 5;
        int rightW = w - rightX - 5;
        int actY = 35;
        for (int i = 0; i < actions.size(); i++) {
            int aY = actY + (i - actionScrollOffset) * 18;
            if (mouseX >= rightX && mouseX < rightX + rightW - 2 && mouseY >= aY && mouseY < aY + 16) {
                selectedActionIdx = i;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Cycle effect type on 'T' key when effect selected
        if (keyCode == GLFW.GLFW_KEY_T) {
            if (selectedEffectIdx >= 0 && selectedEffectIdx < effects.size()) {
                cycleEffectType(effects.get(selectedEffectIdx));
                return true;
            }
            if (selectedActionIdx >= 0 && selectedActionIdx < actions.size()) {
                cycleTriggerType(actions.get(selectedActionIdx));
                return true;
            }
        }
        // Cycle effect config type on 'E' for actions
        if (keyCode == GLFW.GLFW_KEY_E) {
            if (selectedActionIdx >= 0 && selectedActionIdx < actions.size()) {
                cycleActionEffectType(actions.get(selectedActionIdx).rootEffect);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static final String[] EFFECT_TYPES = {
        "Attribute", "Mining Speed", "Potion", "Enchantment", "Attack Knockback",
        "Experience", "Swim Speed", "Lava Speed", "Effect Immunity", "Creative Flight", "Elytra"
    };
    private static final String[] TRIGGER_TYPES = {
        "BLOCK_BREAK", "ATTACK_TARGET", "ATTACK_SELF", "TIMER", "UNLOCK", "ACTIVATED"
    };
    private static final String[] ACTION_EFFECT_TYPES = {
        "Command", "Burn", "Heal", "Launch", "Delayed", "Raycast"
    };

    private void cycleEffectType(EffectData e) {
        for (int i = 0; i < EFFECT_TYPES.length; i++) {
            if (EFFECT_TYPES[i].equals(e.type)) { e.type = EFFECT_TYPES[(i + 1) % EFFECT_TYPES.length]; return; }
        }
        e.type = EFFECT_TYPES[0];
    }

    private void cycleTriggerType(ActionData a) {
        for (int i = 0; i < TRIGGER_TYPES.length; i++) {
            if (TRIGGER_TYPES[i].equals(a.trigger)) { a.trigger = TRIGGER_TYPES[(i + 1) % TRIGGER_TYPES.length]; return; }
        }
        a.trigger = TRIGGER_TYPES[0];
    }

    private void cycleActionEffectType(EffectConfig cfg) {
        for (int i = 0; i < ACTION_EFFECT_TYPES.length; i++) {
            if (ACTION_EFFECT_TYPES[i].equals(cfg.type)) { cfg.type = ACTION_EFFECT_TYPES[(i + 1) % ACTION_EFFECT_TYPES.length]; return; }
        }
        cfg.type = ACTION_EFFECT_TYPES[0];
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int w = this.width;
        int midX = 130;
        int midW = (w / 2 - 5) - midX;
        int rightX = w / 2 + 5;
        if (mouseX >= midX && mouseX < midX + midW) {
            effectScrollOffset = Math.max(0, effectScrollOffset - (int) amount);
        } else if (mouseX >= rightX) {
            actionScrollOffset = Math.max(0, actionScrollOffset - (int) amount);
        }
        return true;
    }

    private void loadAvailableSkills() {
        try { availableSkills = SkillManager.getAllSkills().stream().map(s -> s.getId().toString()).sorted().collect(Collectors.toList()); }
        catch (Exception e) { availableSkills = new ArrayList<>(); }
    }

    private void sendExportPacket() {
        String json = generateJson();
        if (json.isEmpty()) return;
        SkillNetworking.CHANNEL.sendToServer(
                new SkillNetworking.SaveSkillMessage(exportNamespace, exportFileName, json));
    }

    private void saveLocally() {
        try {
            String json = generateJson();
            if (json.isEmpty()) return;
            java.nio.file.Path dir = this.minecraft.gameDirectory.toPath().resolve("generated_skills");
            java.nio.file.Files.createDirectories(dir);
            String safeName = exportFileName.replaceAll("[^a-zA-Z0-9_\\-.]", "") + ".json";
            java.nio.file.Files.writeString(dir.resolve(safeName), json,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String generateJson() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("name", name);
            root.addProperty("description", description);
            root.addProperty("icon", icon);
            if (iconNbt != null && !iconNbt.trim().isEmpty()) root.addProperty("icon_nbt", iconNbt);
            root.addProperty("tier", tier);
            root.addProperty("cost", tryParse(cost));
            if (tryParse(cooldown) > 0) root.addProperty("cooldown", tryParse(cooldown));

            if (!parentIds.isEmpty()) {
                JsonArray parents = new JsonArray();
                for (String pid : parentIds) { if (!pid.trim().isEmpty()) parents.add(pid); }
                if (!parents.isEmpty()) root.add("prerequisites", parents);
            }

            JsonArray effectsJson = new JsonArray();
            for (EffectData e : effects) {
                JsonObject eff = new JsonObject();
                switch (e.type) {
                    case "Attribute" -> { eff.addProperty("type", "jd_skill_tree:attribute"); eff.addProperty("attribute", e.attr); eff.addProperty("operation", e.op); eff.addProperty("value", parseDouble(e.val)); }
                    case "Mining Speed" -> { eff.addProperty("type", "jd_skill_tree:mining_speed"); eff.addProperty("value", parseDouble(e.val)); }
                    case "Potion" -> { eff.addProperty("type", "jd_skill_tree:potion"); eff.addProperty("effect", e.effectId); eff.addProperty("amplifier", tryParse(e.amplifier)); eff.addProperty("hide_particles", e.hideParticles); }
                    case "Enchantment" -> { eff.addProperty("type", "jd_skill_tree:enchantment"); eff.addProperty("enchantment", e.enchId); eff.addProperty("level_added", tryParse(e.enchLevel)); eff.addProperty("slot", e.enchSlot); eff.addProperty("over_enchant", e.overEnchant); }
                    case "Attack Knockback" -> { eff.addProperty("type", "jd_skill_tree:attack_knockback"); eff.addProperty("value", parseDouble(e.knockbackValue)); }
                    case "Experience" -> { eff.addProperty("type", "jd_skill_tree:experience"); eff.addProperty("value", parseDouble(e.xpValue)); }
                    case "Swim Speed" -> { eff.addProperty("type", "jd_skill_tree:swim_speed"); eff.addProperty("value", parseDouble(e.swimValue)); }
                    case "Lava Speed" -> { eff.addProperty("type", "jd_skill_tree:lava_speed"); eff.addProperty("value", parseDouble(e.lavaValue)); }
                    case "Effect Immunity" -> { eff.addProperty("type", "jd_skill_tree:effect_immunity"); eff.addProperty("effect", e.immuneEffectId); }
                    case "Creative Flight" -> eff.addProperty("type", "jd_skill_tree:creative_flight");
                    case "Elytra" -> eff.addProperty("type", "jd_skill_tree:elytra");
                }
                if (e.condition != null) eff.add("condition", generateConditionJson(e.condition));
                effectsJson.add(eff);
            }
            if (!effectsJson.isEmpty()) root.add("effects", effectsJson);

            JsonArray actionsJson = new JsonArray();
            for (ActionData a : actions) {
                JsonObject act = new JsonObject();
                act.addProperty("trigger", a.trigger);
                if (a.trigger.equals("TIMER")) act.addProperty("interval", tryParse(a.interval));
                act.add("effect", generateEffectJson(a.rootEffect));
                if (a.condition != null) act.add("condition", generateConditionJson(a.condition));
                actionsJson.add(act);
            }
            if (!actionsJson.isEmpty()) root.add("actions", actionsJson);

            return new GsonBuilder().setPrettyPrinting().create().toJson(root);
        } catch (Exception e) { return ""; }
    }

    private JsonObject generateEffectJson(EffectConfig config) {
        JsonObject json = new JsonObject();
        switch (config.type) {
            case "Command" -> { json.addProperty("type", "jd_skill_tree:command"); json.addProperty("command", config.command); }
            case "Burn" -> { json.addProperty("type", "jd_skill_tree:burn"); json.addProperty("duration", tryParse(config.duration)); json.addProperty("ignore_armor", config.bool1); }
            case "Explosion" -> { json.addProperty("type", "jd_skill_tree:explosion"); json.addProperty("power", parseFloat(config.value1)); json.addProperty("fire", config.bool1); json.addProperty("break_blocks", config.bool2); }
            case "Heal" -> { json.addProperty("type", "jd_skill_tree:heal"); json.addProperty("amount", parseFloat(config.value1)); json.addProperty("is_hunger", config.bool1); }
            case "Launch" -> { json.addProperty("type", "jd_skill_tree:launch"); json.addProperty("strength", parseFloat(config.value1)); json.addProperty("vertical", parseFloat(config.value2)); }
            case "Delayed" -> {
                json.addProperty("type", "jd_skill_tree:delayed");
                json.addProperty("delay", tryParse(config.duration));
                if (config.childEffect != null) json.add("effect", generateEffectJson(config.childEffect));
                if (config.childCondition != null) json.add("condition", generateConditionJson(config.childCondition));
            }
            case "Raycast" -> {
                json.addProperty("type", "jd_skill_tree:raycast");
                json.addProperty("length", parseDouble(config.value1));
                json.addProperty("fluids", config.bool1);
                json.addProperty("entities", config.bool2);
                if (config.childEffect != null) json.add("effect", generateEffectJson(config.childEffect));
            }
        }
        return json;
    }

    private JsonObject generateConditionJson(ConditionData c) {
        JsonObject cond = new JsonObject();
        switch (c.type) {
            case "AND" -> { cond.addProperty("type", "jd_skill_tree:and"); JsonArray ch = new JsonArray(); c.children.forEach(cd -> ch.add(generateConditionJson(cd))); cond.add("conditions", ch); }
            case "OR" -> { cond.addProperty("type", "jd_skill_tree:or"); JsonArray ch = new JsonArray(); c.children.forEach(cd -> ch.add(generateConditionJson(cd))); cond.add("conditions", ch); }
            case "NOT" -> { cond.addProperty("type", "jd_skill_tree:not"); if (c.child != null) cond.add("condition", generateConditionJson(c.child)); }
            case "Hand Item" -> { cond.addProperty("type", "jd_skill_tree:hand_item"); cond.addProperty("item", c.item); cond.addProperty("count", tryParse(c.count)); cond.addProperty("slot", c.slot.toLowerCase()); if (c.nbt != null && !c.nbt.trim().isEmpty()) cond.addProperty("nbt", c.nbt); }
            case "Equipped Item" -> { cond.addProperty("type", "jd_skill_tree:equipped_item"); cond.addProperty("item", c.item); cond.addProperty("slot", c.slot.toLowerCase()); if (c.nbt != null && !c.nbt.trim().isEmpty()) cond.addProperty("nbt", c.nbt); }
            case "Health" -> { cond.addProperty("type", "jd_skill_tree:health"); cond.addProperty("comparison", c.healthComparison); cond.addProperty("amount", parseFloat(c.healthValue)); }
            case "Hunger" -> { cond.addProperty("type", "jd_skill_tree:hunger"); cond.addProperty("comparison", c.hungerComparison); cond.addProperty("amount", tryParse(c.hungerValue)); }
            case "Armor" -> { cond.addProperty("type", "jd_skill_tree:armor"); cond.addProperty("comparison", c.armorComparison); cond.addProperty("amount", tryParse(c.armorValue)); }
            case "Sprinting" -> cond.addProperty("type", "jd_skill_tree:sprinting");
            case "Crouching" -> cond.addProperty("type", "jd_skill_tree:crouching");
            case "Wetness" -> cond.addProperty("type", "jd_skill_tree:wetness");
            case "In Lava" -> cond.addProperty("type", "jd_skill_tree:in_lava");
            case "Y-Level" -> { cond.addProperty("type", "jd_skill_tree:y_level"); cond.addProperty("comparison", c.yComparison); cond.addProperty("y_level", tryParse(c.yValue)); }
            case "Time" -> { cond.addProperty("type", "jd_skill_tree:time"); cond.addProperty("min", tryParse(c.timeMin)); cond.addProperty("max", tryParse(c.timeMax)); }
            case "Dimension" -> { cond.addProperty("type", "jd_skill_tree:dimension"); cond.addProperty("dimension", c.dimension); }
            case "Walking On" -> { cond.addProperty("type", "jd_skill_tree:walking_on"); cond.addProperty("block", c.walkingBlock); }
        }
        return cond;
    }

    private int tryParse(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private double parseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }
    private float parseFloat(String s) { try { return Float.parseFloat(s.trim()); } catch (Exception e) { return 0.0f; } }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
