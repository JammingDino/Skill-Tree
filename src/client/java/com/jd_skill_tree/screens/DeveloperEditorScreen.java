package com.jd_skill_tree.screens;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.screens.widgets.SkillWidget;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.actions.BurnActionEffect;
import com.jd_skill_tree.skills.actions.CommandActionEffect;
import com.jd_skill_tree.skills.actions.SkillAction;
import com.jd_skill_tree.skills.actions.SkillActionEffect;
import com.jd_skill_tree.skills.conditions.*;
import com.jd_skill_tree.skills.effects.*;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DeveloperEditorScreen extends BaseOwoScreen<StackLayout> {

    // --- Autosave File ---
    private static final Path AUTOSAVE_PATH = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config").resolve("jd_skill_tree_editor_autosave.json");

    // --- State ---
    private String name = "New Skill";
    private String cost = "100";
    private String icon = "minecraft:apple";
    private String iconNbt = "";
    private String description = "Description...";
    private final List<String> parentIds = new ArrayList<>();
    private int tier = 1;

    // --- Component References ---
    private TextBoxComponent nameField;
    private TextBoxComponent costField;
    private ButtonComponent tierButton;
    private TextBoxComponent iconField;
    private TextBoxComponent iconNbtField;
    private TextBoxComponent descriptionField;
    private TextBoxComponent namespaceField;
    private TextBoxComponent fileNameField;

    // --- Data Classes ---
    private static class EffectData {
        ConditionData condition = null;

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
    }
    private final List<EffectData> effects = new ArrayList<>();

    private static class ActionData {
        ConditionData condition = null;

        String trigger = "BLOCK_BREAK";
        String interval = "20";
        String effectType = "Command";
        String command = "/say hi";
        String burnDuration = "100";
        boolean burnIgnoreArmor = false;
    }
    private final List<ActionData> actions = new ArrayList<>();

    // --- Condition Data ---
    private static class ConditionData {
        // --- Type Identifier ---
        // Options: AND, OR, NOT, Hand Item, Equipped Item, Y-Level, Health, Hunger, Armor,
        // Time, Dimension, Walking On, Wetness, In Lava, Sprinting, Crouching
        String type = "Hand Item";

        // --- Logic Structure (Recursive) ---
        List<ConditionData> children = new ArrayList<>(); // Used for AND, OR
        ConditionData child = null;                       // Used for NOT

        // --- Item Conditions (Hand Item, Equipped Item) ---
        String item = "minecraft:stick";
        String count = "1";
        // Used for both Hand (MAINHAND/OFFHAND) and Armor slots (HELMET/CHEST/LEGS/BOOTS)
        String slot = "MAINHAND";
        String nbt = "";

        // --- Y-Level Condition ---
        String yComparison = "GREATER_THAN";
        String yValue = "64";

        // --- Health Condition ---
        String healthComparison = "GREATER_THAN";
        String healthValue = "10.0"; // Float value (20.0 = 10 hearts)

        // --- Hunger Condition ---
        String hungerComparison = "GREATER_THAN";
        String hungerValue = "10";   // Int value (20 = full bar)

        // --- Armor Condition ---
        String armorComparison = "GREATER_THAN";
        String armorValue = "10";

        // --- Time Condition ---
        String timeMin = "0";
        String timeMax = "24000";

        // --- Dimension Condition ---
        String dimension = "minecraft:overworld";

        // --- Walking On Block Condition ---
        String walkingBlock = "minecraft:grass_block";

        // --- Boolean/State Conditions ---
        // Sprinting, Crouching, Wetness, In Lava don't need extra fields.
        // Their existence is defined solely by the 'type' string.

        /**
         * Helper to check if this node holds other conditions.
         */
        boolean isLogic() {
            return type.equals("AND") || type.equals("OR") || type.equals("NOT");
        }
    }

    private SkillWidget previewWidget;
    private Skill previewSkill;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new com.jd_skill_tree.skills.IdentifierAdapter())
            .registerTypeAdapter(new com.google.common.reflect.TypeToken<java.util.List<com.jd_skill_tree.skills.effects.SkillEffect>>(){}.getType(), new SkillEffectListAdapter())
            .setPrettyPrinting()
            .create();

    // --- UI Variables ---
    private FlowLayout effectsContainer;
    private FlowLayout actionsContainer;
    private FlowLayout conditionsContainer;
    private FlowLayout parentsContainer;
    private FlowLayout overlayLayer;
    private LabelComponent exportTextDisplay;
    private List<String> availableSkills = new ArrayList<>();

    private String exportNamespace = "my_skills";
    private String exportFileName = "new_skill";
    private String loadSkillId = "";

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::stack);
    }

    @Override
    protected void build(StackLayout root) {
        // 1. Load State
        loadEditorState();
        loadAvailableSkills();

        // Capture lists so we can rebuild the UI rows
        List<String> loadedParents = new ArrayList<>(this.parentIds);
        this.parentIds.clear();

        List<EffectData> loadedEffects = new ArrayList<>(this.effects);
        this.effects.clear();

        List<ActionData> loadedActions = new ArrayList<>(this.actions);
        this.actions.clear();

        root.surface(Surface.VANILLA_TRANSLUCENT);

        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        mainContent.gap(10);
        mainContent.padding(Insets.of(5));

        // ==========================================================================================
        // LEFT COLUMN: EDITOR
        // ==========================================================================================
        FlowLayout editorContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.surface(Surface.DARK_PANEL).padding(Insets.of(10));
        editorContent.child(Components.label(Text.of("Skill Editor")).shadow(true).margins(Insets.bottom(15)));

        // Row 1: Name, Cost, Tier
        var nameLayout = field("Name", name, s -> { name = s; updatePreview(); }, 50);
        this.nameField = (TextBoxComponent) nameLayout.children().get(1);

        var costLayout = field("Cost", cost, s -> { cost = s; updatePreview(); }, 20);
        this.costField = (TextBoxComponent) costLayout.children().get(1);

        var tierLayout = dropdown("Tier", List.of("1", "2", "3", "4", "5"), String.valueOf(tier), s -> { tier = Integer.parseInt(s); updatePreview(); }, 30);
        this.tierButton = (ButtonComponent) tierLayout.children().get(1);

        editorContent.child(row(nameLayout, costLayout, tierLayout));

        // Row 2: Icon
        List<String> itemIds = Registries.ITEM.getIds().stream().map(Identifier::toString).sorted().toList();
        var iconLayout = autocompleteField("Icon (Item ID)", icon, itemIds, s -> { icon = s; updatePreview(); }, 100);
        this.iconField = (TextBoxComponent) iconLayout.children().get(1);
        editorContent.child(iconLayout);

        // Icon NBT
        var iconNbtLayout = field("Icon NBT (e.g. {Enchantments:[{}]})", iconNbt, s -> { iconNbt = s; updatePreview(); }, 100);
        this.iconNbtField = (TextBoxComponent) iconNbtLayout.children().get(1);
        editorContent.child(iconNbtLayout);

        // Row 3: Description
        var descLayout = field("Description", description, s -> { description = s; updatePreview(); }, 100);
        this.descriptionField = (TextBoxComponent) descLayout.children().get(1);
        editorContent.child(descLayout);

        // Divider
        editorContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(10)));

        // Prerequisites Section
        FlowLayout parentsHeader = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        parentsHeader.child(Components.label(Text.of("Prerequisites")).color(Color.ofRgb(0x55FFFF)));
        parentsHeader.margins(Insets.bottom(5));
        editorContent.child(parentsHeader);

        parentsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.child(parentsContainer);

        var addParentBtn = Components.button(Text.of("+ Add Parent Skill"), btn -> { addParentRow(""); updatePreview(); });
        addParentBtn.sizing(Sizing.fill(100), Sizing.fixed(20)).margins(Insets.bottom(5));
        applyStandardButtonRenderer(addParentBtn);
        editorContent.child(addParentBtn);

        // Divider
        editorContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(10)));

        // Effects Section
        editorContent.child(Components.label(Text.of("Passive Effects")).color(Color.ofRgb(0x55FFFF)).margins(Insets.bottom(5)));
        effectsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.child(effectsContainer);

        var addEffectBtn = Components.button(Text.of("+ Add Effect"), btn -> { addEffectRow(new EffectData()); updatePreview(); });
        addEffectBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
        applyStandardButtonRenderer(addEffectBtn);
        editorContent.child(addEffectBtn);

        // Actions Section
        editorContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(10)));
        editorContent.child(Components.label(Text.of("Actions (Triggers)")).color(Color.ofRgb(0x55FFFF)).margins(Insets.bottom(5)));

        actionsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.child(actionsContainer);

        var addActionBtn = Components.button(Text.of("+ Add Action"), btn -> { addActionRow(new ActionData()); updatePreview(); });
        addActionBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
        applyStandardButtonRenderer(addActionBtn);
        editorContent.child(addActionBtn);

        // Restore Rows
        for (String p : loadedParents) addParentRow(p);
        for (EffectData e : loadedEffects) addEffectRow(e);
        for (ActionData a : loadedActions) addActionRow(a);

        // Clear/Reset Button
        editorContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(15)));
        var clearBtn = Components.button(Text.of("Clear Skill / Reset"), btn -> clearEditorState());
        clearBtn.renderer(ButtonComponent.Renderer.flat(0x44FF0000, 0x88FF0000, 0xFFFF0000));
        clearBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
        editorContent.child(clearBtn);

        var editorScroll = Containers.verticalScroll(Sizing.fill(63), Sizing.fill(100), editorContent);
        mainContent.child(editorScroll);

        // ==========================================================================================
        // RIGHT COLUMN: SIDEBAR (SCROLLABLE)
        // ==========================================================================================
        FlowLayout sidebarContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        sidebarContent.surface(Surface.DARK_PANEL).padding(Insets.of(10));
        sidebarContent.alignment(HorizontalAlignment.LEFT, VerticalAlignment.TOP);

        // Load Skill
        sidebarContent.child(Components.label(Text.of("Load Existing Skill")).shadow(true).margins(Insets.bottom(5)));
        var loadContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        loadContainer.gap(5);
        loadContainer.verticalAlignment(VerticalAlignment.TOP);

        var loadInputLayout = autocompleteField("", "", availableSkills, s -> loadSkillId = s, 70);
        TextBoxComponent loadInput = (TextBoxComponent) loadInputLayout.children().get(0);

        var loadBtn = Components.button(Text.of("Load"), btn -> {
            if (loadSkillId != null && !loadSkillId.isEmpty()) {
                Identifier id = Identifier.tryParse(loadSkillId);
                if (id != null) {
                    Optional<Skill> skill = SkillManager.getSkill(id);
                    if (skill.isPresent()) {
                        loadSkillIntoEditor(skill.get());
                        btn.setMessage(Text.of("Loaded!"));
                        new Thread(() -> { try { Thread.sleep(1500); MinecraftClient.getInstance().execute(() -> btn.setMessage(Text.of("Load"))); } catch (Exception ignored) {} }).start();
                    } else {
                        btn.setMessage(Text.of("Not Found"));
                    }
                }
            }
        });
        loadBtn.sizing(Sizing.fill(30), Sizing.fixed(20));
        applyStandardButtonRenderer(loadBtn);

        loadContainer.child(loadInputLayout);
        loadContainer.child(loadBtn);
        sidebarContent.child(loadContainer);
        sidebarContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(10)));

        // Preview
        sidebarContent.child(Components.label(Text.of("Preview")).shadow(true).margins(Insets.bottom(5)));
        sidebarContent.child(new PreviewComponent().margins(Insets.bottom(5)));

        // Export Settings
        sidebarContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.bottom(5)));
        sidebarContent.child(Components.label(Text.of("Export Settings")).shadow(true).margins(Insets.bottom(5)));

        var nsLayout = field("Namespace", exportNamespace, s -> exportNamespace = s, 100);
        this.namespaceField = (TextBoxComponent) nsLayout.children().get(1);
        sidebarContent.child(nsLayout);

        var fileLayout = field("File Name", exportFileName, s -> exportFileName = s, 100);
        this.fileNameField = (TextBoxComponent) fileLayout.children().get(1);
        sidebarContent.child(fileLayout);

        // Action Buttons
        FlowLayout buttonsRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonsRow.gap(5);
        buttonsRow.margins(Insets.bottom(10));

        ButtonComponent exportBtn = Components.button(Text.of("Export to Server"), btn -> sendExportPacket(btn));
        exportBtn.sizing(Sizing.fill(50), Sizing.fixed(20));
        applyStandardButtonRenderer(exportBtn);

        ButtonComponent copyBtn = Components.button(Text.of("Copy JSON"), btn -> {
            String json = generateJson();
            if (!json.isEmpty()) {
                MinecraftClient.getInstance().keyboard.setClipboard(json);
                btn.setMessage(Text.of("Copied!"));
                new Thread(() -> { try { Thread.sleep(2000); MinecraftClient.getInstance().execute(() -> btn.setMessage(Text.of("Copy JSON"))); } catch (InterruptedException ignored) {} }).start();
            }
        });
        copyBtn.sizing(Sizing.fill(50), Sizing.fixed(20));
        applyStandardButtonRenderer(copyBtn);

        buttonsRow.child(exportBtn);
        buttonsRow.child(copyBtn);
        sidebarContent.child(buttonsRow);

        // JSON Output
        sidebarContent.child(Components.label(Text.of("JSON Output")).color(Color.ofRgb(0xAAAAAA)).margins(Insets.bottom(2)));

        FlowLayout jsonDisplayContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        jsonDisplayContainer.surface((context, component) -> {
            context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0xFF000000);
            context.drawBorder(component.x(), component.y(), component.width(), component.height(), 0xFF333333);
        }).padding(Insets.of(5));

        exportTextDisplay = Components.label(Text.of(""));
        exportTextDisplay.maxWidth(Integer.MAX_VALUE);
        exportTextDisplay.color(Color.ofRgb(0x00FF00));

        jsonDisplayContainer.child(exportTextDisplay);
        sidebarContent.child(jsonDisplayContainer);

        var sidebarScroll = Containers.verticalScroll(Sizing.fill(35), Sizing.fill(100), sidebarContent);

        mainContent.child(sidebarScroll);
        root.child(mainContent);

        overlayLayer = Containers.verticalFlow(Sizing.fixed(0), Sizing.fixed(0));
        overlayLayer.positioning(Positioning.absolute(0, 0));
        overlayLayer.zIndex(500);
        overlayLayer.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (!overlayLayer.children().isEmpty()) {
                Component child = overlayLayer.children().get(0);
                boolean clickedDropdown = mouseX >= child.x() && mouseX <= child.x() + child.width() && mouseY >= child.y() && mouseY <= child.y() + child.height();
                if (!clickedDropdown) { closeOverlay(); return true; }
            }
            return false;
        });
        root.child(overlayLayer);

        updatePreview();
    }

    // --- HELPER: Visual Styles ---

    private void applyStandardButtonRenderer(ButtonComponent btn) {
        btn.renderer((context, button, delta) -> {
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), 0xFF000000);
            int borderColor = button.isHovered() ? 0xFFFFFFFF : 0xFFA0A0A0;
            context.drawBorder(button.x(), button.y(), button.width(), button.height(), borderColor);
            context.drawText(MinecraftClient.getInstance().textRenderer, button.getMessage(),
                    button.x() + (button.width() - MinecraftClient.getInstance().textRenderer.getWidth(button.getMessage())) / 2,
                    button.y() + (button.height() - 8) / 2,
                    0xFFE0E0E0, false);
        });
    }

    // --- AUTOSAVE & CLEAR LOGIC ---

    @Override
    public void removed() {
        saveEditorState();
        super.removed();
    }

    private void saveEditorState() {
        try {
            EditorSaveState state = new EditorSaveState();
            state.name = this.name;
            state.cost = this.cost;
            state.icon = this.icon;
            state.description = this.description;
            state.tier = this.tier;
            state.namespace = this.exportNamespace;
            state.fileName = this.exportFileName;
            state.parents = this.parentIds;
            state.effects = this.effects;
            state.actions = this.actions;

            String json = GSON.toJson(state);
            Files.createDirectories(AUTOSAVE_PATH.getParent());
            Files.writeString(AUTOSAVE_PATH, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadEditorState() {
        if (!Files.exists(AUTOSAVE_PATH)) return;
        try {
            String json = Files.readString(AUTOSAVE_PATH);
            EditorSaveState state = GSON.fromJson(json, EditorSaveState.class);
            if (state != null) {
                this.name = state.name;
                this.cost = state.cost;
                this.icon = state.icon;
                this.description = state.description;
                this.tier = state.tier;
                this.exportNamespace = state.namespace;
                this.exportFileName = state.fileName;
                if (state.parents != null) this.parentIds.addAll(state.parents);
                if (state.effects != null) this.effects.addAll(state.effects);
                if (state.actions != null) this.actions.addAll(state.actions);
            }
        } catch (Exception e) {
            System.err.println("Failed to load editor autosave");
        }
    }

    private void clearEditorState() {
        this.name = "New Skill";
        this.cost = "100";
        this.tier = 1;
        this.icon = "minecraft:apple";
        this.iconNbt = "";
        if (iconNbtField != null) iconNbtField.setText("");
        this.description = "Description...";
        this.exportNamespace = "my_skills";
        this.exportFileName = "new_skill";
        this.parentIds.clear();
        this.effects.clear();
        this.actions.clear();

        if (nameField != null) nameField.setText(name);
        if (costField != null) costField.setText(cost);
        if (tierButton != null) tierButton.setMessage(Text.of("1"));
        if (iconField != null) iconField.setText(icon);
        if (descriptionField != null) descriptionField.setText(description);
        if (namespaceField != null) namespaceField.setText(exportNamespace);
        if (fileNameField != null) fileNameField.setText(exportFileName);

        if (parentsContainer != null) parentsContainer.clearChildren();
        if (effectsContainer != null) effectsContainer.clearChildren();
        if (actionsContainer != null) actionsContainer.clearChildren();
        if (conditionsContainer != null) conditionsContainer.clearChildren();

        updatePreview();
        saveEditorState();
    }

    private static class EditorSaveState {
        String name, cost, icon, description, namespace, fileName;
        int tier;
        List<String> parents;
        List<EffectData> effects;
        List<ActionData> actions;
        List<ConditionData> conditions;
    }

    // --- LOGIC: Load Skill ---

    private void loadSkillIntoEditor(Skill skill) {
        this.name = skill.getName();
        this.nameField.setText(this.name);
        this.cost = String.valueOf(skill.getCost());
        this.costField.setText(this.cost);
        this.tier = skill.getTier();
        this.tierButton.setMessage(Text.of(String.valueOf(this.tier)));
        this.description = skill.getDescription();
        this.descriptionField.setText(this.description);
        this.icon = Registries.ITEM.getId(skill.getIcon().getItem()).toString();
        this.iconField.setText(this.icon);
        this.iconNbt = skill.getIconNbt() != null ? skill.getIconNbt() : "";
        this.iconNbtField.setText(this.iconNbt);

        if (skill.getId() != null) {
            this.exportNamespace = skill.getId().getNamespace();
            this.exportFileName = skill.getId().getPath();
            this.namespaceField.setText(this.exportNamespace);
            this.fileNameField.setText(this.exportFileName);
        }

        this.parentIds.clear();
        this.parentsContainer.clearChildren();
        for (Identifier pid : skill.getPrerequisiteIds()) {
            addParentRow(pid.toString());
        }

        this.effects.clear();
        this.effectsContainer.clearChildren();
        for (SkillEffect effect : skill.getEffects()) {
            EffectData data = new EffectData();

            if (effect.getCondition() != null) {
                data.condition = convertConditionToData(effect.getCondition());
            }

            if (effect instanceof AttributeSkillEffect attr) {
                data.type = "Attribute";
                data.attr = Objects.requireNonNull(Registries.ATTRIBUTE.getId(attr.getAttribute())).toString();
                data.op = attr.getOperation().name();
                data.val = String.valueOf(attr.getValue());
            }
            else if (effect instanceof MiningSpeedSkillEffect mining) {
                data.type = "Mining Speed";
                data.val = String.valueOf(mining.getMultiplier());
            }
            else if (effect instanceof PotionSkillEffect pot) {
                data.type = "Potion";
                data.effectId = pot.getEffectId().toString();
                data.amplifier = String.valueOf(pot.getAmplifier());
                data.hideParticles = pot.shouldHideParticles();
            }
            else if (effect instanceof EnchantmentSkillEffect ench) {
                data.type = "Enchantment";
                data.enchId = ench.getEnchantmentId().toString();
                data.enchLevel = String.valueOf(ench.getLevelAdded());
                data.overEnchant = ench.isAllowOverEnchanting();
                data.enchSlot = switch(ench.getTargetSlot()) {
                    case CHEST -> "chest";
                    case LEGS -> "legs";
                    case FEET -> "boots";
                    default -> "helmet";
                };
            }
            else if (effect instanceof AttackKnockbackSkillEffect kb) {
                data.type = "Attack Knockback";
                data.knockbackValue = String.valueOf(kb.getAmount());
            }
            else if (effect instanceof ExperienceSkillEffect xp) {
                data.type = "Experience";
                data.xpValue = String.valueOf(xp.getMultiplier());
            }
            else if (effect instanceof SwimSpeedSkillEffect swim) {
                data.type = "Swim Speed";
                data.swimValue = String.valueOf(swim.getMultiplier());
            }
            else if (effect instanceof LavaSpeedSkillEffect lava) {
                data.type = "Lava Speed";
                data.lavaValue = String.valueOf(lava.getMultiplier());
            }

            addEffectRow(data);
        }

        this.actions.clear();
        this.actionsContainer.clearChildren();
        for (SkillAction action : skill.getActions()) {
            ActionData data = new ActionData();
            data.trigger = action.getTrigger().name();
            data.interval = String.valueOf(action.getInterval());

            if (action.getCondition() != null) {
                data.condition = convertConditionToData(action.getCondition());
            }

            SkillActionEffect effect = action.getEffect();

            if (effect instanceof CommandActionEffect cmd) {
                data.effectType = "Command";
                data.command = cmd.getCommand();
            } else if (effect instanceof BurnActionEffect burn) {
                data.effectType = "Burn";
                data.burnDuration = String.valueOf(burn.getDuration());
                data.burnIgnoreArmor = burn.isIgnoreArmor();
            }
            addActionRow(data);
        }

        updatePreview();
    }

    private ConditionData convertConditionToData(SkillCondition cond) {
        if (cond == null) return null;

        ConditionData data = new ConditionData();

        // --- LOGIC CONDITIONS ---
        if (cond instanceof com.jd_skill_tree.skills.conditions.AndCondition c) {
            data.type = "AND";
            for (SkillCondition child : c.getConditions()) {
                data.children.add(convertConditionToData(child));
            }
        }
        else if (cond instanceof com.jd_skill_tree.skills.conditions.OrCondition c) {
            data.type = "OR";
            for (SkillCondition child : c.getConditions()) {
                data.children.add(convertConditionToData(child));
            }
        }
        else if (cond instanceof com.jd_skill_tree.skills.conditions.NotCondition c) {
            data.type = "NOT";
            data.child = convertConditionToData(c.getCondition());
        }

        // --- ITEM CONDITIONS ---
        else if (cond instanceof HandItemCondition c) {
            data.type = "Hand Item";
            data.item = Registries.ITEM.getId(c.getTargetItem()).toString();
            data.count = String.valueOf(c.getMinCount());
            data.slot = c.getSlot().name();
            data.nbt = c.getNbt() != null ? c.getNbt().toString() : "";
        }
        else if (cond instanceof EquippedItemCondition c) {
            data.type = "Equipped Item";
            data.item = Registries.ITEM.getId(c.getTargetItem()).toString();
            // Convert EquipmentSlot enum to GUI friendly string
            data.slot = switch (c.getSlot()) {
                case FEET -> "BOOTS";
                case LEGS -> "LEGS";
                case CHEST -> "CHEST";
                default -> "HELMET";
            };
            data.nbt = c.getNbt() != null ? c.getNbt().toString() : "";
        }

        // --- PLAYER STATS ---
        else if (cond instanceof HealthCondition c) {
            data.type = "Health";
            data.healthComparison = c.getComparison().name();
            data.healthValue = String.valueOf(c.getTargetHealth());
        }
        else if (cond instanceof HungerCondition c) {
            data.type = "Hunger";
            data.hungerComparison = c.getComparison().name();
            data.hungerValue = String.valueOf(c.getTargetHunger());
        }
        else if (cond instanceof ArmorCondition c) {
            data.type = "Armor";
            data.armorComparison = c.getComparison().name();
            data.armorValue = String.valueOf(c.getTargetArmor());
        }
        else if (cond instanceof SprintingCondition) {
            data.type = "Sprinting";
        }
        else if (cond instanceof CrouchingCondition) {
            data.type = "Crouching";
        }
        else if (cond instanceof WetnessCondition) {
            data.type = "Wetness";
        }
        else if (cond instanceof InLavaCondition) {
            data.type = "In Lava";
        }

        // --- WORLD STATE ---
        else if (cond instanceof YLevelCondition c) {
            data.type = "Y-Level";
            data.yComparison = c.getComparison().name();
            data.yValue = String.valueOf(c.getTargetY());
        }
        else if (cond instanceof TimeOfDayCondition c) {
            data.type = "Time";
            data.timeMin = String.valueOf(c.getMinTime());
            data.timeMax = String.valueOf(c.getMaxTime());
        }
        else if (cond instanceof DimensionCondition c) {
            data.type = "Dimension";
            data.dimension = c.getDimensionId().toString();
        }
        else if (cond instanceof WalkingOnBlockCondition c) {
            data.type = "Walking On";
            data.walkingBlock = Registries.BLOCK.getId(c.getTargetBlock()).toString();
        }

        return data;
    }

    // --- WIDGET GENERATORS ---

    private FlowLayout field(String label, String value, Consumer<String> onChange, int weight) {
        var layout = Containers.verticalFlow(Sizing.fill(weight), Sizing.content());
        layout.margins(Insets.bottom(5));
        layout.child(Components.label(Text.of(label)).color(Color.ofArgb(0xFFAAAAAA)));
        var box = Components.textBox(Sizing.fill(100));
        box.verticalSizing(Sizing.fixed(20));
        box.setMaxLength(Integer.MAX_VALUE);
        box.setText(value);
        box.setCursorToStart();
        box.onChanged().subscribe(onChange::accept);
        layout.child(box);
        return layout;
    }

    private FlowLayout autocompleteField(String label, String value, List<String> options, Consumer<String> onChange, int weight) {
        var layout = Containers.verticalFlow(Sizing.fill(weight), Sizing.content());
        if(!label.isEmpty()) layout.child(Components.label(Text.of(label)).color(Color.ofArgb(0xFFAAAAAA)));
        layout.margins(Insets.bottom(5));
        var box = Components.textBox(Sizing.fill(100));
        box.verticalSizing(Sizing.fixed(20));
        box.setMaxLength(Integer.MAX_VALUE);
        box.setText(value);
        box.setCursorToStart();
        Consumer<String> selectItem = (match) -> { box.setText(match); onChange.accept(match); closeOverlay(); };
        box.onChanged().subscribe(text -> {
            onChange.accept(text);
            closeOverlay();
            if (text.isEmpty()) return;
            List<String> matches = options.stream().filter(s -> s.contains(text.toLowerCase())).sorted((a, b) -> {
                boolean aStart = a.startsWith(text); boolean bStart = b.startsWith(text);
                if (aStart && !bStart) return -1; if (!aStart && bStart) return 1; return a.compareTo(b);
            }).limit(50).toList();
            if (matches.isEmpty()) return;
            int itemHeight = 15; int maxItemsVisible = 8; int borderPadding = 2;
            int containerHeight = Math.min(matches.size(), maxItemsVisible) * itemHeight + borderPadding;
            FlowLayout suggestionsList = Containers.verticalFlow(Sizing.fixed(box.width()), Sizing.content());
            for (String match : matches) {
                var itemBtn = Components.button(Text.of(match), btn -> selectItem.accept(match));
                itemBtn.sizing(Sizing.fill(100), Sizing.fixed(itemHeight));
                itemBtn.renderer(ButtonComponent.Renderer.flat(0x00000000, 0xFF444444, 0xFF555555));
                suggestionsList.child(itemBtn);
            }
            ScrollContainer<?> scroll = Containers.verticalScroll(Sizing.fixed(box.width()), Sizing.fixed(containerHeight), suggestionsList);
            scroll.surface((context, component) -> { context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0xFF222222); context.drawBorder(component.x(), component.y(), component.width(), component.height(), 0xFF000000); });
            scroll.padding(Insets.of(1)); scroll.positioning(Positioning.absolute(box.x(), box.y() + box.height())); scroll.zIndex(600);
            openOverlay(scroll);
        });
        box.keyPress().subscribe((code, scan, mod) -> { if (code == GLFW.GLFW_KEY_TAB) { if (!overlayLayer.children().isEmpty()) { Component child = overlayLayer.children().get(0); if (child instanceof ScrollContainer<?> scroll) { if (scroll.child() instanceof FlowLayout list && !list.children().isEmpty()) { Component firstBtn = list.children().get(0); if(firstBtn instanceof ButtonComponent btn) { selectItem.accept(btn.getMessage().getString()); return true; } } } } } return false; });
        layout.child(box);
        return layout;
    }

    private FlowLayout dropdown(String label, List<String> options, String current, Consumer<String> onSelect, int weight) {
        var layout = Containers.verticalFlow(Sizing.fill(weight), Sizing.content());
        layout.child(Components.label(Text.of(label)).color(Color.ofArgb(0xFFAAAAAA)));

        var btn = Components.button(Text.of(current), b -> {});
        btn.sizing(Sizing.fill(100), Sizing.fixed(20));

        btn.renderer((context, button, delta) -> {
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), 0xFF000000);
            int borderColor = button.isHovered() ? 0xFFFFFFFF : 0xFFA0A0A0;
            context.drawBorder(button.x(), button.y(), button.width(), button.height(), borderColor);

            int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(button.getMessage());
            int centerX = button.x() + (button.width() - textWidth) / 2;
            int centerY = button.y() + (button.height() - 8) / 2;

            context.drawText(MinecraftClient.getInstance().textRenderer, button.getMessage(),
                    centerX, centerY, 0xFFE0E0E0, false);
        });

        btn.onPress(b -> {
            if(!overlayLayer.children().isEmpty()) { closeOverlay(); return; }
            int itemHeight = 15; int maxItemsVisible = 8; int borderPadding = 2;
            int containerHeight = Math.min(options.size(), maxItemsVisible) * itemHeight + borderPadding;
            FlowLayout list = Containers.verticalFlow(Sizing.fixed(btn.width()), Sizing.content());
            for (String opt : options) {
                var optBtn = Components.button(Text.of(opt), ob -> {
                    btn.setMessage(Text.of(opt));
                    onSelect.accept(opt);
                    closeOverlay();
                });
                optBtn.sizing(Sizing.fill(100), Sizing.fixed(itemHeight));
                optBtn.renderer(ButtonComponent.Renderer.flat(0xFF000000, 0xFF444444, 0xFFA0A0A0));
                list.child(optBtn);
            }
            ScrollContainer<?> scroll = Containers.verticalScroll(Sizing.fixed(btn.width()), Sizing.fixed(containerHeight), list);
            scroll.surface((context, component) -> { context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0xFF222222); context.drawBorder(component.x(), component.y(), component.width(), component.height(), 0xFF000000); });
            scroll.positioning(Positioning.absolute(btn.x(), btn.y() + btn.height())); scroll.padding(Insets.of(1)); scroll.zIndex(600);
            openOverlay(scroll);
        });
        layout.child(btn);
        return layout;
    }

    private FlowLayout row(Component... children) {
        var layout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        layout.gap(5);
        layout.verticalAlignment(VerticalAlignment.TOP);
        for (Component c : children) layout.child(c);
        return layout;
    }

    private void addParentRow(String parentId) {
        parentIds.add(parentId);
        var row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(5);
        row.verticalAlignment(VerticalAlignment.TOP);
        row.margins(Insets.bottom(5));

        var parentField = autocompleteField("", parentId, availableSkills, s -> {
            int index = parentsContainer.children().indexOf(row);
            if (index >= 0 && index < parentIds.size()) { parentIds.set(index, s); updatePreview(); }
        }, 85);

        var removeBtn = Components.button(Text.of("X"), btn -> {
            int index = parentsContainer.children().indexOf(row);
            if (index >= 0 && index < parentIds.size()) { parentIds.remove(index); parentsContainer.removeChild(row); updatePreview(); }
        });
        removeBtn.sizing(Sizing.fill(15), Sizing.fixed(20));
        applyStandardButtonRenderer(removeBtn);

        row.child(parentField);
        row.child(removeBtn);
        parentsContainer.child(row);
    }

    // --- UPDATED ACTION ROW LOGIC ---

    private void addActionRow(ActionData data) {
        actions.add(data);
        var collapsible = Containers.collapsible(Sizing.fill(100), Sizing.content(), Text.of("Action"), true);
        var content = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        content.padding(Insets.of(5));

        // 1. Trigger Dropdown (Always Index 0)
        content.child(dropdown("Trigger", List.of("BLOCK_BREAK", "BLOCK_PLACE", "TIMER"), data.trigger, s -> {
            data.trigger = s;
            rebuildActionRow(collapsible, content, data);
            updatePreview();
        }, 100).margins(Insets.bottom(5)));

        // 2. Build Fields & Condition Editor
        buildActionFields(content, data, collapsible);

        collapsible.child(content);
        collapsible.margins(Insets.bottom(10));
        actionsContainer.child(collapsible);
    }

    private void buildActionFields(FlowLayout content, ActionData data, Component collapsible) {
        // Trigger Specifics
        if (data.trigger.equals("TIMER")) {
            content.child(field("Interval (Ticks)", data.interval, s -> { data.interval = s; updatePreview(); }, 100).margins(Insets.bottom(5)));
        }

        // Effect Type Selector
        content.child(dropdown("Effect Type", List.of("Command", "Burn"), data.effectType, s -> {
            data.effectType = s;
            rebuildActionRow(collapsible, content, data); // Rebuild to change specific fields
            updatePreview();
        }, 100).margins(Insets.bottom(5)));

        // Effect Fields
        if (data.effectType.equals("Command")) {
            content.child(field("Command", data.command, s -> { data.command = s; updatePreview(); }, 100));
        } else if (data.effectType.equals("Burn")) {
            content.child(field("Duration (Ticks)", data.burnDuration, s -> { data.burnDuration = s; updatePreview(); }, 100));
            var check = Components.checkbox(Text.of("Ignore Armor"));
            check.checked(data.burnIgnoreArmor);
            check.onChanged(checked -> { data.burnIgnoreArmor = checked; updatePreview(); });
            content.child(check.margins(Insets.top(5)));
        }

        // --- NEW: CONDITION EDITOR FOR ACTIONS ---
        content.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(5)));
        content.child(Components.label(Text.of("Activation Condition (Optional)")).color(Color.ofRgb(0xAAAAAA)));

        FlowLayout conditionContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        renderConditionEditor(conditionContainer, data.condition, (newCond) -> {
            data.condition = newCond;
            updatePreview();
        });
        content.child(conditionContainer);
        // -----------------------------------------

        // Remove Button
        var removeBtn = Components.button(Text.of("Remove Action"), btn -> {
            actions.remove(data);
            actionsContainer.removeChild(collapsible);
            updatePreview();
        });
        removeBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
        removeBtn.margins(Insets.top(5));
        removeBtn.renderer(ButtonComponent.Renderer.flat(0x00000000, 0xFF444444, 0xFF555555));
        content.child(removeBtn);
    }

    private void rebuildActionRow(Component collapsible, FlowLayout content, ActionData data) {
        var children = new ArrayList<>(content.children());
        // Remove everything AFTER the Trigger dropdown (index 0)
        for (int i = 1; i < children.size(); i++) {
            content.removeChild(children.get(i));
        }
        // Re-add fields and the condition editor
        buildActionFields(content, data, collapsible);
    }

    // --- END ACTION LOGIC ---

    private void addEffectRow(EffectData data) {
        effects.add(data);
        var collapsible = Containers.collapsible(Sizing.fill(100), Sizing.content(), Text.of("Effect"), true);
        var content = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        content.padding(Insets.of(5));

        content.child(dropdown("Type", List.of("Attribute", "Mining Speed", "Potion", "Enchantment", "Attack Knockback", "Experience", "Swim Speed", "Lava Speed"), data.type, s -> {
            data.type = s;
            rebuildEffectRow(collapsible, content, data);
            updatePreview();
        }, 100).margins(Insets.bottom(5)));

        buildEffectFields(content, data, collapsible);

        content.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(5)));
        content.child(Components.label(Text.of("Activation Condition (Optional)")).color(Color.ofRgb(0xAAAAAA)));

        FlowLayout conditionContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        renderConditionEditor(conditionContainer, data.condition, (newCond) -> {
            data.condition = newCond;
            updatePreview();
        });
        content.child(conditionContainer);

        collapsible.child(content);
        collapsible.margins(Insets.bottom(10));
        effectsContainer.child(collapsible);
    }

    private void renderConditionEditor(FlowLayout container, ConditionData currentData, Consumer<ConditionData> onUpdate) {
        container.clearChildren();

        // 1. If null, show the "Add" button
        if (currentData == null) {
            ButtonComponent addBtn = Components.button(Text.of("+ Add Condition"), btn -> {
                onUpdate.accept(new ConditionData()); // Create default
                renderConditionEditor(container, new ConditionData(), onUpdate); // Re-render as editor
            });
            addBtn.sizing(Sizing.fill(100), Sizing.fixed(16));
            addBtn.renderer(ButtonComponent.Renderer.flat(0x4400FF00, 0x8800FF00, 0xFF00FF00));
            container.child(addBtn);
            return;
        }

        // 2. We have data, so show the editor
        FlowLayout wrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        wrapper.surface(Surface.flat(0x33000000)).padding(Insets.of(3));

        if (currentData.isLogic()) {
            wrapper.surface((context, component) -> {
                context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0x33000000);
                context.drawBorder(component.x(), component.y(), component.width(), component.height(), 0xFF555555);
            });
        }

        // --- TYPE SELECTOR ---
        List<String> types = List.of(
                "AND", "OR", "NOT",
                "Hand Item", "Equipped Item",
                "Health", "Hunger", "Armor", "Sprinting", "Crouching", "Wetness", "In Lava",
                "Y-Level", "Time", "Dimension", "Walking On"
        );

        wrapper.child(dropdown("Cond Type", types, currentData.type, s -> {
            currentData.type = s;
            if(!currentData.isLogic()) {
                currentData.children.clear();
                currentData.child = null;
            }
            onUpdate.accept(currentData);
            renderConditionEditor(container, currentData, onUpdate);
        }, 100).margins(Insets.bottom(2)));

        // --- RENDER CHILDREN (Recursive) ---
        if (currentData.type.equals("AND") || currentData.type.equals("OR")) {
            FlowLayout childrenContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            childrenContainer.margins(Insets.left(8));

            for (int i = 0; i < currentData.children.size(); i++) {
                int idx = i;
                FlowLayout childRow = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
                childRow.margins(Insets.bottom(2));

                renderConditionEditor(childRow, currentData.children.get(i), (updated) -> {
                    if (updated == null) {
                        currentData.children.remove(idx);
                    } else {
                        currentData.children.set(idx, updated);
                    }
                    onUpdate.accept(currentData);
                    renderConditionEditor(container, currentData, onUpdate);
                });
                childrenContainer.child(childRow);
            }

            ButtonComponent addChild = Components.button(Text.of("+ Add Sub-Condition"), btn -> {
                currentData.children.add(new ConditionData());
                onUpdate.accept(currentData);
                renderConditionEditor(container, currentData, onUpdate);
            });
            addChild.sizing(Sizing.fill(100), Sizing.fixed(16));
            childrenContainer.child(addChild);
            wrapper.child(childrenContainer);
        }
        else if (currentData.type.equals("NOT")) {
            FlowLayout childRow = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            childRow.margins(Insets.left(8));

            renderConditionEditor(childRow, currentData.child, (updated) -> {
                currentData.child = updated;
                onUpdate.accept(currentData);
            });
            wrapper.child(childRow);
        }
        else {
            buildConditionFields(wrapper, currentData, () -> onUpdate.accept(currentData));
        }

        // --- REMOVE BUTTON (FIXED) ---
        ButtonComponent remove = Components.button(Text.of("Remove Condition"), btn -> {
            // 1. Update the data model to null
            onUpdate.accept(null);
            // 2. FIX: Immediately re-render THIS container with null data
            // to force it to switch back to the "+ Add Condition" button state.
            renderConditionEditor(container, null, onUpdate);
        });
        remove.renderer(ButtonComponent.Renderer.flat(0x00000000, 0x44FF0000, 0x88FF0000));
        remove.sizing(Sizing.fill(100), Sizing.fixed(14));
        remove.margins(Insets.top(2));
        wrapper.child(remove);

        container.child(wrapper);
    }

    // Moved specific field logic here
    private void buildConditionFields(FlowLayout content, ConditionData data, Runnable onUpdate) {

        // --- ITEM ---
        if (data.type.equals("Hand Item")) {
            List<String> items = Registries.ITEM.getIds().stream().map(Identifier::toString).sorted().toList();
            content.child(autocompleteField("Item", data.item, items, s -> { data.item = s; onUpdate.run(); }, 100));
            content.child(field("Min Count", data.count, s -> { data.count = s; onUpdate.run(); }, 100));
            content.child(dropdown("Slot", List.of("MAINHAND", "OFFHAND"), data.slot, s -> { data.slot = s; onUpdate.run(); }, 100));
            content.child(field("NBT", data.nbt, s -> { data.nbt = s; onUpdate.run(); }, 100));
        }
        else if (data.type.equals("Equipped Item")) {
            List<String> items = Registries.ITEM.getIds().stream().map(Identifier::toString).sorted().toList();
            content.child(autocompleteField("Item", data.item, items, s -> { data.item = s; onUpdate.run(); }, 100));
            content.child(dropdown("Slot", List.of("HELMET", "CHEST", "LEGS", "BOOTS"), data.slot, s -> { data.slot = s; onUpdate.run(); }, 100));
            content.child(field("NBT", data.nbt, s -> { data.nbt = s; onUpdate.run(); }, 100));
        }

        // --- PLAYER STATS ---
        else if (data.type.equals("Health")) {
            content.child(dropdown("Comp", List.of("GREATER_THAN", "LESS_THAN", "EQUAL_TO"), data.healthComparison, s -> { data.healthComparison = s; onUpdate.run(); }, 100));
            content.child(field("Value (20.0 = Max)", data.healthValue, s -> { data.healthValue = s; onUpdate.run(); }, 100));
        }
        else if (data.type.equals("Hunger")) {
            content.child(dropdown("Comp", List.of("GREATER_THAN", "LESS_THAN", "EQUAL_TO"), data.hungerComparison, s -> { data.hungerComparison = s; onUpdate.run(); }, 100));
            content.child(field("Value (20 = Full)", data.hungerValue, s -> { data.hungerValue = s; onUpdate.run(); }, 100));
        }
        else if (data.type.equals("Armor")) {
            content.child(dropdown("Comp", List.of("GREATER_THAN", "LESS_THAN", "EQUAL_TO"), data.armorComparison, s -> { data.armorComparison = s; onUpdate.run(); }, 100));
            content.child(field("Armor Points", data.armorValue, s -> { data.armorValue = s; onUpdate.run(); }, 100));
        }

        // --- WORLD ---
        else if (data.type.equals("Y-Level")) {
            content.child(dropdown("Comp", List.of("GREATER_THAN", "LESS_THAN", "EQUAL_TO"), data.yComparison, s -> { data.yComparison = s; onUpdate.run(); }, 100));
            content.child(field("Y Level", data.yValue, s -> { data.yValue = s; onUpdate.run(); }, 100));
        }
        else if (data.type.equals("Time")) {
            content.child(field("Min (0-24000)", data.timeMin, s -> { data.timeMin = s; onUpdate.run(); }, 100));
            content.child(field("Max (0-24000)", data.timeMax, s -> { data.timeMax = s; onUpdate.run(); }, 100));
        }
        else if (data.type.equals("Dimension")) {
            content.child(field("Dimension ID", data.dimension, s -> { data.dimension = s; onUpdate.run(); }, 100));
        }
        else if (data.type.equals("Walking On")) {
            List<String> blocks = Registries.BLOCK.getIds().stream().map(Identifier::toString).sorted().toList();
            content.child(autocompleteField("Block", data.walkingBlock, blocks, s -> { data.walkingBlock = s; onUpdate.run(); }, 100));
        }

        // --- BOOLEANS (No Fields Needed) ---
        else if (data.type.equals("Sprinting") || data.type.equals("Crouching") ||
                data.type.equals("Wetness") || data.type.equals("In Lava")) {
            // Just display a small helper text
            content.child(Components.label(Text.of("True if " + data.type)).color(Color.ofRgb(0x888888)));
        }
    }

    private void buildEffectFields(FlowLayout content, EffectData data, Component collapsible) {
        if (data.type.equals("Attribute")) {
            List<String> attrIds = Registries.ATTRIBUTE.getIds().stream().map(Identifier::toString).sorted().toList();
            content.child(autocompleteField("Attribute ID", data.attr, attrIds, s -> { data.attr = s; updatePreview(); }, 100));
            content.child(dropdown("Operation", List.of("ADDITION", "MULTIPLY_BASE", "MULTIPLY_TOTAL"), data.op, s -> { data.op = s; updatePreview(); }, 100).margins(Insets.top(5)));
            content.child(field("Value", data.val, s -> { data.val = s; updatePreview(); }, 100).margins(Insets.top(5)));
        }
        else if (data.type.equals("Mining Speed")) {
            content.child(field("Value (0.2 = +20%)", data.val, s -> { data.val = s; updatePreview(); }, 100));
        }
        else if (data.type.equals("Potion")) {
            List<String> effectIds = Registries.STATUS_EFFECT.getIds().stream().map(Identifier::toString).sorted().toList();
            content.child(autocompleteField("Effect ID", data.effectId, effectIds, s -> { data.effectId = s; updatePreview(); }, 100));
            content.child(field("Amplifier (0 = Level 1)", data.amplifier, s -> { data.amplifier = s; updatePreview(); }, 100).margins(Insets.top(5)));
            var checkbox = Components.checkbox(Text.of("Hide Particles"));
            checkbox.checked(data.hideParticles);
            checkbox.onChanged(checked -> { data.hideParticles = checked; updatePreview(); });
            checkbox.margins(Insets.top(5));
            content.child(checkbox);
        }
        else if (data.type.equals("Enchantment")) {
            List<String> enchIds = Registries.ENCHANTMENT.getIds().stream().map(Identifier::toString).sorted().toList();
            content.child(autocompleteField("Enchantment ID", data.enchId, enchIds, s -> { data.enchId = s; updatePreview(); }, 100));
            content.child(field("Level Added", data.enchLevel, s -> { data.enchLevel = s; updatePreview(); }, 100).margins(Insets.top(5)));
            content.child(dropdown("Slot", List.of("mainhand", "offhand", "helmet", "chest", "legs", "boots"), data.enchSlot, s -> {
                data.enchSlot = s;
                updatePreview();
            }, 100).margins(Insets.top(5)));
            var checkbox = Components.checkbox(Text.of("Allow Over-Enchanting"));
            checkbox.checked(data.overEnchant);
            checkbox.onChanged(checked -> { data.overEnchant = checked; updatePreview(); });
            checkbox.margins(Insets.top(5));
            content.child(checkbox);
        }
        else if (data.type.equals("Attack Knockback")) {
            content.child(field("Value (0.5 = Push, -1.0 = Pull)", data.knockbackValue, s -> { data.knockbackValue = s; updatePreview(); }, 100));
        }
        else if (data.type.equals("Experience")) {
            content.child(field("Multiplier (0.5 = +50% XP)", data.xpValue, s -> { data.xpValue = s; updatePreview(); }, 100));
        }
        else if (data.type.equals("Swim Speed")) {
            content.child(field("Multiplier (0.5 = +50% Speed)", data.swimValue, s -> { data.swimValue = s; updatePreview(); }, 100));
        }
        else if (data.type.equals("Lava Speed")) {
            content.child(field("Multiplier (0.5 = +50% Speed)", data.lavaValue, s -> { data.lavaValue = s; updatePreview(); }, 100));
        }

        var removeBtn = Components.button(Text.of("Remove"), btn -> {
            effects.remove(data);
            effectsContainer.removeChild(collapsible);
            updatePreview();
        });
        removeBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
        removeBtn.margins(Insets.top(10));
        applyStandardButtonRenderer(removeBtn);

        content.child(removeBtn);
    }

    private void rebuildEffectRow(Component collapsible, FlowLayout content, EffectData data) { var children = new ArrayList<>(content.children()); for (int i = 1; i < children.size(); i++) { content.removeChild(children.get(i)); } buildEffectFields(content, data, collapsible); }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { if (!overlayLayer.children().isEmpty()) { Component child = overlayLayer.children().get(0); boolean clickedDropdown = mouseX >= child.x() && mouseX <= child.x() + child.width() && mouseY >= child.y() && mouseY <= child.y() + child.height(); if (!clickedDropdown) { closeOverlay(); return true; } } return super.mouseClicked(mouseX, mouseY, button); }
    private void openOverlay(Component child) { overlayLayer.clearChildren(); overlayLayer.sizing(Sizing.fill(100), Sizing.fill(100)); overlayLayer.child(child); }
    private void closeOverlay() { overlayLayer.clearChildren(); overlayLayer.sizing(Sizing.fixed(0), Sizing.fixed(0)); }

    private String generateJson() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("name", name);
            root.addProperty("description", description);
            root.addProperty("icon", icon);
            if (!iconNbt.isEmpty()) root.addProperty("icon_nbt", iconNbt);
            root.addProperty("tier", tier);
            root.addProperty("cost", tryParse(cost));

            if (!parentIds.isEmpty()) {
                JsonArray parents = new JsonArray();
                for (String parentId : parentIds) { if (!parentId.trim().isEmpty()) { parents.add(parentId); } }
                if (!parents.isEmpty()) { root.add("prerequisites", parents); }
            }

            JsonArray effectsJson = new JsonArray();
            for (EffectData e : effects) {
                JsonObject eff = new JsonObject();
                if (e.type.equals("Attribute")) {
                    eff.addProperty("type", "jd_skill_tree:attribute");
                    eff.addProperty("attribute", e.attr);
                    eff.addProperty("operation", e.op);
                    eff.addProperty("value", Double.parseDouble(e.val));
                } else if (e.type.equals("Mining Speed")) {
                    eff.addProperty("type", "jd_skill_tree:mining_speed");
                    eff.addProperty("value", Double.parseDouble(e.val));
                } else if (e.type.equals("Potion")) {
                    eff.addProperty("type", "jd_skill_tree:potion");
                    eff.addProperty("effect", e.effectId);
                    eff.addProperty("amplifier", tryParse(e.amplifier));
                    eff.addProperty("hide_particles", e.hideParticles);
                } else if (e.type.equals("Enchantment")) {
                    eff.addProperty("type", "jd_skill_tree:enchantment");
                    eff.addProperty("enchantment", e.enchId);
                    eff.addProperty("level_added", tryParse(e.enchLevel));
                    eff.addProperty("slot", e.enchSlot);
                    eff.addProperty("over_enchant", e.overEnchant);
                } else if (e.type.equals("Attack Knockback")) {
                    eff.addProperty("type", "jd_skill_tree:attack_knockback");
                    try { eff.addProperty("value", Double.parseDouble(e.knockbackValue)); } catch (Exception ex) { eff.addProperty("value", 0.0); }
                } else if (e.type.equals("Experience")) {
                    eff.addProperty("type", "jd_skill_tree:experience");
                    try { eff.addProperty("value", Double.parseDouble(e.xpValue)); } catch (Exception ex) { eff.addProperty("value", 0.0); }
                } else if (e.type.equals("Swim Speed")) {
                    eff.addProperty("type", "jd_skill_tree:swim_speed");
                    try { eff.addProperty("value", Double.parseDouble(e.swimValue)); } catch (Exception ex) { eff.addProperty("value", 0.0); }
                } else if (e.type.equals("Lava Speed")) {
                    eff.addProperty("type", "jd_skill_tree:lava_speed");
                    try { eff.addProperty("value", Double.parseDouble(e.lavaValue)); } catch (Exception ex) { eff.addProperty("value", 0.0); }
                }

                if (e.condition != null) {
                    eff.add("condition", generateConditionJson(e.condition));
                }
                effectsJson.add(eff);
            }

            if (!effectsJson.isEmpty()) root.add("effects", effectsJson);

            // Actions
            JsonArray actionsJson = new JsonArray();
            for (ActionData a : actions) {
                JsonObject act = new JsonObject();
                // 1. Trigger
                act.addProperty("trigger", a.trigger);
                // 2. Interval (only for TIMER)
                if (a.trigger.equals("TIMER")) {
                    act.addProperty("interval", tryParse(a.interval));
                }
                // 3. Nested Effect Object
                JsonObject effectObj = new JsonObject();
                if (a.effectType.equals("Command")) {
                    effectObj.addProperty("type", "jd_skill_tree:command");
                    effectObj.addProperty("command", a.command);
                } else if (a.effectType.equals("Burn")) {
                    effectObj.addProperty("type", "jd_skill_tree:burn");
                    effectObj.addProperty("duration", tryParse(a.burnDuration));
                    effectObj.addProperty("ignore_armor", a.burnIgnoreArmor);
                }
                act.add("effect", effectObj);

                if (a.condition != null) {
                    act.add("condition", generateConditionJson(a.condition));
                }

                actionsJson.add(act);
            }
            if (!actionsJson.isEmpty()) root.add("actions", actionsJson);

            return GSON.toJson(root);
        } catch (Exception e) { return ""; }
    }

    private JsonObject generateConditionJson(ConditionData c) {
        JsonObject cond = new JsonObject();

        // --- LOGIC ---
        if (c.type.equals("AND")) {
            cond.addProperty("type", "jd_skill_tree:and");
            JsonArray children = new JsonArray();
            for (ConditionData child : c.children) children.add(generateConditionJson(child));
            cond.add("conditions", children);
        }
        else if (c.type.equals("OR")) {
            cond.addProperty("type", "jd_skill_tree:or");
            JsonArray children = new JsonArray();
            for (ConditionData child : c.children) children.add(generateConditionJson(child));
            cond.add("conditions", children);
        }
        else if (c.type.equals("NOT")) {
            cond.addProperty("type", "jd_skill_tree:not");
            if (c.child != null) cond.add("condition", generateConditionJson(c.child));
        }

        // --- ITEMS ---
        else if (c.type.equals("Hand Item")) {
            cond.addProperty("type", "jd_skill_tree:hand_item");
            cond.addProperty("item", c.item);
            cond.addProperty("count", tryParse(c.count));
            cond.addProperty("slot", c.slot.toLowerCase());
            if (!c.nbt.isEmpty()) cond.addProperty("nbt", c.nbt);
        }
        else if (c.type.equals("Equipped Item")) {
            cond.addProperty("type", "jd_skill_tree:equipped_item");
            cond.addProperty("item", c.item);
            String slotName = switch (c.slot) {
                case "BOOTS" -> "boots";
                case "LEGS" -> "legs";
                case "CHEST" -> "chest";
                default -> "helmet";
            };
            cond.addProperty("slot", slotName);
            if (!c.nbt.isEmpty()) cond.addProperty("nbt", c.nbt);
        }

        // --- PLAYER STATE ---
        else if (c.type.equals("Health")) {
            cond.addProperty("type", "jd_skill_tree:health");
            cond.addProperty("comparison", c.healthComparison);
            try { cond.addProperty("amount", Float.parseFloat(c.healthValue)); } catch(Exception e) { cond.addProperty("amount", 20.0f); }
        }
        else if (c.type.equals("Hunger")) {
            cond.addProperty("type", "jd_skill_tree:hunger");
            cond.addProperty("comparison", c.hungerComparison);
            cond.addProperty("amount", tryParse(c.hungerValue));
        }
        else if (c.type.equals("Armor")) {
            cond.addProperty("type", "jd_skill_tree:armor");
            cond.addProperty("comparison", c.armorComparison);
            cond.addProperty("amount", tryParse(c.armorValue));
        }
        else if (c.type.equals("Sprinting")) {
            cond.addProperty("type", "jd_skill_tree:sprinting");
        }
        else if (c.type.equals("Crouching")) {
            cond.addProperty("type", "jd_skill_tree:crouching");
        }
        else if (c.type.equals("Wetness")) {
            cond.addProperty("type", "jd_skill_tree:wetness");
        }
        else if (c.type.equals("In Lava")) {
            cond.addProperty("type", "jd_skill_tree:in_lava");
        }

        // --- WORLD ---
        else if (c.type.equals("Y-Level")) {
            cond.addProperty("type", "jd_skill_tree:y_level");
            cond.addProperty("comparison", c.yComparison);
            cond.addProperty("y_level", tryParse(c.yValue));
        }
        else if (c.type.equals("Time")) {
            cond.addProperty("type", "jd_skill_tree:time");
            cond.addProperty("min", tryParse(c.timeMin));
            cond.addProperty("max", tryParse(c.timeMax));
        }
        else if (c.type.equals("Dimension")) {
            cond.addProperty("type", "jd_skill_tree:dimension");
            cond.addProperty("dimension", c.dimension);
        }
        else if (c.type.equals("Walking On")) {
            cond.addProperty("type", "jd_skill_tree:walking_on");
            cond.addProperty("block", c.walkingBlock);
        }

        return cond;
    }

    private void updatePreview() { String json = generateJson(); if (json.isEmpty()) { exportTextDisplay.text(Text.of("Invalid JSON")); return; } exportTextDisplay.text(Text.of(json)); try { this.previewSkill = GSON.fromJson(json, Skill.class); this.previewSkill.setId(new Identifier("preview", "live")); } catch (Exception ignored) {} }
    private int tryParse(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
    private void loadAvailableSkills() { try { availableSkills = SkillManager.getAllSkills().stream().map(skill -> skill.getId().toString()).sorted().collect(Collectors.toList()); } catch (Exception e) { availableSkills = new ArrayList<>(); } }
    private void sendExportPacket(ButtonComponent btn) {
        String json = generateJson();
        if (json.isEmpty()) { return; }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(exportNamespace);
        buf.writeString(exportFileName);
        buf.writeString(json, 262144);
        ClientPlayNetworking.send(SkillNetworking.SAVE_SKILL_PACKET_ID, buf);
        btn.setMessage(Text.of("Request Sent"));
        new Thread(() -> { try { Thread.sleep(2000); MinecraftClient.getInstance().execute(() -> btn.setMessage(Text.of("Export to Server"))); } catch (InterruptedException ignored) {} }).start();
    }

    private class PreviewComponent extends BaseComponent {
        public PreviewComponent() { this.sizing(Sizing.fill(100), Sizing.fixed(65)); }
        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            if (previewSkill == null) return;
            int startX = this.x;
            int startY = this.y;
            if (previewWidget == null || previewWidget.getSkill() != previewSkill) { previewWidget = new SkillWidget(previewSkill, startX, startY, DeveloperEditorScreen.this); }
            previewWidget = new SkillWidget(previewSkill, startX, startY, DeveloperEditorScreen.this);
            previewWidget.render(context, 0, 0, mouseX, mouseY, delta);
            previewWidget.renderTooltip(context, startX + 36, startY + 30);
        }
    }
}