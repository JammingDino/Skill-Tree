package com.jd_skill_tree.screens;

import com.jd_skill_tree.Jd_skill_tree;
import com.jd_skill_tree.screens.widgets.SkillWidget;
import com.jd_skill_tree.skills.ModSkills;
import com.jd_skill_tree.skills.Skill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltarScreen extends Screen {

    // ... (Constants remain the same)
    private static final Identifier WINDOW_TEXTURE = new Identifier("textures/gui/advancements/window.png");
    private static final String ALTAR_TEXT = "Skill Altar Tier ";
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 140;

    private static int tier = 0;
    public static int x = 0;
    public static int y = 0;

    private final List<SkillWidget> skillWidgets = new ArrayList<>();
    private final Map<String, SkillWidget> skillWidgetMap = new HashMap<>();

    // --- NEW PANNING AND BOUNDARY VARIABLES ---
    private double panX = 0;
    private double panY = 0;
    // Bounding box of the entire skill tree
    private int treeMinX, treeMinY, treeMaxX, treeMaxY;

    public AltarScreen(Text title, int Tier) {
        super(title);
        tier = Tier;
    }

    @Override
    protected void init() {
        this.skillWidgets.clear();
        this.skillWidgetMap.clear();

        List<Skill> skillsToDisplay = ModSkills.getSkillsForTier(tier);

        // Position skills in their "world space"
        int startX = this.width / 2 - (skillsToDisplay.size() * 30 / 2);
        int startY = this.height / 2 - 13;

        for (int i = 0; i < skillsToDisplay.size(); i++) {
            Skill skill = skillsToDisplay.get(i);
            // We now pass the world coordinates to the widget
            SkillWidget widget = new SkillWidget(skill, startX + (i * 30), startY, this);
            this.skillWidgets.add(widget);
            this.skillWidgetMap.put(skill.getId(), widget);
        }

        // --- CALCULATE TREE BOUNDARIES ---
        calculateTreeBounds();

        // --- CENTER THE INITIAL VIEW ---
        // Center the view on the tree's content
        int treeWidth = this.treeMaxX - this.treeMinX;
        int treeHeight = this.treeMaxY - this.treeMinY;
        // The static `x` and `y` are the top-left of the GUI window
        x = (this.width - WINDOW_WIDTH) / 2;
        y = (this.height - WINDOW_HEIGHT) / 2;
        // Set pan to center the tree within the window
        this.panX = (x + WINDOW_WIDTH / 2.0) - (this.treeMinX + treeWidth / 2.0);
        this.panY = (y + WINDOW_HEIGHT / 2.0) - (this.treeMinY + treeHeight / 2.0);

        // Ensure the initial pan is within the allowed limits
        clampPan();
    }

    private void calculateTreeBounds() {
        if (skillWidgets.isEmpty()) {
            return;
        }
        // Initialize with the position of the first widget
        this.treeMinX = skillWidgets.get(0).getWorldX();
        this.treeMinY = skillWidgets.get(0).getWorldY();
        this.treeMaxX = skillWidgets.get(0).getWorldX() + 26;
        this.treeMaxY = skillWidgets.get(0).getWorldY() + 26;

        // Expand the bounds for all other widgets
        for (SkillWidget widget : skillWidgets) {
            this.treeMinX = Math.max(this.treeMinX, widget.getWorldX());
            this.treeMinY = Math.max(this.treeMinY, widget.getWorldY());
            this.treeMaxX = Math.min(this.treeMaxX, widget.getWorldX() + 26);
            this.treeMaxY = Math.min(this.treeMaxY, widget.getWorldY() + 26);
        }
    }

    // --- MOUSE EVENT HANDLING FOR PANNING ---
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // We only pan with the left mouse button (button 0)
        if (button == 0) {
            this.panX += deltaX;
            this.panY += deltaY;
            clampPan();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (SkillWidget widget : this.skillWidgets) {
            // First check if the widget was clicked
            if (widget.mouseClicked(mouseX, mouseY, button, (int)this.panX, (int)this.panY)) {

                if (widget.getSkill().getId().equals("reset_skills")) {
                    // Only allow reset if the player actually has skills to reset
                    if (widget.getState() == SkillWidget.SkillState.CAN_UNLOCK) {
                        sendResetRequest();
                        assert this.client != null;
                        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0F));
                        return true;
                    }
                }

                else if (widget.getState() == SkillWidget.SkillState.CAN_UNLOCK) {
                    // This is where you send the packet to the server!
                    sendUnlockRequest(widget.getSkill());

                    // Play a click sound for immediate user feedback
                    assert this.client != null;
                    this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Creates and sends the C2S packet to request a skill unlock.
     * @param skill The skill the player is trying to unlock.
     */
    private void sendUnlockRequest(Skill skill) {
        System.out.println("CLIENT: Sending unlock request for skill: " + skill.getId());

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(skill.getId());
        ClientPlayNetworking.send(Jd_skill_tree.UNLOCK_SKILL_PACKET_ID, buf);
    }

    private void sendResetRequest() {
        System.out.println("CLIENT: Sending skill reset request.");
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(Jd_skill_tree.RESET_SKILLS_PACKET_ID, buf);
    }

    // --- PANNING BOUNDARY LOGIC ---
    private void clampPan() {
        // Define the visible area inside the GUI window frame
        int viewX1 = x + 5;
        int viewY1 = y + 18; // Give space for title
        int viewX2 = x + WINDOW_WIDTH - 5;
        int viewY2 = y + WINDOW_HEIGHT - 5;

        int treeWidth = this.treeMaxX - this.treeMinX;
        int viewWidth = viewX2 - viewX1;

        // If the tree is wider than the view, prevent it from being moved out of sight
        if (treeWidth > viewWidth) {
            this.panX = Math.max(this.panX, viewX2 - this.treeMaxX); // Lower bound
            this.panX = Math.min(this.panX, viewX1 - this.treeMinX); // Upper bound
        } else {
            // If the tree is narrower, keep it within the view
            this.panX = Math.max(this.panX, viewX1 - this.treeMinX); // Lower bound
            this.panX = Math.min(this.panX, viewX2 - this.treeMaxX); // Upper bound
        }

        // Repeat the same logic for the Y-axis
        int treeHeight = this.treeMaxY - this.treeMinY;
        int viewHeight = viewY2 - viewY1;

        if (treeHeight > viewHeight) {
            this.panY = Math.max(this.panY, viewY2 - this.treeMaxY);
            this.panY = Math.min(this.panY, viewY1 - this.treeMinY);
        } else {
            this.panY = Math.max(this.panY, viewY1 - this.treeMinY);
            this.panY = Math.min(this.panY, viewY2 - this.treeMaxY);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        x = (this.width - WINDOW_WIDTH) / 2;
        y = (this.height - WINDOW_HEIGHT) / 2;

        this.renderBackground(context);
        this.drawWindow(context, x, y);

        // We need to clip rendering so skills and lines don't draw outside the window.
        context.enableScissor(x + 10, y + 2, x + WINDOW_WIDTH - 10, y + WINDOW_HEIGHT - 10);

        drawSkillLines(context);

        for (SkillWidget widget : this.skillWidgets) {
            // Pass pan values to the widget's render method
            widget.render(context, (int)this.panX, (int)this.panY, mouseX, mouseY, delta);
        }

        context.disableScissor();

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSkillLines(DrawContext context) {
        int lineColor = 0xFF808080;
        for (SkillWidget widget : this.skillWidgets) {
            widget.getSkill().getRequiredSkill().ifPresent(requiredSkill -> {
                SkillWidget requiredWidget = skillWidgetMap.get(requiredSkill.getId());
                if (requiredWidget != null) {
                    // Add pan offset to all coordinates when drawing lines
                    int startX = widget.getWorldX() + 13 + (int)this.panX;
                    int startY = widget.getWorldY() + 13 + (int)this.panY;
                    int endX = requiredWidget.getWorldX() + 13 + (int)this.panX;
                    int endY = requiredWidget.getWorldY() + 13 + (int)this.panY;

                    // Using your diagonal line drawer. It needs to be made static or moved.
                    // For now, assuming it is available as a static helper:
                    AltarScreen.drawLine(context, startX, startY, endX, endY, lineColor);
                }
            });
        }
    }

    // Make sure your drawLine method is available and static if it's in this class
    public static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // ... (Bresenham's algorithm implementation from the previous answer)
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;
        int err = dx - dy;
        while (true) {
            context.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    // ... (The rest of your AltarScreen methods: drawWindow, renderBackground, etc. remain the same)
    public void drawWindow(DrawContext context, int x, int y) {
        RenderSystem.enableBlend();
        context.drawTexture(WINDOW_TEXTURE, x, y, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        context.drawText(this.textRenderer, ALTAR_TEXT + tier, x + 8, y + 6, 4210752, false);
    }

    @Override
    public void renderBackground(DrawContext context) {
        assert this.client != null;
        if (this.client.world != null) {
            context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        } else {
            this.renderBackgroundTexture(context);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private SkillWidget findWidgetForSkill(Skill skill) {
        return this.skillWidgetMap.get(skill.getId());
    }
}