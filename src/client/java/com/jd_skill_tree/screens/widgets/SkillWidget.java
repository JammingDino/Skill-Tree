package com.jd_skill_tree.screens.widgets;

import com.jd_skill_tree.skills.ClientSkillData;
import com.jd_skill_tree.skills.Skill;
import com.mojang.blaze3d.systems.RenderSystem;
import com.jd_skill_tree.utils.ExperienceUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.List;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

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

        int boxX1 = tooltipX - 3;
        int boxY1 = tooltipY - 3;
        int boxX2 = tooltipX + tooltipWidth + 3;
        int boxY2 = tooltipY + tooltipHeight + 3;

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 400);

        // 1. Draw Background
        int backgroundColor = -267386864; // 0xF0100010
        context.fillGradient(boxX1, boxY1 - 1, boxX2, boxY1, backgroundColor, backgroundColor);
        context.fillGradient(boxX1, boxY2, boxX2, boxY2 + 1, backgroundColor, backgroundColor);
        context.fillGradient(boxX1, boxY1, boxX2, boxY2, backgroundColor, backgroundColor);
        context.fillGradient(boxX1 - 1, boxY1, boxX1, boxY2, backgroundColor, backgroundColor);
        context.fillGradient(boxX2, boxY1, boxX2 + 1, boxY2, backgroundColor, backgroundColor);

        // 2. Borders & Title Color
        int titleColor;

        if (this.skill.getTier() == 5) {
            // --- TIER 5: DYNAMIC GLOW ---

            // TIMING FIX:
            // We use a 4-second period.
            // We calculate normalized progress (0.0 -> 1.0) and multiply by 2*PI.
            // This ensures end-state matches start-state perfectly.
            long period = 4000L;
            float progress = (System.currentTimeMillis() % period) / (float) period;
            float phase = progress * (float) (Math.PI * 2);

            drawDynamicBorder(context, boxX1, boxY1, boxX2 - boxX1, boxY2 - boxY1, phase, 6);
            titleColor = getHummingPurpleGold(phase);
        } else {
            // --- TIERS 1-4: SUBTLE GLOW ---
            int baseColor;
            switch (this.skill.getTier()) {
                case 2 -> baseColor = 0xFF55FF55; // Green
                case 3 -> baseColor = 0xFF5555FF; // Blue
                case 4 -> baseColor = 0xFFFFAA00; // Gold
                default -> baseColor = 0xFFAAAAAA; // Grey
            }

            drawStaticGlow(context, boxX1, boxY1, boxX2 - boxX1, boxY2 - boxY1, baseColor, 4);
            titleColor = baseColor;
        }

        // 3. Draw Text
        context.drawText(this.client.textRenderer, this.title, tooltipX, tooltipY, titleColor, true);

        int descY = tooltipY + 12;
        for (OrderedText line : this.description) {
            context.drawText(this.client.textRenderer, line, tooltipX, descY, -1, true);
            descY += 10;
        }

        Text costText = Text.of("Cost: " + skill.getCost() + " XP");
        SkillState state = getState();
        int costColor = 0xFF5555; // Red

        if (state == SkillState.UNLOCKED) {
            costColor = 0x66FF55; // Green
        } else {
            int currentXp = ExperienceUtils.getPlayerTotalXp(this.client.player);
            if (currentXp >= skill.getCost()) {
                costColor = 0xFFFF55; // Yellow
            }
        }

        context.drawText(this.client.textRenderer, costText, tooltipX, descY, costColor, true);

        context.getMatrices().pop();
    }

    private void drawStaticGlow(DrawContext context, int x, int y, int w, int h, int color, int glowSize) {
        int fadeColor = color & 0x00FFFFFF;

        // Sides
        drawGlowQuad(context, x, y - glowSize, w, glowSize, fadeColor, fadeColor, color, color);
        drawGlowQuad(context, x, y + h, w, glowSize, color, color, fadeColor, fadeColor);
        drawGlowQuad(context, x - glowSize, y, glowSize, h, fadeColor, color, color, fadeColor);
        drawGlowQuad(context, x + w, y, glowSize, h, color, fadeColor, fadeColor, color);

        // Corners (Symmetric Logic)
        // TL: Inner (BR) is Solid
        drawGlowQuad(context, x - glowSize, y - glowSize, glowSize, glowSize, fadeColor, fadeColor, color, fadeColor);
        // TR: Inner (BL) is Solid
        drawGlowQuad(context, x + w, y - glowSize, glowSize, glowSize, fadeColor, fadeColor, fadeColor, color);
        // BR: Inner (TL) is Solid
        drawGlowQuad(context, x + w, y + h, glowSize, glowSize, color, fadeColor, fadeColor, fadeColor);
        // BL: Inner (TR) is Solid
        drawGlowQuad(context, x - glowSize, y + h, glowSize, glowSize, fadeColor, color, fadeColor, fadeColor);

        // 1px Hard Border
        context.drawBorder(x, y, w, h, color);
    }

    private void drawDynamicBorder(DrawContext context, int x, int y, int w, int h, float phase, int glowSize) {
        int cTL = getHummingPurpleGold(phase);
        int cTR = getHummingPurpleGold(phase + (float)(Math.PI / 2));
        int cBR = getHummingPurpleGold(phase + (float)(Math.PI));
        int cBL = getHummingPurpleGold(phase + (float)(Math.PI * 1.5));

        int cTL_fade = cTL & 0x00FFFFFF;
        int cTR_fade = cTR & 0x00FFFFFF;
        int cBR_fade = cBR & 0x00FFFFFF;
        int cBL_fade = cBL & 0x00FFFFFF;

        // Sides
        drawGlowQuad(context, x, y - glowSize, w, glowSize, cTL_fade, cTR_fade, cTR, cTL);
        drawGlowQuad(context, x, y + h, w, glowSize, cBL, cBR, cBR_fade, cBL_fade);
        drawGlowQuad(context, x - glowSize, y, glowSize, h, cTL_fade, cTL, cBL, cBL_fade);
        drawGlowQuad(context, x + w, y, glowSize, h, cTR, cTR_fade, cBR_fade, cBR);

        // Corners (Auto-detected smooth rendering)
        drawGlowQuad(context, x - glowSize, y - glowSize, glowSize, glowSize, cTL_fade, cTL_fade, cTL, cTL_fade);
        drawGlowQuad(context, x + w, y - glowSize, glowSize, glowSize, cTR_fade, cTR_fade, cTR_fade, cTR);
        drawGlowQuad(context, x + w, y + h, glowSize, glowSize, cBR, cBR_fade, cBR_fade, cBR_fade);
        drawGlowQuad(context, x - glowSize, y + h, glowSize, glowSize, cBL_fade, cBL, cBL_fade, cBL_fade);

        // Hard Border
        drawGlowQuad(context, x, y, w, 1, cTL, cTR, cTR, cTL);
        drawGlowQuad(context, x + w - 1, y, 1, h, cTR, cTR, cBR, cBR);
        drawGlowQuad(context, x, y + h - 1, w, 1, cBL, cBR, cBR, cBL);
        drawGlowQuad(context, x, y, 1, h, cTL, cTL, cBL, cBL);
    }

    private void drawGlowQuad(DrawContext context, int x, int y, int w, int h, int cTL, int cTR, int cBR, int cBL) {
        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        // Extract Alphas first to decide triangulation
        float aTL = (float)(cTL >> 24 & 255) / 255.0F;
        float aTR = (float)(cTR >> 24 & 255) / 255.0F;
        float aBR = (float)(cBR >> 24 & 255) / 255.0F;
        float aBL = (float)(cBL >> 24 & 255) / 255.0F;

        // Auto-Fix: Determine which diagonal carries the "weight" of the gradient.
        // If the TR-BL diagonal is brighter than the TL-BR diagonal, we flip the
        // internal triangulation (drawing TL->BR->BL->TR) to avoid a hard crease.
        boolean flip = (aTR + aBL) > (aTL + aBR);

        // Extract remaining components
        float rTL = (float)(cTL >> 16 & 255) / 255.0F; float gTL = (float)(cTL >> 8 & 255) / 255.0F; float bTL = (float)(cTL & 255) / 255.0F;
        float rTR = (float)(cTR >> 16 & 255) / 255.0F; float gTR = (float)(cTR >> 8 & 255) / 255.0F; float bTR = (float)(cTR & 255) / 255.0F;
        float rBR = (float)(cBR >> 16 & 255) / 255.0F; float gBR = (float)(cBR >> 8 & 255) / 255.0F; float bBR = (float)(cBR & 255) / 255.0F;
        float rBL = (float)(cBL >> 16 & 255) / 255.0F; float gBL = (float)(cBL >> 8 & 255) / 255.0F; float bBL = (float)(cBL & 255) / 255.0F;

        if (flip) {
            // Flipped Order: Forces the GPU to cut the quad from Top-Left to Bottom-Right
            // This prevents the "Hard Edge" when the gradient source is TR or BL
            vertexConsumer.vertex(matrix, x, y, 0).color(rTL, gTL, bTL, aTL).next();             // TL
            vertexConsumer.vertex(matrix, x, y + h, 0).color(rBL, gBL, bBL, aBL).next();         // BL
            vertexConsumer.vertex(matrix, x + w, y + h, 0).color(rBR, gBR, bBR, aBR).next();     // BR
            vertexConsumer.vertex(matrix, x + w, y, 0).color(rTR, gTR, bTR, aTR).next();         // TR
        } else {
            // Standard Order: GPU cuts from Bottom-Left to Top-Right
            vertexConsumer.vertex(matrix, x, y + h, 0).color(rBL, gBL, bBL, aBL).next();         // BL
            vertexConsumer.vertex(matrix, x + w, y + h, 0).color(rBR, gBR, bBR, aBR).next();     // BR
            vertexConsumer.vertex(matrix, x + w, y, 0).color(rTR, gTR, bTR, aTR).next();         // TR
            vertexConsumer.vertex(matrix, x, y, 0).color(rTL, gTL, bTL, aTL).next();             // TL
        }
    }

    private int getHummingPurpleGold(float phase) {
        float t = (MathHelper.sin(phase) + 1.0f) * 0.5f;
        int r = (int) (160 + (255 - 160) * t);
        int g = (int) (32 + (215 - 32) * t);
        int b = (int) (240 + (0 - 240) * t);
        // Alpha must be 255 (0xFF) so we can bitwise AND it later to make it transparent
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public SkillState getState() {
        assert this.client.player != null;
        if (ClientSkillData.isSkillUnlocked(this.skill)) return SkillState.UNLOCKED;

        // UPDATED CHECK: Compare Total XP Points vs Cost
        int currentXp = ExperienceUtils.getPlayerTotalXp(this.client.player);
        boolean hasEnoughXp = currentXp >= this.skill.getCost();

        List<Skill> requiredSkills = this.skill.getRequiredSkills();
        boolean hasAllRequiredSkills = true;
        for (Skill requiredSkill : requiredSkills) {
            if (!ClientSkillData.isSkillUnlocked(requiredSkill)) {
                hasAllRequiredSkills = false;
                break;
            }
        }

        if (hasEnoughXp && hasAllRequiredSkills) return SkillState.CAN_UNLOCK;
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