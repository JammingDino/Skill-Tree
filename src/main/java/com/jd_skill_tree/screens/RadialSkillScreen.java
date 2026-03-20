package com.jd_skill_tree.screens;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.skills.Skill;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RadialSkillScreen extends Screen {

    private final List<Skill> activeSkills;
    private final KeyMapping boundKey;
    private int hoveredIndex = -1;
    private int centerX;
    private int centerY;
    private boolean actionTriggered = false;

    private static final float INNER_RADIUS = 30.0f;
    private static final float OUTER_RADIUS = 80.0f;
    private static final float SELECTED_EXPANSION = 10.0f;

    public RadialSkillScreen(List<Skill> skills, KeyMapping key) {
        super(Component.literal("Active Skills"));
        this.activeSkills = skills;
        this.boundKey = key;
    }

    @Override
    protected void init() {
        this.centerX = this.width / 2;
        this.centerY = this.height / 2;
        this.actionTriggered = false;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (activeSkills.isEmpty()) return;
        updateHoveredIndex(mouseX, mouseY);
        float angleStep = 360.0f / activeSkills.size();

        for (int i = 0; i < activeSkills.size(); i++) {
            float startAngle = (i * angleStep) - 90 - (angleStep / 2);
            float endAngle = startAngle + angleStep;
            boolean isHovered = (i == hoveredIndex);
            int color = isHovered ? 0xCCFFAA00 : 0xAA202020;
            float radiusMod = isHovered ? SELECTED_EXPANSION : 0;
            drawRingSection(guiGraphics, centerX, centerY, INNER_RADIUS, OUTER_RADIUS + radiusMod, startAngle, endAngle, color);
        }

        for (int i = 0; i < activeSkills.size(); i++) {
            float angle = (i * angleStep) - 90 - (angleStep / 2);
            drawLineRadial(guiGraphics, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, angle, 2.0f, 0xFF000000);
        }

        for (int i = 0; i < activeSkills.size(); i++) {
            Skill skill = activeSkills.get(i);
            boolean isHovered = (i == hoveredIndex);
            float angle = (float) Math.toRadians(i * angleStep - 90);
            float dist = (INNER_RADIUS + OUTER_RADIUS + (isHovered ? SELECTED_EXPANSION : 0)) / 2.0f;
            float x = centerX + (float) Math.cos(angle) * dist;
            float y = centerY + (float) Math.sin(angle) * dist;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            float scale = isHovered ? 1.5f : 1.0f;
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.renderItem(skill.getIcon(), -8, -8);

            if (this.minecraft != null && this.minecraft.player != null) {
                IUnlockedSkillsData data = (IUnlockedSkillsData) this.minecraft.player;
                float ticksLeft = data.getCooldownProgress(skill.getId(), delta);
                if (ticksLeft > 0 && skill.getCooldown() > 0) {
                    float percentage = ticksLeft / (float) skill.getCooldown();
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 100);
                    drawCooldownOverlay(guiGraphics, 0, 0, percentage);
                    guiGraphics.pose().scale(0.7f, 0.7f, 1.0f);
                    String timeText = String.format("%.1fs", ticksLeft / 20.0f);
                    int txtW = this.font.width(timeText);
                    guiGraphics.drawString(this.font, timeText, -txtW / 2, 14, 0xFFFFFF, true);
                    guiGraphics.pose().pop();
                }
            }
            guiGraphics.pose().popPose();
        }

        if (hoveredIndex != -1) {
            Skill selected = activeSkills.get(hoveredIndex);
            int nameW = this.font.width(selected.getName());
            guiGraphics.drawString(this.font, selected.getName(), centerX - nameW / 2, centerY - 4, 0xFFFFFF, true);
        } else {
            int selectW = this.font.width("Select");
            guiGraphics.drawString(this.font, "Select", centerX - selectW / 2, centerY - 4, 0xAAAAAA, true);
        }
    }

    private void updateHoveredIndex(int mouseX, int mouseY) {
        float dx = mouseX - centerX; float dy = mouseY - centerY;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);
        if (dist < INNER_RADIUS - 5 || dist > OUTER_RADIUS + 30) { hoveredIndex = -1; return; }
        double angleRad = Math.atan2(dy, dx);
        double angleDeg = Math.toDegrees(angleRad);
        double normalized = angleDeg + 90;
        if (normalized < 0) normalized += 360;
        float sliceSize = 360.0f / activeSkills.size();
        double checkAngle = normalized + (sliceSize / 2);
        if (checkAngle >= 360) checkAngle -= 360;
        hoveredIndex = (int) (checkAngle / sliceSize);
        if (hoveredIndex >= activeSkills.size()) hoveredIndex = 0;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == this.boundKey.getKey().getValue() && !actionTriggered) {
            triggerSelectedSkill();
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void triggerSelectedSkill() {
        if (hoveredIndex != -1 && hoveredIndex < activeSkills.size()) {
            Skill selected = activeSkills.get(hoveredIndex);
            SkillNetworking.CHANNEL.sendToServer(new SkillNetworking.TriggerActiveSkillMessage(selected.getId()));
            actionTriggered = true;
        }
    }

    private void drawRingSection(GuiGraphics guiGraphics, int cx, int cy, float rInner, float rOuter,
                                  float startDeg, float endDeg, int color) {
        Matrix4f matrix = guiGraphics.pose().last().pose();
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float step = 5.0f;
        for (float i = startDeg; i < endDeg; i += step) {
            float next = Math.min(i + step, endDeg);
            float rad1 = (float)Math.toRadians(i); float rad2 = (float)Math.toRadians(next);
            float cos1 = (float)Math.cos(rad1); float sin1 = (float)Math.sin(rad1);
            float cos2 = (float)Math.cos(rad2); float sin2 = (float)Math.sin(rad2);
            buffer.vertex(matrix, cx + cos2 * rOuter, cy + sin2 * rOuter, 0).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, cx + cos2 * rInner, cy + sin2 * rInner, 0).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, cx + cos1 * rInner, cy + sin1 * rInner, 0).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, cx + cos1 * rOuter, cy + sin1 * rOuter, 0).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private void drawLineRadial(GuiGraphics guiGraphics, int cx, int cy, float rInner, float rOuter,
                                 float angleDeg, float lineWidth, int color) {
        double rad = Math.toRadians(angleDeg);
        float cos = (float)Math.cos(rad); float sin = (float)Math.sin(rad);
        float startX = cx + cos * rInner; float startY = cy + sin * rInner;
        float endX = cx + cos * rOuter; float endY = cy + sin * rOuter;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(lineWidth);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, startX, startY, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, endX, endY, 0).color(r, g, b, a).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private void drawCooldownOverlay(GuiGraphics guiGraphics, int x, int y, float progress) {
        if (progress <= 0) return;
        Matrix4f matrix = guiGraphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y, 0).color(0.0f, 0.0f, 0.0f, 0.6f).endVertex();
        float maxAngle = 360.0f * progress;
        for (int i = 0; i <= (int)maxAngle; i += 5) {
            double radAngle = Math.toRadians(i - 90);
            buffer.vertex(matrix, x + (float)Math.cos(radAngle) * 16, y + (float)Math.sin(radAngle) * 16, 0)
                    .color(0.0f, 0.0f, 0.0f, 0.6f).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
}
