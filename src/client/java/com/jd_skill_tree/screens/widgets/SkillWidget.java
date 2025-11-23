package com.jd_skill_tree.screens.widgets;

import com.jd_skill_tree.skills.ClientSkillData;
import com.jd_skill_tree.skills.Skill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.List;

public class SkillWidget {

    public enum SkillState {
        LOCKED,
        CAN_UNLOCK,
        UNLOCKED
    }

    private static final Identifier WIDGETS_TEXTURE = new Identifier("jd_skill_tree", "textures/gui/widgets.png");
    private final Skill skill;
    private final int worldX;
    private final int worldY;
    private final MinecraftClient client;
    private final OrderedText title;
    private final List<OrderedText> description;

    public SkillWidget(Skill skill, int worldX, int worldY, Screen parentScreen) {
        this.skill = skill;
        this.worldX = worldX;
        this.worldY = worldY;
        this.client = MinecraftClient.getInstance();
        this.title = Text.of(skill.getName()).asOrderedText();
        this.description = this.client.textRenderer.wrapLines(Text.of(skill.getDescription()), 150);
    }

    public void render(DrawContext context, int panX, int panY, int mouseX, int mouseY, float delta) {
        int screenX = this.worldX + panX;
        int screenY = this.worldY + panY;

        RenderSystem.enableBlend();

        SkillState state = this.getState();

        int v;
        if (isMouseOver(panX, panY, mouseX, mouseY)) {
            v = 52; // Hover texture
        } else {
            switch (state) {
                case UNLOCKED:
                    v = 78;
                    break;
                case CAN_UNLOCK:
                    v = 0;
                    break;
                case LOCKED:
                default:
                    v = 26;
                    break;
            }
        }

        context.drawTexture(WIDGETS_TEXTURE, screenX, screenY, 0, v, 26, 26, 78, 104);
        context.drawItem(skill.getIcon(), screenX + 5, screenY + 5);
    }

    public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 28;
        int tooltipWidth = 160;
        int tooltipHeight = 8 + (description.size() * 10) + 12;

        // Define the boundaries of the main box
        int boxX1 = tooltipX - 3;
        int boxY1 = tooltipY - 3;
        int boxX2 = tooltipX + tooltipWidth + 3;
        int boxY2 = tooltipY + tooltipHeight + 3;

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 400);

        // 1. Draw Standard Background (Dark Translucent Black)
        int backgroundColor = -267386864; // 0xF0100010
        context.fillGradient(boxX1, boxY1 - 1, boxX2, boxY1, backgroundColor, backgroundColor);
        context.fillGradient(boxX1, boxY2, boxX2, boxY2 + 1, backgroundColor, backgroundColor);
        context.fillGradient(boxX1, boxY1, boxX2, boxY2, backgroundColor, backgroundColor);
        context.fillGradient(boxX1 - 1, boxY1, boxX1, boxY2, backgroundColor, backgroundColor);
        context.fillGradient(boxX2, boxY1, boxX2 + 1, boxY2, backgroundColor, backgroundColor);

        // 2. Determine Colors based on Tier
        int r = 255, g = 255, b = 255;
        int glowBaseAlpha = 0x50; // Standard glow opacity

        switch (this.skill.getTier()) {
            case 1: // Grey
                r = 170; g = 170; b = 170;
                break;
            case 2: // Green
                r = 85; g = 255; b = 85;
                break;
            case 3: // Blue
                r = 85; g = 85; b = 255;
                break;
            case 4: // Orange
                r = 255; g = 170; b = 0;
                break;
            case 5: // Aggressive Red
                double time = System.currentTimeMillis() * 0.005;
                double sine = Math.sin(time); // -1.0 to 1.0

                // Aggressive Red Formula
                int redIntensity = (int)(230 + (sine * 25));
                r = Math.max(0, Math.min(255, redIntensity));
                g = 0;
                b = 0;

                // Higher opacity for Tier 5 to make it "pop"
                glowBaseAlpha = 0xA0;
                break;
        }

        // 3. Draw OUTWARD Glow
        int glowSize = 10; // Distance the glow radiates outwards

        // Loop from 1 to glowSize to draw concentric rings moving outwards
        for (int i = 1; i <= glowSize; i++) {
            // Calculate fading alpha (Strongest near box, 0 at edge)
            float fade = 1.0f - ((float)i / glowSize);
            // Apply a curve so it doesn't fade too linearly (optional, looks nicer)
            fade = fade * fade;

            int currentAlpha = (int)(glowBaseAlpha * fade);
            int fadeColor = (currentAlpha << 24) | (r << 16) | (g << 8) | b;

            // Coordinates for the current ring
            int x1 = boxX1 - i;
            int y1 = boxY1 - i;
            int x2 = boxX2 + i;
            int y2 = boxY2 + i;

            // Draw the 4 sides of the ring
            // Top Line (Full Width)
            context.fill(x1, y1, x2, y1 + 1, fadeColor);
            // Bottom Line (Full Width)
            context.fill(x1, y2 - 1, x2, y2, fadeColor);
            // Left Line (Between Top and Bottom)
            context.fill(x1, y1 + 1, x1 + 1, y2 - 1, fadeColor);
            // Right Line (Between Top and Bottom)
            context.fill(x2 - 1, y1 + 1, x2, y2 - 1, fadeColor);
        }

        // 4. Draw 1px Solid Borders (Over the inner-most part of the glow)
        int fullAlpha = 0xFF;
        int borderColor = (fullAlpha << 24) | (r << 16) | (g << 8) | b;

        context.fill(boxX1, boxY1, boxX2, boxY1 + 1, borderColor); // Top
        context.fill(boxX1, boxY2 - 1, boxX2, boxY2, borderColor); // Bottom
        context.fill(boxX1, boxY1, boxX1 + 1, boxY2, borderColor); // Left
        context.fill(boxX2 - 1, boxY1, boxX2, boxY2, borderColor); // Right

        // 5. Draw Text
        // Title (Colored)
        context.drawText(this.client.textRenderer, this.title, tooltipX, tooltipY, borderColor, true);

        int descY = tooltipY + 12;
        for (OrderedText line : this.description) {
            context.drawText(this.client.textRenderer, line, tooltipX, descY, -1, true);
            descY += 10;
        }

        Text costText = Text.of("Cost: " + skill.getCost());
        SkillState state = getState();
        int costColor = 0xFF5555;
        if (state == SkillState.UNLOCKED) {
            costColor = 0x66FF55;
        } else if (client.player.experienceLevel >= skill.getCost()) {
            costColor = 0xFFFF55;
        }

        context.drawText(this.client.textRenderer, costText, tooltipX, descY, costColor, true);

        context.getMatrices().pop();
    }

    public SkillState getState() {
        assert this.client.player != null;
        if (ClientSkillData.isSkillUnlocked(this.skill)) return SkillState.UNLOCKED;

        boolean hasEnoughLevels = this.client.player.experienceLevel >= this.skill.getCost();
        List<Skill> requiredSkills = this.skill.getRequiredSkills();
        boolean hasAllRequiredSkills = true;
        for (Skill requiredSkill : requiredSkills) {
            if (!ClientSkillData.isSkillUnlocked(requiredSkill)) {
                hasAllRequiredSkills = false;
                break;
            }
        }

        if (hasEnoughLevels && hasAllRequiredSkills) return SkillState.CAN_UNLOCK;
        return SkillState.LOCKED;
    }

    public Skill getSkill() { return this.skill; }
    public int getWorldX() { return this.worldX; }
    public int getWorldY() { return this.worldY; }
    public boolean isMouseOver(int panX, int panY, int mouseX, int mouseY) {
        int screenX = this.worldX + panX;
        int screenY = this.worldY + panY;
        return mouseX >= screenX && mouseX <= screenX + 26 && mouseY >= screenY && mouseY <= screenY + 26;
    }
    public boolean mouseClicked(double mouseX, double mouseY, int button, int panX, int panY) {
        return isMouseOver(panX, panY, (int)mouseX, (int)mouseY);
    }
}