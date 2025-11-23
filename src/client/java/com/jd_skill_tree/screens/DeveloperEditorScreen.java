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

        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        mainContent.gap(0);

        // --- EDITOR PANEL (30%) ---
        FlowLayout editorContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.surface(Surface.DARK_PANEL).padding(Insets.of(10));

        editorContent.child(Components.label(Text.of("Developer Console")).shadow(true).margins(Insets.bottom(10)));

        // Inputs
        editorContent.child(row(
                field("Name", name, s -> { name = s; updatePreview(); }, 63),
                field("Cost", cost, s -> { cost = s; updatePreview(); }, 32)
        ));

        List<String> itemIds = Registries.ITEM.getIds().stream().map(Identifier::toString).sorted().toList();
        editorContent.child(row(
                autocompleteField("Icon", icon, itemIds, s -> { icon = s; updatePreview(); }, 63),
                dropdown("Tier", List.of("1", "2", "3", "4", "5"), String.valueOf(tier), s -> {
                    tier = Integer.parseInt(s);
                    updatePreview();
                }, 32)
        ));

        editorContent.child(field("Description", description, s -> { description = s; updatePreview(); }, 100));

        editorContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(10)));
        editorContent.child(Components.label(Text.of("Parents")).color(Color.ofRgb(0x55FFFF)).margins(Insets.bottom(5)));

        // Parents Container
        parentsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.child(parentsContainer);

        editorContent.child(Components.button(Text.of("+ Add Parent"), btn -> {
            addParentRow("");
            updatePreview();
        }).sizing(Sizing.fill(100), Sizing.fixed(20)));

        editorContent.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).color(Color.ofArgb(0xFF555555)).margins(Insets.vertical(10)));
        editorContent.child(Components.label(Text.of("Effects")).color(Color.ofRgb(0x55FFFF)).margins(Insets.bottom(5)));

        // Effects Container
        effectsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        editorContent.child(effectsContainer);

        editorContent.child(Components.button(Text.of("+ Add Effect"), btn -> {
            addEffectRow(new EffectData());
            updatePreview();
        }).sizing(Sizing.fill(100), Sizing.fixed(20)));

        // Scroll Container (30% width)
        var scrollContainer = Containers.verticalScroll(Sizing.fill(30), Sizing.fill(100), editorContent);
        mainContent.child(scrollContainer);

        // --- PREVIEW PANEL (40%) ---
        FlowLayout previewPanel = Containers.verticalFlow(Sizing.fill(40), Sizing.fill(100));
        previewPanel.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

        previewPanel.child(new PreviewComponent());

        mainContent.child(previewPanel);

        // --- EXPORT PANEL (30%) ---
        FlowLayout exportPanel = Containers.verticalFlow(Sizing.fill(30), Sizing.fill(100));
        exportPanel.surface(Surface.DARK_PANEL).padding(Insets.of(10));

        exportPanel.child(Components.label(Text.of("Export")).shadow(true).margins(Insets.bottom(10)));

        // Export settings
        exportPanel.child(field("Namespace", exportNamespace, s -> exportNamespace = s, 100));
        exportPanel.child(field("File Name", exportFileName, s -> exportFileName = s, 100));

        // --- BUTTONS ROW (Above JSON) ---
        FlowLayout buttonsRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonsRow.gap(5);
        buttonsRow.margins(Insets.bottom(5));

        // 1. Export Button
        ButtonComponent exportBtn = Components.button(Text.of("Export to Server"), btn -> {
            sendExportPacket(btn);
        });
        exportBtn.sizing(Sizing.fill(50), Sizing.fixed(20));
        buttonsRow.child(exportBtn);

        // 2. Copy Button
        ButtonComponent copyBtn = Components.button(Text.of("Copy JSON"), btn -> {
            String json = generateJson();
            if (!json.isEmpty()) {
                MinecraftClient.getInstance().keyboard.setClipboard(json);
                btn.setMessage(Text.of("Copied!"));
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        MinecraftClient.getInstance().execute(() -> btn.setMessage(Text.of("Copy JSON")));
                    } catch (InterruptedException ignored) {}
                }).start();
            }
        });
        copyBtn.sizing(Sizing.fill(50), Sizing.fixed(20));
        buttonsRow.child(copyBtn);

        exportPanel.child(buttonsRow);

        // --- JSON DISPLAY AREA (Fills Remaining Space) ---
        FlowLayout jsonDisplayContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        jsonDisplayContainer.surface(Surface.PANEL).padding(Insets.of(5));

        exportTextDisplay = Components.label(Text.of(""));
        exportTextDisplay.maxWidth(Integer.MAX_VALUE); // Allow wrapping
        exportTextDisplay.color(Color.BLACK);

        var jsonContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .child(exportTextDisplay);

        var jsonScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), jsonContent);

        jsonDisplayContainer.child(jsonScroll);

        // Ensure the JSON area fills the remaining height of the export panel
        exportPanel.child(jsonDisplayContainer);

        mainContent.child(exportPanel);

        root.child(mainContent);

        // --- OVERLAY LAYER ---
        overlayLayer = Containers.verticalFlow(Sizing.fixed(0), Sizing.fixed(0));
        overlayLayer.positioning(Positioning.absolute(0, 0));
        overlayLayer.zIndex(500);
        overlayLayer.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (overlayLayer.children().size() > 0) {
                boolean clickedDropdown = overlayLayer.children().stream()
                        .anyMatch(c -> mouseX >= c.x() && mouseX <= c.x() + c.width() && mouseY >= c.y() && mouseY <= c.y() + c.height());

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

    // --- WIDGET GENERATORS (Unchanged) ---
    private FlowLayout field(String label, String value, Consumer<String> onChange, int weight) {
        var layout = Containers.verticalFlow(Sizing.fill(weight), Sizing.content());
        layout.margins(Insets.bottom(5));
        layout.child(Components.label(Text.of(label)).color(Color.ofArgb(0xFFAAAAAA)));

        var box = Components.textBox(Sizing.fill(100));
        box.verticalSizing(Sizing.fixed(20));
        box.setText(value);
        box.setCursorToStart();
        box.setMaxLength(1000);
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

        Consumer<String> selectItem = (match) -> {
            box.setText(match);
            onChange.accept(match);
            closeOverlay();
        };

        box.onChanged().subscribe(text -> {
            onChange.accept(text);
            closeOverlay();

            if (text.isEmpty()) return;

            List<String> matches = options.stream()
                    .filter(s -> s.contains(text.toLowerCase()))
                    .sorted((a, b) -> {
                        boolean aStart = a.startsWith(text);
                        boolean bStart = b.startsWith(text);
                        if (aStart && !bStart) return -1;
                        if (!aStart && bStart) return 1;
                        return a.compareTo(b);
                    })
                    .limit(8)
                    .toList();

            if (matches.isEmpty()) return;

            FlowLayout suggestions = Containers.verticalFlow(Sizing.fixed(box.width()), Sizing.content());

            suggestions.surface((context, component) -> {
                context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0xFF222222);
                context.drawBorder(component.x(), component.y(), component.width(), component.height(), 0xFF000000);
            });

            suggestions.padding(Insets.of(1));
            suggestions.positioning(Positioning.absolute(box.x(), box.y() + box.height()));
            suggestions.zIndex(600);

            for (String match : matches) {
                var itemBtn = Components.button(Text.of(match), btn -> selectItem.accept(match));
                itemBtn.sizing(Sizing.fill(100), Sizing.fixed(15));
                itemBtn.renderer(ButtonComponent.Renderer.flat(0x00000000, 0xFF444444, 0xFF555555));
                suggestions.child(itemBtn);
            }

            openOverlay(suggestions);
        });

        box.keyPress().subscribe((code, scan, mod) -> {
            if (code == GLFW.GLFW_KEY_TAB) {
                if (overlayLayer.children().size() > 0) {
                    Component child = overlayLayer.children().get(0);
                    if (child instanceof FlowLayout list && list.children().size() > 0) {
                        Component firstBtn = list.children().get(0);
                        if(firstBtn instanceof ButtonComponent btn) {
                            String suggestion = btn.getMessage().getString();
                            selectItem.accept(suggestion);
                            return true;
                        }
                    }
                }
            }
            return false;
        });

        layout.child(box);
        return layout;
    }

    private FlowLayout dropdown(String label, List<String> options, String current, Consumer<String> onSelect, int weight) {
        var layout = Containers.verticalFlow(Sizing.fill(weight), Sizing.content());
        layout.child(Components.label(Text.of(label)).color(Color.ofArgb(0xFFAAAAAA)));

        var btn = Components.button(Text.of(current), b -> {});
        btn.sizing(Sizing.fill(100), Sizing.fixed(20));

        btn.onPress(b -> {
            if(overlayLayer.children().size() > 0) {
                closeOverlay();
                return;
            }

            FlowLayout list = Containers.verticalFlow(Sizing.fixed(btn.width()), Sizing.content());

            list.surface((context, component) -> {
                context.fill(component.x(), component.y(), component.x() + component.width(), component.y() + component.height(), 0xFF222222);
                context.drawBorder(component.x(), component.y(), component.width(), component.height(), 0xFF000000);
            });

            list.positioning(Positioning.absolute(btn.x(), btn.y() + btn.height()));
            list.padding(Insets.of(1));
            list.zIndex(600);

            for (String opt : options) {
                var optBtn = Components.button(Text.of(opt), ob -> {
                    btn.setMessage(Text.of(opt));
                    onSelect.accept(opt);
                    closeOverlay();
                });
                optBtn.sizing(Sizing.fill(100), Sizing.fixed(15));
                optBtn.renderer(ButtonComponent.Renderer.flat(0x00000000, 0xFF444444, 0xFF555555));
                list.child(optBtn);
            }

            openOverlay(list);
        });

        layout.child(btn);
        return layout;
    }

    private FlowLayout row(Component... children) {
        var layout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        layout.gap(5);
        for (Component c : children) layout.child(c);
        return layout;
    }

    private void addParentRow(String parentId) {
        parentIds.add(parentId);

        var row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(5);
        row.margins(Insets.bottom(5));

        var parentField = autocompleteField("", parentId, availableSkills, s -> {
            int index = parentsContainer.children().indexOf(row);
            if (index >= 0 && index < parentIds.size()) {
                parentIds.set(index, s);
                updatePreview();
            }
        }, 85);

        var removeBtn = Components.button(Text.of("X"), btn -> {
            int index = parentsContainer.children().indexOf(row);
            if (index >= 0 && index < parentIds.size()) {
                parentIds.remove(index);
                parentsContainer.removeChild(row);
                updatePreview();
            }
        });
        removeBtn.sizing(Sizing.fill(15), Sizing.fixed(20));

        row.child(parentField);
        row.child(removeBtn);

        parentsContainer.child(row);
    }

    private void addEffectRow(EffectData data) {
        effects.add(data);

        var collapsible = Containers.collapsible(
                Sizing.fill(100), Sizing.content(),
                Text.of("Effect"), true
        );

        var content = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        content.padding(Insets.of(5));

        content.child(dropdown("Type", List.of("Attribute", "Mining Speed"), data.type, s -> {
            data.type = s;
            rebuildEffectRow(collapsible, content, data);
            updatePreview();
        }, 100).margins(Insets.bottom(5)));

        buildEffectFields(content, data, collapsible);

        collapsible.child(content);
        collapsible.margins(Insets.bottom(10));

        effectsContainer.child(collapsible);
    }

    private void buildEffectFields(FlowLayout content, EffectData data, Component collapsible) {
        if (data.type.equals("Attribute")) {
            List<String> attrIds = Registries.ATTRIBUTE.getIds().stream().map(Identifier::toString).sorted().toList();
            content.child(autocompleteField("Attribute ID", data.attr, attrIds, s -> {
                data.attr = s;
                updatePreview();
            }, 100));

            content.child(dropdown("Operation", List.of("ADDITION", "MULTIPLY_BASE", "MULTIPLY_TOTAL"), data.op, s -> {
                data.op = s;
                updatePreview();
            }, 100).margins(Insets.top(5)));

            content.child(field("Value", data.val, s -> {
                data.val = s;
                updatePreview();
            }, 100).margins(Insets.top(5)));
        } else if (data.type.equals("Mining Speed")) {
            content.child(field("Value", data.val, s -> {
                data.val = s;
                updatePreview();
            }, 100));
        }

        content.child(Components.button(Text.of("Remove"), btn -> {
            effects.remove(data);
            effectsContainer.removeChild(collapsible);
            updatePreview();
        }).sizing(Sizing.fill(100), Sizing.fixed(20)).margins(Insets.top(10)));
    }

    private void rebuildEffectRow(Component collapsible, FlowLayout content, EffectData data) {
        var children = new ArrayList<>(content.children());
        for (int i = 1; i < children.size(); i++) {
            content.removeChild(children.get(i));
        }
        buildEffectFields(content, data, collapsible);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overlayLayer.children().size() > 0) {
            boolean clickedDropdown = overlayLayer.children().stream()
                    .anyMatch(c -> mouseX >= c.x() && mouseX <= c.x() + c.width() && mouseY >= c.y() && mouseY <= c.y() + c.height());

            if (!clickedDropdown) {
                closeOverlay();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openOverlay(Component child) {
        overlayLayer.clearChildren();
        overlayLayer.sizing(Sizing.fill(100), Sizing.fill(100));
        overlayLayer.child(child);
    }

    private void closeOverlay() {
        overlayLayer.clearChildren();
        overlayLayer.sizing(Sizing.fixed(0), Sizing.fixed(0));
    }

    private String generateJson() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("name", name);
            root.addProperty("description", description);
            root.addProperty("icon", icon);
            root.addProperty("tier", tier);
            root.addProperty("cost", tryParse(cost));

            if (!parentIds.isEmpty()) {
                JsonArray parents = new JsonArray();
                for (String parentId : parentIds) {
                    if (!parentId.trim().isEmpty()) {
                        parents.add(parentId);
                    }
                }
                if (parents.size() > 0) {
                    root.add("prerequisites", parents);
                }
            }

            JsonArray effectsJson = new JsonArray();
            for (EffectData e : effects) {
                JsonObject eff = new JsonObject();
                if (e.type.equals("Attribute")) {
                    eff.addProperty("type", "jd_skill_tree:attribute");
                    eff.addProperty("attribute", e.attr);
                    eff.addProperty("operation", e.op);
                    eff.addProperty("value", Double.parseDouble(e.val));
                } else {
                    eff.addProperty("type", "jd_skill_tree:mining_speed");
                    eff.addProperty("value", Double.parseDouble(e.val));
                }
                effectsJson.add(eff);
            }
            if (effectsJson.size() > 0) root.add("effects", effectsJson);

            return GSON.toJson(root);
        } catch (Exception e) {
            return "";
        }
    }

    private void updatePreview() {
        String json = generateJson();
        if (json.isEmpty()) {
            exportTextDisplay.text(Text.of("Invalid JSON"));
            return;
        }

        exportTextDisplay.text(Text.of(json));

        try {
            this.previewSkill = GSON.fromJson(json, Skill.class);
            this.previewSkill.setId(new Identifier("preview", "live"));
        } catch (Exception ignored) {}
    }

    private int tryParse(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }

    private void loadAvailableSkills() {
        try {
            availableSkills = SkillManager.getAllSkills().stream()
                    .map(skill -> skill.getId().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            availableSkills = new ArrayList<>();
        }
    }

    // --- REPLACED EXPORT LOGIC ---
    private void sendExportPacket(ButtonComponent btn) {
        String json = generateJson();
        if (json.isEmpty()) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(exportNamespace);
        buf.writeString(exportFileName);
        buf.writeString(json);

        ClientPlayNetworking.send(SkillNetworking.SAVE_SKILL_PACKET_ID, buf);

        btn.setMessage(Text.of("Request Sent"));
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                MinecraftClient.getInstance().execute(() -> btn.setMessage(Text.of("Export to Server")));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private class PreviewComponent extends BaseComponent {
        public PreviewComponent() {
            this.sizing(Sizing.fixed(200), Sizing.fixed(200));
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            if (previewSkill == null) return;
            int cx = this.x + this.width / 2 - 13;
            int cy = this.y + this.height / 2 - 13;

            if (previewWidget == null || previewWidget.getSkill() != previewSkill) {
                previewWidget = new SkillWidget(previewSkill, cx, cy, DeveloperEditorScreen.this);
            }

            int size = 80;
            context.fill(cx + 13 - size, cy + 13 - size, cx + 13 + size, cy + 13 + size, 0x80000000);
            context.drawBorder(cx + 13 - size, cy + 13 - size, size * 2, size * 2, 0xFFFFFFFF);

            previewWidget = new SkillWidget(previewSkill, cx, cy, DeveloperEditorScreen.this);
            previewWidget.render(context, 0, 0, mouseX, mouseY, delta);
            previewWidget.renderTooltip(context, cx + 13, cy + 30);
        }
    }
}