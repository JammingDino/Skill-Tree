package com.jd_skill_tree.screens.widgets;

import com.jd_skill_tree.skills.ClientSkillData;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.utils.ExperienceUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

public class SkillWidget {

    public enum SkillState {
        LOCKED,
        CAN_UNLOCK,
        UNLOCKED
    }

    private static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation("jd_skill_tree", "textures/gui/widgets.png");
    private final Skill skill;
    private final int worldX;
    private final int worldY;
    private final Minecraft client;
    private final FormattedCharSequence title;
    private final List<FormattedCharSequence> description;

    public SkillWidget(Skill skill, int worldX, int worldY, Screen parentScreen) {
        this.skill = skill;
        this.worldX = worldX;
        this.worldY = worldY;
        this.client = Minecraft.getInstance();
        this.title = Component.literal(skill.getName()).getVisualOrderText();
        this.description = this.client.font.split(Component.literal(skill.getDescription()), 150);
    }

    public void render(GuiGraphics guiGraphics, int panX, int panY, int mouseX, int mouseY, float delta) {
        int screenX = this.worldX + panX;
        int screenY = this.worldY + panY;

        RenderSystem.enableBlend();

        SkillState state = this.getState();

        int v;
        if (isMouseOver(panX, panY, mouseX, mouseY)) {
            v = 52;
        } else {
            v = switch (state) {
                case UNLOCKED -> 78;
                case CAN_UNLOCK -> 0;
                default -> 26;
            };
        }

        guiGraphics.blit(WIDGETS_TEXTURE, screenX, screenY, 0, v, 26, 26, 78, 104);
        guiGraphics.renderItem(skill.getIcon(), screenX + 5, screenY + 5);
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Font font = this.client.font;
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 28;
        int tooltipWidth = 160;
        int tooltipHeight = 8 + (description.size() * 10) + 12;

        int boxX1 = tooltipX - 3;
        int boxY1 = tooltipY - 3;
        int boxX2 = tooltipX + tooltipWidth + 3;
        int boxY2 = tooltipY + tooltipHeight + 3;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);

        int backgroundColor = -267386864;
        guiGraphics.fillGradient(boxX1, boxY1 - 1, boxX2, boxY1, backgroundColor, backgroundColor);
        guiGraphics.fillGradient(boxX1, boxY2, boxX2, boxY2 + 1, backgroundColor, backgroundColor);
        guiGraphics.fillGradient(boxX1, boxY1, boxX2, boxY2, backgroundColor, backgroundColor);
        guiGraphics.fillGradient(boxX1 - 1, boxY1, boxX1, boxY2, backgroundColor, backgroundColor);
        guiGraphics.fillGradient(boxX2, boxY1, boxX2 + 1, boxY2, backgroundColor, backgroundColor);

        int titleColor;
        if (this.skill.getTier() == 5) {
            long period = 4000L;
            float progress = (System.currentTimeMillis() % period) / (float) period;
            float phase = progress * (float) (Math.PI * 2);
            drawDynamicBorder(guiGraphics, boxX1, boxY1, boxX2 - boxX1, boxY2 - boxY1, phase, 6);
            titleColor = getHummingPurpleGold(phase);
        } else {
            int baseColor = switch (this.skill.getTier()) {
                case 2 -> 0xFF55FF55;
                case 3 -> 0xFF5555FF;
                case 4 -> 0xFFFFAA00;
                default -> 0xFFAAAAAA;
            };
            drawStaticGlow(guiGraphics, boxX1, boxY1, boxX2 - boxX1, boxY2 - boxY1, baseColor, 4);
            titleColor = baseColor;
        }

        guiGraphics.drawString(font, this.title, tooltipX, tooltipY, titleColor, true);

        int descY = tooltipY + 12;
        for (FormattedCharSequence line : this.description) {
            guiGraphics.drawString(font, line, tooltipX, descY, -1, true);
            descY += 10;
        }

        SkillState state = getState();
        int costColor;
        if (state == SkillState.UNLOCKED) {
            costColor = 0x66FF55;
        } else {
            int currentXp = ExperienceUtils.getPlayerTotalXp(this.client.player);
            costColor = currentXp >= skill.getCost() ? 0xFFFF55 : 0xFF5555;
        }
        guiGraphics.drawString(font, Component.literal("Cost: " + skill.getCost() + " XP"), tooltipX, descY, costColor, true);

        guiGraphics.pose().popPose();
    }

    private void drawStaticGlow(GuiGraphics guiGraphics, int x, int y, int w, int h, int color, int glowSize) {
        int fadeColor = color & 0x00FFFFFF;
        drawGlowQuad(guiGraphics, x, y - glowSize, w, glowSize, fadeColor, fadeColor, color, color);
        drawGlowQuad(guiGraphics, x, y + h, w, glowSize, color, color, fadeColor, fadeColor);
        drawGlowQuad(guiGraphics, x - glowSize, y, glowSize, h, fadeColor, color, color, fadeColor);
        drawGlowQuad(guiGraphics, x + w, y, glowSize, h, color, fadeColor, fadeColor, color);
        drawGlowQuad(guiGraphics, x - glowSize, y - glowSize, glowSize, glowSize, fadeColor, fadeColor, color, fadeColor);
        drawGlowQuad(guiGraphics, x + w, y - glowSize, glowSize, glowSize, fadeColor, fadeColor, fadeColor, color);
        drawGlowQuad(guiGraphics, x + w, y + h, glowSize, glowSize, color, fadeColor, fadeColor, fadeColor);
        drawGlowQuad(guiGraphics, x - glowSize, y + h, glowSize, glowSize, fadeColor, color, fadeColor, fadeColor);
        guiGraphics.renderOutline(x, y, w, h, color);
    }

    private void drawDynamicBorder(GuiGraphics guiGraphics, int x, int y, int w, int h, float phase, int glowSize) {
        int cTL = getHummingPurpleGold(phase);
        int cTR = getHummingPurpleGold(phase + (float)(Math.PI / 2));
        int cBR = getHummingPurpleGold(phase + (float)(Math.PI));
        int cBL = getHummingPurpleGold(phase + (float)(Math.PI * 1.5));
        int cTL_fade = cTL & 0x00FFFFFF;
        int cTR_fade = cTR & 0x00FFFFFF;
        int cBR_fade = cBR & 0x00FFFFFF;
        int cBL_fade = cBL & 0x00FFFFFF;
        drawGlowQuad(guiGraphics, x, y - glowSize, w, glowSize, cTL_fade, cTR_fade, cTR, cTL);
        drawGlowQuad(guiGraphics, x, y + h, w, glowSize, cBL, cBR, cBR_fade, cBL_fade);
        drawGlowQuad(guiGraphics, x - glowSize, y, glowSize, h, cTL_fade, cTL, cBL, cBL_fade);
        drawGlowQuad(guiGraphics, x + w, y, glowSize, h, cTR, cTR_fade, cBR_fade, cBR);
        drawGlowQuad(guiGraphics, x - glowSize, y - glowSize, glowSize, glowSize, cTL_fade, cTL_fade, cTL, cTL_fade);
        drawGlowQuad(guiGraphics, x + w, y - glowSize, glowSize, glowSize, cTR_fade, cTR_fade, cTR_fade, cTR);
        drawGlowQuad(guiGraphics, x + w, y + h, glowSize, glowSize, cBR, cBR_fade, cBR_fade, cBR_fade);
        drawGlowQuad(guiGraphics, x - glowSize, y + h, glowSize, glowSize, cBL_fade, cBL, cBL_fade, cBL_fade);
        drawGlowQuad(guiGraphics, x, y, w, 1, cTL, cTR, cTR, cTL);
        drawGlowQuad(guiGraphics, x + w - 1, y, 1, h, cTR, cTR, cBR, cBR);
        drawGlowQuad(guiGraphics, x, y + h - 1, w, 1, cBL, cBR, cBR, cBL);
        drawGlowQuad(guiGraphics, x, y, 1, h, cTL, cTL, cBL, cBL);
    }

    private void drawGlowQuad(GuiGraphics guiGraphics, int x, int y, int w, int h,
                               int cTL, int cTR, int cBR, int cBL) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = guiGraphics.pose().last().pose();

        float aTL = (float)(cTL >> 24 & 255) / 255.0F;
        float aTR = (float)(cTR >> 24 & 255) / 255.0F;
        float aBR = (float)(cBR >> 24 & 255) / 255.0F;
        float aBL = (float)(cBL >> 24 & 255) / 255.0F;
        boolean flip = (aTR + aBL) > (aTL + aBR);

        float rTL = (float)(cTL >> 16 & 255) / 255.0F; float gTL = (float)(cTL >> 8 & 255) / 255.0F; float bTL = (float)(cTL & 255) / 255.0F;
        float rTR = (float)(cTR >> 16 & 255) / 255.0F; float gTR = (float)(cTR >> 8 & 255) / 255.0F; float bTR = (float)(cTR & 255) / 255.0F;
        float rBR = (float)(cBR >> 16 & 255) / 255.0F; float gBR = (float)(cBR >> 8 & 255) / 255.0F; float bBR = (float)(cBR & 255) / 255.0F;
        float rBL = (float)(cBL >> 16 & 255) / 255.0F; float gBL = (float)(cBL >> 8 & 255) / 255.0F; float bBL = (float)(cBL & 255) / 255.0F;

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        if (flip) {
            buffer.vertex(matrix, x, y, 0).color(rTL, gTL, bTL, aTL).endVertex();
            buffer.vertex(matrix, x, y + h, 0).color(rBL, gBL, bBL, aBL).endVertex();
            buffer.vertex(matrix, x + w, y + h, 0).color(rBR, gBR, bBR, aBR).endVertex();
            buffer.vertex(matrix, x + w, y, 0).color(rTR, gTR, bTR, aTR).endVertex();
        } else {
            buffer.vertex(matrix, x, y + h, 0).color(rBL, gBL, bBL, aBL).endVertex();
            buffer.vertex(matrix, x + w, y + h, 0).color(rBR, gBR, bBR, aBR).endVertex();
            buffer.vertex(matrix, x + w, y, 0).color(rTR, gTR, bTR, aTR).endVertex();
            buffer.vertex(matrix, x, y, 0).color(rTL, gTL, bTL, aTL).endVertex();
        }
        tesselator.end();
        RenderSystem.disableBlend();
    }

    private int getHummingPurpleGold(float phase) {
        float t = (Mth.sin(phase) + 1.0f) * 0.5f;
        int r = (int) (160 + (255 - 160) * t);
        int g = (int) (32 + (215 - 32) * t);
        int b = (int) (240 + (0 - 240) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public SkillState getState() {
        if (ClientSkillData.isSkillUnlocked(this.skill)) return SkillState.UNLOCKED;
        int currentXp = ExperienceUtils.getPlayerTotalXp(this.client.player);
        boolean hasEnoughXp = currentXp >= this.skill.getCost();
        boolean hasAllRequired = this.skill.getRequiredSkills().stream()
                .allMatch(ClientSkillData::isSkillUnlocked);
        if (hasEnoughXp && hasAllRequired) return SkillState.CAN_UNLOCK;
        return SkillState.LOCKED;
    }

    public Skill getSkill() { return this.skill; }
    public int getWorldX() { return this.worldX; }
    public int getWorldY() { return this.worldY; }
    public boolean isMouseOver(int panX, int panY, int mouseX, int mouseY) {
        int sx = this.worldX + panX;
        int sy = this.worldY + panY;
        return mouseX >= sx && mouseX <= sx + 26 && mouseY >= sy && mouseY <= sy + 26;
    }
    public boolean mouseClicked(double mouseX, double mouseY, int button, int panX, int panY) {
        return isMouseOver(panX, panY, (int) mouseX, (int) mouseY);
    }
}
