package com.jd_skill_tree.screens;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.screens.widgets.SkillWidget;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillManager;
import com.jd_skill_tree.skills.effects.SkillEffectListAdapter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DeveloperEditorScreen extends BaseOwoScreen<StackLayout> {

    // --- State ---
    private String name = "New Skill";
    private String cost = "5";
    private String icon = "minecraft:apple";
    private String description = "Description...";
    private final List<String> parentIds = new ArrayList<>();

    private int tier = 1;

    private static class EffectData {
        String type = "Attribute";
        String attr = "minecraft:generic.max_health";
        String op = "ADDITION";
        String val = "1.0";
    }
    private final List<EffectData> effects = new ArrayList<>();

    private SkillWidget previewWidget;
    private Skill previewSkill;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new com.jd_skill_tree.skills.IdentifierAdapter())
            .registerTypeAdapter(new com.google.common.reflect.TypeToken<java.util.List<com.jd_skill_tree.skills.effects.SkillEffect>>(){}.getType(), new SkillEffectListAdapter())
            .setPrettyPrinting()
            .create();

    // --- UI Variables ---
    private FlowLayout effectsContainer;
    private FlowLayout parentsContainer;
    private FlowLayout overlayLayer;
    private LabelComponent exportTextDisplay;
    private List<String> availableSkills = new ArrayList<>();

    // Export settings
    private String exportNamespace = "my_skills";
    private String exportFileName = "new_skill";

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::stack);
    }

    @Override
    protected void build(StackLayout root) {
        // Populate available skills from the skill registry
        loadAvailableSkills();

        root.surface(Surface.VANILLA_TRANSLUCENT);

        // Main Horizontal Split with GAP
        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        mainContent.gap(10); // Spacing between panels
        mainContent.padding(Insets.of(5)); // External padding

        // ==========================================================================================
        // LEFT COLUMN: EDITOR (63% Width)
        // ==========================================================================================
        FlowLayout editorContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.surface(Surface.DARK_PANEL).padding(Insets.of(10));

        editorContent.child(Components.label(Text.of("Skill Editor")).shadow(true).margins(Insets.bottom(15)));

        // -- Row 1: Name (50%), Cost (20%), Tier (30%) --
        editorContent.child(row(
                field("Name", name, s -> { name = s; updatePreview(); }, 50),
                field("Cost", cost, s -> { cost = s; updatePreview(); }, 20),
                dropdown("Tier", List.of("1", "2", "3", "4", "5"), String.valueOf(tier), s -> {
                    tier = Integer.parseInt(s);
                    updatePreview();
                }, 30)
        ));

        List<String> itemIds = Registries.ITEM.getIds().stream().map(Identifier::toString).sorted().toList();

        // -- Row 2: Icon (100%) --
        editorContent.child(
                autocompleteField("Icon (Item ID)", icon, itemIds, s -> { icon = s; updatePreview(); }, 100)
        );

        // -- Row 3: Description (100%) --
        editorContent.child(field("Description", description, s -> { description = s; updatePreview(); }, 100));

        // -- Divider --
        editorContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(10)));

        // -- Parents Section --
        FlowLayout parentsHeader = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        parentsHeader.child(Components.label(Text.of("Prerequisites")).color(Color.ofRgb(0x55FFFF)));
        parentsHeader.margins(Insets.bottom(5));
        editorContent.child(parentsHeader);

        parentsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.child(parentsContainer);

        editorContent.child(Components.button(Text.of("+ Add Parent Skill"), btn -> {
            addParentRow("");
            updatePreview();
        }).sizing(Sizing.fill(100), Sizing.fixed(20)).margins(Insets.bottom(5)));

        // -- Divider --
        editorContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(10)));

        // -- Effects Section --
        editorContent.child(Components.label(Text.of("Passive Effects")).color(Color.ofRgb(0x55FFFF)).margins(Insets.bottom(5)));

        effectsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.child(effectsContainer);

        editorContent.child(Components.button(Text.of("+ Add Effect"), btn -> {
            addEffectRow(new EffectData());
            updatePreview();
        }).sizing(Sizing.fill(100), Sizing.fixed(20)));

        editorContent.child(Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(50)));

        var editorScroll = Containers.verticalScroll(Sizing.fill(63), Sizing.fill(100), editorContent);
        mainContent.child(editorScroll);


        // ==========================================================================================
        // RIGHT COLUMN: SIDEBAR (35% Width)
        // ==========================================================================================
        FlowLayout sidebar = Containers.verticalFlow(Sizing.fill(35), Sizing.fill(100));
        sidebar.surface(Surface.DARK_PANEL).padding(Insets.of(10));
        sidebar.alignment(HorizontalAlignment.LEFT, VerticalAlignment.TOP);

        // 1. Preview Component
        sidebar.child(Components.label(Text.of("Preview")).shadow(true).margins(Insets.bottom(5)));

        // CHANGED: Reduced margin from 15 to 5
        sidebar.child(new PreviewComponent().margins(Insets.bottom(5)));

        // 2. Export Settings
        // CHANGED: Reduced margin from 10 to 5
        sidebar.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.bottom(5)));
        sidebar.child(Components.label(Text.of("Export Settings")).shadow(true).margins(Insets.bottom(5)));

        sidebar.child(field("Namespace", exportNamespace, s -> exportNamespace = s, 100));
        sidebar.child(field("File Name", exportFileName, s -> exportFileName = s, 100));

        // 3. Action Buttons
        FlowLayout buttonsRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonsRow.gap(5);
        buttonsRow.margins(Insets.bottom(10));

        ButtonComponent exportBtn = Components.button(Text.of("Export to Server"), btn -> sendExportPacket(btn));
        exportBtn.sizing(Sizing.fill(50), Sizing.fixed(20));

        ButtonComponent copyBtn = Components.button(Text.of("Copy JSON"), btn -> {
            String json = generateJson();
            if (!json.isEmpty()) {
                MinecraftClient.getInstance().keyboard.setClipboard(json);
                btn.setMessage(Text.of("Copied!"));
                new Thread(() -> {
                    try { Thread.sleep(2000); MinecraftClient.getInstance().execute(() -> btn.setMessage(Text.of("Copy JSON"))); }
                    catch (InterruptedException ignored) {}
                }).start();
            }
        });
        copyBtn.sizing(Sizing.fill(50), Sizing.fixed(20));

        buttonsRow.child(exportBtn);
        buttonsRow.child(copyBtn);
        sidebar.child(buttonsRow);

        // 4. JSON Display
        sidebar.child(Components.label(Text.of("JSON Output")).color(Color.ofRgb(0xAAAAAA)).margins(Insets.bottom(2)));

        FlowLayout jsonDisplayContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        jsonDisplayContainer.surface((context, component) -> {
            context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0xFF000000);
            context.drawBorder(component.x(), component.y(), component.width(), component.height(), 0xFF333333);
        }).padding(Insets.of(5));

        exportTextDisplay = Components.label(Text.of(""));
        exportTextDisplay.maxWidth(Integer.MAX_VALUE);
        exportTextDisplay.color(Color.ofRgb(0x00FF00));

        var jsonContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).child(exportTextDisplay);
        var jsonScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), jsonContent);

        jsonDisplayContainer.child(jsonScroll);
        sidebar.child(jsonDisplayContainer);

        mainContent.child(sidebar);
        root.child(mainContent);

        // --- OVERLAY LAYER ---
        overlayLayer = Containers.verticalFlow(Sizing.fixed(0), Sizing.fixed(0));
        overlayLayer.positioning(Positioning.absolute(0, 0));
        overlayLayer.zIndex(500);
        overlayLayer.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (overlayLayer.children().size() > 0) {
                Component child = overlayLayer.children().get(0);
                boolean clickedDropdown = mouseX >= child.x() && mouseX <= child.x() + child.width()
                        && mouseY >= child.y() && mouseY <= child.y() + child.height();

                if (!clickedDropdown) {
                    closeOverlay();
                    return true;
                }
            }
            return false;
        });
        root.child(overlayLayer);

        updatePreview();
    }

    // --- WIDGET GENERATORS ---
    // (Standard methods included without changes for context)
    private FlowLayout field(String label, String value, Consumer<String> onChange, int weight) {
        var layout = Containers.verticalFlow(Sizing.fill(weight), Sizing.content());
        layout.margins(Insets.bottom(5));
        layout.child(Components.label(Text.of(label)).color(Color.ofArgb(0xFFAAAAAA)));
        var box = Components.textBox(Sizing.fill(100));
        box.verticalSizing(Sizing.fixed(20));
        box.setText(value);
        box.setCursorToStart();
        box.setMaxLength(Integer.MAX_VALUE);
        box.onChanged().subscribe(onChange::accept);
        layout.child(box);
        return layout;
    }

    private FlowLayout autocompleteField(String label, String value, List<String> options, Consumer<String> onChange, int weight) {
        var layout = Containers.verticalFlow(Sizing.fill(weight), Sizing.content());
        layout.margins(Insets.bottom(5));
        layout.child(Components.label(Text.of(label)).color(Color.ofArgb(0xFFAAAAAA)));
        var box = Components.textBox(Sizing.fill(100));
        box.verticalSizing(Sizing.fixed(20));
        box.setText(value);
        box.setCursorToStart();
        box.setMaxLength(Integer.MAX_VALUE);
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
        box.keyPress().subscribe((code, scan, mod) -> { if (code == GLFW.GLFW_KEY_TAB) { if (overlayLayer.children().size() > 0) { Component child = overlayLayer.children().get(0); if (child instanceof ScrollContainer<?> scroll) { if (scroll.child() instanceof FlowLayout list && list.children().size() > 0) { Component firstBtn = list.children().get(0); if(firstBtn instanceof ButtonComponent btn) { selectItem.accept(btn.getMessage().getString()); return true; } } } } } return false; });
        layout.child(box);
        return layout;
    }

    private FlowLayout dropdown(String label, List<String> options, String current, Consumer<String> onSelect, int weight) {
        var layout = Containers.verticalFlow(Sizing.fill(weight), Sizing.content());
        layout.child(Components.label(Text.of(label)).color(Color.ofArgb(0xFFAAAAAA)));
        var btn = Components.button(Text.of(current), b -> {});
        btn.sizing(Sizing.fill(100), Sizing.fixed(20));
        btn.onPress(b -> {
            if(overlayLayer.children().size() > 0) { closeOverlay(); return; }
            int itemHeight = 15; int maxItemsVisible = 8; int borderPadding = 2;
            int containerHeight = Math.min(options.size(), maxItemsVisible) * itemHeight + borderPadding;
            FlowLayout list = Containers.verticalFlow(Sizing.fixed(btn.width()), Sizing.content());
            for (String opt : options) {
                var optBtn = Components.button(Text.of(opt), ob -> { btn.setMessage(Text.of(opt)); onSelect.accept(opt); closeOverlay(); });
                optBtn.sizing(Sizing.fill(100), Sizing.fixed(itemHeight));
                optBtn.renderer(ButtonComponent.Renderer.flat(0x00000000, 0xFF444444, 0xFF555555));
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

    private FlowLayout row(Component... children) { var layout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content()); layout.gap(5); for (Component c : children) layout.child(c); return layout; }
    private void addParentRow(String parentId) { parentIds.add(parentId); var row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content()); row.gap(5); row.margins(Insets.bottom(5)); var parentField = autocompleteField("", parentId, availableSkills, s -> { int index = parentsContainer.children().indexOf(row); if (index >= 0 && index < parentIds.size()) { parentIds.set(index, s); updatePreview(); } }, 85); var removeBtn = Components.button(Text.of("X"), btn -> { int index = parentsContainer.children().indexOf(row); if (index >= 0 && index < parentIds.size()) { parentIds.remove(index); parentsContainer.removeChild(row); updatePreview(); } }); removeBtn.sizing(Sizing.fill(15), Sizing.fixed(20)); row.child(parentField); row.child(removeBtn); parentsContainer.child(row); }
    private void addEffectRow(EffectData data) { effects.add(data); var collapsible = Containers.collapsible(Sizing.fill(100), Sizing.content(), Text.of("Effect"), true); var content = Containers.verticalFlow(Sizing.fill(100), Sizing.content()); content.padding(Insets.of(5)); content.child(dropdown("Type", List.of("Attribute", "Mining Speed"), data.type, s -> { data.type = s; rebuildEffectRow(collapsible, content, data); updatePreview(); }, 100).margins(Insets.bottom(5))); buildEffectFields(content, data, collapsible); collapsible.child(content); collapsible.margins(Insets.bottom(10)); effectsContainer.child(collapsible); }
    private void buildEffectFields(FlowLayout content, EffectData data, Component collapsible) { if (data.type.equals("Attribute")) { List<String> attrIds = Registries.ATTRIBUTE.getIds().stream().map(Identifier::toString).sorted().toList(); content.child(autocompleteField("Attribute ID", data.attr, attrIds, s -> { data.attr = s; updatePreview(); }, 100)); content.child(dropdown("Operation", List.of("ADDITION", "MULTIPLY_BASE", "MULTIPLY_TOTAL"), data.op, s -> { data.op = s; updatePreview(); }, 100).margins(Insets.top(5))); content.child(field("Value", data.val, s -> { data.val = s; updatePreview(); }, 100).margins(Insets.top(5))); } else if (data.type.equals("Mining Speed")) { content.child(field("Value", data.val, s -> { data.val = s; updatePreview(); }, 100)); } content.child(Components.button(Text.of("Remove"), btn -> { effects.remove(data); effectsContainer.removeChild(collapsible); updatePreview(); }).sizing(Sizing.fill(100), Sizing.fixed(20)).margins(Insets.top(10))); }
    private void rebuildEffectRow(Component collapsible, FlowLayout content, EffectData data) { var children = new ArrayList<>(content.children()); for (int i = 1; i < children.size(); i++) { content.removeChild(children.get(i)); } buildEffectFields(content, data, collapsible); }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { if (overlayLayer.children().size() > 0) { Component child = overlayLayer.children().get(0); boolean clickedDropdown = mouseX >= child.x() && mouseX <= child.x() + child.width() && mouseY >= child.y() && mouseY <= child.y() + child.height(); if (!clickedDropdown) { closeOverlay(); return true; } } return super.mouseClicked(mouseX, mouseY, button); }
    private void openOverlay(Component child) { overlayLayer.clearChildren(); overlayLayer.sizing(Sizing.fill(100), Sizing.fill(100)); overlayLayer.child(child); }
    private void closeOverlay() { overlayLayer.clearChildren(); overlayLayer.sizing(Sizing.fixed(0), Sizing.fixed(0)); }
    private String generateJson() { try { JsonObject root = new JsonObject(); root.addProperty("name", name); root.addProperty("description", description); root.addProperty("icon", icon); root.addProperty("tier", tier); root.addProperty("cost", tryParse(cost)); if (!parentIds.isEmpty()) { JsonArray parents = new JsonArray(); for (String parentId : parentIds) { if (!parentId.trim().isEmpty()) { parents.add(parentId); } } if (parents.size() > 0) { root.add("prerequisites", parents); } } JsonArray effectsJson = new JsonArray(); for (EffectData e : effects) { JsonObject eff = new JsonObject(); if (e.type.equals("Attribute")) { eff.addProperty("type", "jd_skill_tree:attribute"); eff.addProperty("attribute", e.attr); eff.addProperty("operation", e.op); eff.addProperty("value", Double.parseDouble(e.val)); } else { eff.addProperty("type", "jd_skill_tree:mining_speed"); eff.addProperty("value", Double.parseDouble(e.val)); } effectsJson.add(eff); } if (effectsJson.size() > 0) root.add("effects", effectsJson); return GSON.toJson(root); } catch (Exception e) { return ""; } }
    private void updatePreview() { String json = generateJson(); if (json.isEmpty()) { exportTextDisplay.text(Text.of("Invalid JSON")); return; } exportTextDisplay.text(Text.of(json)); try { this.previewSkill = GSON.fromJson(json, Skill.class); this.previewSkill.setId(new Identifier("preview", "live")); } catch (Exception ignored) {} }
    private int tryParse(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
    private void loadAvailableSkills() { try { availableSkills = SkillManager.getAllSkills().stream().map(skill -> skill.getId().toString()).sorted().collect(Collectors.toList()); } catch (Exception e) { availableSkills = new ArrayList<>(); } }
    private void sendExportPacket(ButtonComponent btn) { String json = generateJson(); if (json.isEmpty()) { return; } PacketByteBuf buf = PacketByteBufs.create(); buf.writeString(exportNamespace); buf.writeString(exportFileName); buf.writeString(json); ClientPlayNetworking.send(SkillNetworking.SAVE_SKILL_PACKET_ID, buf); btn.setMessage(Text.of("Request Sent")); new Thread(() -> { try { Thread.sleep(2000); MinecraftClient.getInstance().execute(() -> btn.setMessage(Text.of("Export to Server"))); } catch (InterruptedException ignored) {} }).start(); }

    private class PreviewComponent extends BaseComponent {
        public PreviewComponent() {
            // CHANGED: Reduced height from 120 to 65.
            // This should remove the large empty gap while still allowing 2-3 lines of text.
            this.sizing(Sizing.fill(100), Sizing.fixed(65));
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            if (previewSkill == null) return;

            // Anchor to top-left of this component
            int startX = this.x;
            int startY = this.y;

            if (previewWidget == null || previewWidget.getSkill() != previewSkill) {
                previewWidget = new SkillWidget(previewSkill, startX, startY, DeveloperEditorScreen.this);
            }

            // Render widget at (0, 0) relative to this component
            previewWidget = new SkillWidget(previewSkill, startX, startY, DeveloperEditorScreen.this);
            previewWidget.render(context, 0, 0, mouseX, mouseY, delta);

            // Render tooltip immediately to the right of the icon (26px width + 10px padding)
            // Adjust +30 based on typical tooltip rendering offset to align with the top.
            previewWidget.renderTooltip(context, startX + 36, startY + 30);
        }
    }
}