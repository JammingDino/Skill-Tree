package com.jd_skill_tree.screens;

import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.skills.Skill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.util.List;

public class RadialSkillScreen extends Screen {

    private final List<Skill> activeSkills;
    private final InputUtil.Key boundKey;
    private int hoveredIndex = -1;
    private int centerX;
    private int centerY;
    private boolean actionTriggered = false;

    // Visual Constants
    private static final float INNER_RADIUS = 30.0f;
    private static final float OUTER_RADIUS = 80.0f;
    private static final float SELECTED_EXPANSION = 10.0f; // How much the slice grows when hovered

    public RadialSkillScreen(List<Skill> skills, InputUtil.Key key) {
        super(Text.of("Active Skills"));
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
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No full-screen background tint, keeps it feeling like an overlay
        if (activeSkills.isEmpty()) return;

        updateHoveredIndex(mouseX, mouseY);

        // 1. Draw Ring Backgrounds
        float angleStep = 360.0f / activeSkills.size();

        for (int i = 0; i < activeSkills.size(); i++) {
            float startAngle = (i * angleStep) - 90 - (angleStep / 2);
            float endAngle = startAngle + angleStep;
            boolean isHovered = (i == hoveredIndex);

            // Colors
            // Normal: Dark Grey (Transparent)
            // Hovered: Gold/Orange (Solid)
            int color = isHovered ? 0xCCFFAA00 : 0xAA202020;
            float radiusMod = isHovered ? SELECTED_EXPANSION : 0;

            // Draw the Slice
            drawRingSection(context, centerX, centerY, INNER_RADIUS, OUTER_RADIUS + radiusMod, startAngle, endAngle, color);
        }

        // 2. Draw Dividers (Lines)
        for (int i = 0; i < activeSkills.size(); i++) {
            float angle = (i * angleStep) - 90 - (angleStep / 2);
            // Draw a black line to separate slices cleanly
            drawLine(context, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, angle, 2.0f, 0xFF000000);
        }

        // 3. Draw Icons
        for (int i = 0; i < activeSkills.size(); i++) {
            Skill skill = activeSkills.get(i);
            boolean isHovered = (i == hoveredIndex);

            float angle = (float) Math.toRadians(i * angleStep - 90);
            float dist = (INNER_RADIUS + OUTER_RADIUS + (isHovered ? SELECTED_EXPANSION : 0)) / 2.0f;

            float x = centerX + (float) Math.cos(angle) * dist;
            float y = centerY + (float) Math.sin(angle) * dist;

            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);

            // Pop effect when hovered
            float scale = isHovered ? 1.5f : 1.0f;
            context.getMatrices().scale(scale, scale, 1.0f);

            context.drawItem(skill.getIcon(), -8, -8); // Centered on 0,0 relative to push
            context.getMatrices().pop();
        }

        // 4. Center Info (Selected Skill Name)
        if (hoveredIndex != -1) {
            Skill selected = activeSkills.get(hoveredIndex);
            drawCenteredTextWithShadow(context, this.textRenderer, selected.getName(), centerX, centerY - 4, 0xFFFFFF);
        } else {
            drawCenteredTextWithShadow(context, this.textRenderer, "Select", centerX, centerY - 4, 0xAAAAAA);
        }
    }

    private void updateHoveredIndex(int mouseX, int mouseY) {
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);

        // Don't select if mouse is inside the donut hole or too far out
        if (dist < INNER_RADIUS - 5 || dist > OUTER_RADIUS + 30) {
            hoveredIndex = -1;
            return;
        }

        double angleRad = Math.atan2(dy, dx);
        double angleDeg = Math.toDegrees(angleRad);

        // Normalize 0 to Top (-90)
        double normalized = angleDeg + 90;
        if (normalized < 0) normalized += 360;

        float sliceSize = 360.0f / activeSkills.size();

        // Offset logic to align slice center with angle
        double checkAngle = normalized + (sliceSize / 2);
        if (checkAngle >= 360) checkAngle -= 360;

        hoveredIndex = (int) (checkAngle / sliceSize);
        if (hoveredIndex >= activeSkills.size()) hoveredIndex = 0;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == this.boundKey.getCode() && !actionTriggered) {
            triggerSelectedSkill();
            this.close();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void triggerSelectedSkill() {
        if (hoveredIndex != -1 && hoveredIndex < activeSkills.size()) {
            Skill selected = activeSkills.get(hoveredIndex);

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeIdentifier(selected.getId());
            ClientPlayNetworking.send(SkillNetworking.TRIGGER_ACTIVE_SKILL_PACKET_ID, buf);

            actionTriggered = true;
        }
    }

    // --- RENDER HELPERS ---

    private void drawRingSection(DrawContext context, int cx, int cy, float rInner, float rOuter, float startDeg, float endDeg, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Draw segments to form the arc
        float step = 5.0f; // Resolution
        for (float i = startDeg; i < endDeg; i += step) {
            float current = i;
            float next = Math.min(i + step, endDeg);

            float rad1 = (float)Math.toRadians(current);
            float rad2 = (float)Math.toRadians(next);

            float cos1 = (float)Math.cos(rad1);
            float sin1 = (float)Math.sin(rad1);
            float cos2 = (float)Math.cos(rad2);
            float sin2 = (float)Math.sin(rad2);

            // Quad vertices (Outer 1, Inner 1, Inner 2, Outer 2)
            buffer.vertex(matrix, cx + cos2 * rOuter, cy + sin2 * rOuter, 0).color(r, g, b, a).next();
            buffer.vertex(matrix, cx + cos2 * rInner, cy + sin2 * rInner, 0).color(r, g, b, a).next();
            buffer.vertex(matrix, cx + cos1 * rInner, cy + sin1 * rInner, 0).color(r, g, b, a).next();
            buffer.vertex(matrix, cx + cos1 * rOuter, cy + sin1 * rOuter, 0).color(r, g, b, a).next();
        }

        tessellator.draw();
        RenderSystem.disableBlend();
    }

    private void drawLine(DrawContext context, int cx, int cy, float rInner, float rOuter, float angleDeg, float width, int color) {
        // Draw a simple quad as a thick line
        double rad = Math.toRadians(angleDeg);
        float cos = (float)Math.cos(rad);
        float sin = (float)Math.sin(rad);

        float startX = cx + cos * rInner;
        float startY = cy + sin * rInner;
        float endX = cx + cos * rOuter;
        float endY = cy + sin * rOuter;

        // We can use the context's line drawer or just a thin rect
        // Since angles are arbitrary, we use the fill logic but calculated manually or use debug line
        // For visual polish, let's use the Tessellator again for a rotated quad

        // ... (Omitting complex line width math for brevity, sticking to simple GL lines for dividers)

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(width);

        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        Matrix4f m = context.getMatrices().peek().getPositionMatrix();

        float a = (float)(color >> 24 & 255) / 255.0F;

        b.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        b.vertex(m, startX, startY, 0).color(0f,0f,0f, a).next();
        b.vertex(m, endX, endY, 0).color(0f,0f,0f, a).next();
        t.draw();
    }

    // Helper needed for the center text
    private void drawCenteredTextWithShadow(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, String text, int centerX, int centerY, int color) {
        context.drawTextWithShadow(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, centerY, color);
    }
}