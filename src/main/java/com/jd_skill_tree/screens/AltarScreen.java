package com.jd_skill_tree.screens;

import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.screens.widgets.SkillWidget;
import com.jd_skill_tree.skills.ModSkills;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.utils.ExperienceUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class AltarScreen extends Screen {

    private static final ResourceLocation WINDOW_TEXTURE = new ResourceLocation("textures/gui/advancements/window.png");
    private static final String ALTAR_TEXT = "Skill Altar Tier ";
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 140;

    private static int tier = 0;
    public static int x = 0;
    public static int y = 0;

    private final List<SkillWidget> skillWidgets = new ArrayList<>();
    private final Map<ResourceLocation, SkillWidget> skillWidgetMap = new HashMap<>();

    private static final int RADIAL_SPACING = 60;

    private double panX = 0;
    private double panY = 0;
    private int treeMinX, treeMinY, treeMaxX, treeMaxY;

    private double scale = 1.0;
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 2.0;
    private static final double ZOOM_SENSITIVITY = 0.1;

    public AltarScreen(Component title, int Tier) {
        super(title);
        tier = Tier;
    }

    @Override
    protected void init() {
        this.skillWidgets.clear();
        this.skillWidgetMap.clear();
        List<Skill> skillsToDisplay = ModSkills.getSkillsForTier(tier);
        Map<ResourceLocation, SkillNode> skillNodes = buildSkillTree(skillsToDisplay);
        calculateCircularPositions(skillNodes);
        calculateTreeBounds();
        double screenCenterX = x + WINDOW_WIDTH / 2.0;
        double screenCenterY = y + WINDOW_HEIGHT / 2.0;
        this.panX = screenCenterX - 13;
        this.panY = screenCenterY - 9;
    }

    private static class SkillNode {
        Skill skill;
        int depth;
        List<SkillNode> children = new ArrayList<>();
        List<SkillNode> parents = new ArrayList<>();
        int x, y;
        double angle;
        int leavesCount = 0;
        SkillNode(Skill skill) { this.skill = skill; }
    }

    private Map<ResourceLocation, SkillNode> buildSkillTree(List<Skill> skills) {
        Map<ResourceLocation, SkillNode> nodes = new HashMap<>();
        List<SkillNode> rootNodes = new ArrayList<>();
        for (Skill skill : skills) nodes.put(skill.getId(), new SkillNode(skill));
        for (SkillNode node : nodes.values()) {
            List<Skill> requiredSkills = node.skill.getRequiredSkills();
            if (!requiredSkills.isEmpty()) {
                for (Skill requiredSkill : requiredSkills) {
                    SkillNode parent = nodes.get(requiredSkill.getId());
                    if (parent != null) { node.parents.add(parent); parent.children.add(node); }
                }
            } else { rootNodes.add(node); }
        }
        calculateDepths(rootNodes);
        return nodes;
    }

    private void calculateDepths(List<SkillNode> roots) {
        Queue<SkillNode> queue = new LinkedList<>(roots);
        for (SkillNode root : roots) root.depth = 0;
        while (!queue.isEmpty()) {
            SkillNode current = queue.poll();
            for (SkillNode child : current.children) { child.depth = current.depth + 1; queue.add(child); }
        }
    }

    private void calculateCircularPositions(Map<ResourceLocation, SkillNode> nodes) {
        List<SkillNode> rootNodes = new ArrayList<>();
        for (SkillNode node : nodes.values()) { if (node.parents.isEmpty()) rootNodes.add(node); }
        for (SkillNode root : rootNodes) calculateLeafCounts(root, new HashSet<>());
        double currentAngle = 0;
        double totalWeight = 0;
        for (SkillNode root : rootNodes) totalWeight += Math.log(root.leavesCount) + 1.5;
        for (SkillNode root : rootNodes) {
            double weight = Math.log(root.leavesCount) + 1.5;
            double angleWedge = (weight / totalWeight) * (Math.PI * 2);
            assignNodePosition(root, currentAngle, currentAngle + angleWedge, new HashSet<>());
            currentAngle += angleWedge;
        }
        this.skillWidgets.clear();
        for (SkillNode node : nodes.values()) {
            SkillWidget widget = new SkillWidget(node.skill, node.x, node.y, this);
            this.skillWidgets.add(widget);
            this.skillWidgetMap.put(node.skill.getId(), widget);
        }
    }

    private void calculateLeafCounts(SkillNode node, Set<SkillNode> visited) {
        if (!visited.add(node)) return;
        if (node.children.isEmpty()) { node.leavesCount = 1; }
        else {
            node.leavesCount = 0;
            for (SkillNode child : node.children) { calculateLeafCounts(child, visited); node.leavesCount += child.leavesCount; }
        }
    }

    private void assignNodePosition(SkillNode node, double startAngle, double endAngle, Set<SkillNode> visited) {
        if (!visited.add(node)) return;
        node.angle = (startAngle + endAngle) / 2.0;
        double radius = ((node.depth + 1) * RADIAL_SPACING) + (node.depth * 10);
        node.x = (int) (Math.cos(node.angle) * radius);
        node.y = (int) (Math.sin(node.angle) * radius);
        if (!node.children.isEmpty()) {
            double currentChildStart = startAngle;
            double totalChildLeaves = 0;
            for (SkillNode child : node.children) totalChildLeaves += child.leavesCount;
            double availableArc = endAngle - startAngle;
            for (SkillNode child : node.children) {
                double percent = (double) child.leavesCount / totalChildLeaves;
                double childWedge = availableArc * percent;
                assignNodePosition(child, currentChildStart, currentChildStart + childWedge, visited);
                currentChildStart += childWedge;
            }
        }
    }

    private void calculateTreeBounds() {
        if (skillWidgets.isEmpty()) return;
        this.treeMinX = skillWidgets.get(0).getWorldX();
        this.treeMinY = skillWidgets.get(0).getWorldY();
        this.treeMaxX = skillWidgets.get(0).getWorldX() + 26;
        this.treeMaxY = skillWidgets.get(0).getWorldY() + 26;
        for (SkillWidget widget : skillWidgets) {
            this.treeMinX = Math.min(this.treeMinX, widget.getWorldX());
            this.treeMinY = Math.min(this.treeMinY, widget.getWorldY());
            this.treeMaxX = Math.max(this.treeMaxX, widget.getWorldX() + 26);
            this.treeMaxY = Math.max(this.treeMaxY, widget.getWorldY() + 26);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) { this.panX += deltaX; this.panY += deltaY; clampPan(); return true; }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseInWindow((int)mouseX, (int)mouseY)) return false;
        double worldMouseX = (mouseX - this.panX) / this.scale;
        double worldMouseY = (mouseY - this.panY) / this.scale;
        for (SkillWidget widget : this.skillWidgets) {
            if (widget.mouseClicked(worldMouseX, worldMouseY, button, 0, 0)) {
                if (widget.getSkill().getId().toString().equals("jd_skill_tree:reset_skills")) {
                    if (widget.getState() == SkillWidget.SkillState.CAN_UNLOCK) {
                        SkillNetworking.CHANNEL.sendToServer(new SkillNetworking.ResetSkillsMessage());
                        if (this.minecraft != null) this.minecraft.getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.ENDER_DRAGON_FLAP, 1.0F));
                        return true;
                    }
                } else if (widget.getState() == SkillWidget.SkillState.CAN_UNLOCK) {
                    SkillNetworking.CHANNEL.sendToServer(new SkillNetworking.UnlockSkillMessage(widget.getSkill().getId()));
                    if (this.minecraft != null) this.minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseInWindow(int mouseX, int mouseY) {
        return mouseX >= x + 10 && mouseX <= x + WINDOW_WIDTH - 10 &&
                mouseY >= y + 2 && mouseY <= y + WINDOW_HEIGHT - 10;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        double newScale = this.scale + (amount * ZOOM_SENSITIVITY);
        newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
        if (newScale != this.scale) {
            double worldMouseX = (mouseX - this.panX) / this.scale;
            double worldMouseY = (mouseY - this.panY) / this.scale;
            this.scale = newScale;
            this.panX = mouseX - (worldMouseX * this.scale);
            this.panY = mouseY - (worldMouseY * this.scale);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private void clampPan() {
        int viewX1 = x + 5; int viewY1 = y + 18; int viewX2 = x + WINDOW_WIDTH - 5; int viewY2 = y + WINDOW_HEIGHT - 5;
        int treeWidth = this.treeMaxX - this.treeMinX; int viewWidth = viewX2 - viewX1;
        if (treeWidth > viewWidth) {
            this.panX = Math.max(this.panX, viewX2 - this.treeMaxX);
            this.panX = Math.min(this.panX, viewX1 - this.treeMinX);
        } else {
            this.panX = Math.max(this.panX, viewX1 - this.treeMinX);
            this.panX = Math.min(this.panX, viewX2 - this.treeMaxX);
        }
        int treeHeight = this.treeMaxY - this.treeMinY; int viewHeight = viewY2 - viewY1;
        if (treeHeight > viewHeight) {
            this.panY = Math.max(this.panY, viewY2 - this.treeMaxY);
            this.panY = Math.min(this.panY, viewY1 - this.treeMinY);
        } else {
            this.panY = Math.max(this.panY, viewY1 - this.treeMinY);
            this.panY = Math.min(this.panY, viewY2 - this.treeMaxY);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        x = (this.width - WINDOW_WIDTH) / 2;
        y = (this.height - WINDOW_HEIGHT) / 2;
        this.drawWindow(guiGraphics, x, y);
        guiGraphics.enableScissor(x + 9, y + 17, x + WINDOW_WIDTH - 9, y + WINDOW_HEIGHT - 9);
        renderBackground(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.panX, this.panY, 0);
        guiGraphics.pose().scale((float)this.scale, (float)this.scale, 1.0f);
        Set<ResourceLocation> highlightedPath = new HashSet<>();
        double worldMouseX = (mouseX - this.panX) / this.scale;
        double worldMouseY = (mouseY - this.panY) / this.scale;
        for (SkillWidget widget : this.skillWidgets) {
            if (widget.isMouseOver(0, 0, (int) worldMouseX, (int) worldMouseY)) {
                getAncestors(widget.getSkill(), highlightedPath); break;
            }
        }
        drawSkillLines(guiGraphics, 0, 0, highlightedPath);
        for (SkillWidget widget : this.skillWidgets) {
            widget.render(guiGraphics, 0, 0, (int)worldMouseX, (int)worldMouseY, delta);
        }
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        drawTooltips(guiGraphics, mouseX, mouseY);
    }

    private void getAncestors(Skill skill, Set<ResourceLocation> path) {
        path.add(skill.getId());
        for (Skill parent : skill.getRequiredSkills()) {
            if (!path.contains(parent.getId())) getAncestors(parent, path);
        }
    }

    private void drawSkillLines(GuiGraphics guiGraphics, int offsetX, int offsetY, Set<ResourceLocation> highlightedPath) {
        int colorNormal = 0xFF808080; int colorFade = 0x40404040; int colorHigh = 0xFFFFFF00;
        for (SkillWidget widget : this.skillWidgets) {
            for (Skill requiredSkill : widget.getSkill().getRequiredSkills()) {
                SkillWidget requiredWidget = skillWidgetMap.get(requiredSkill.getId());
                if (requiredWidget != null) {
                    int startX = widget.getWorldX() + 13 + offsetX; int startY = widget.getWorldY() + 13 + offsetY;
                    int endX = requiredWidget.getWorldX() + 13 + offsetX; int endY = requiredWidget.getWorldY() + 13 + offsetY;
                    int color = colorNormal;
                    if (!highlightedPath.isEmpty()) {
                        if (highlightedPath.contains(widget.getSkill().getId()) && highlightedPath.contains(requiredSkill.getId()))
                            color = colorHigh;
                        else color = colorFade;
                    }
                    drawLine(guiGraphics, startX, startY, endX, endY, color);
                }
            }
        }
    }

    private void drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isMouseInWindow(mouseX, mouseY)) return;
        double worldMouseX = (mouseX - this.panX) / this.scale;
        double worldMouseY = (mouseY - this.panY) / this.scale;
        for (SkillWidget widget : this.skillWidgets) {
            if (widget.isMouseOver(0, 0, (int) worldMouseX, (int) worldMouseY)) {
                widget.renderTooltip(guiGraphics, mouseX, mouseY); break;
            }
        }
    }

    public static void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1); int dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1; int sy = (y1 < y2) ? 1 : -1;
        int err = dx - dy;
        while (true) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    public void drawWindow(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(WINDOW_TEXTURE, x, y, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        guiGraphics.drawString(this.font, ALTAR_TEXT + tier, x + 8, y + 6, 4210752, false);
        if (this.minecraft != null && this.minecraft.player != null) {
            int currentXp = ExperienceUtils.getPlayerTotalXp(this.minecraft.player);
            String xpText = "XP: " + currentXp;
            int textWidth = this.font.width(xpText);
            int xpX = x + WINDOW_WIDTH - textWidth - 10;
            guiGraphics.drawString(this.font, xpText, xpX, y + 6, 0x80FF20, true);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        if (this.minecraft != null && this.minecraft.level != null) {
            guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        } else {
            super.renderBackground(guiGraphics);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean isPauseScreen() { return false; }
}
