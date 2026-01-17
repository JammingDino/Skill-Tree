package com.jd_skill_tree.screens;

import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.screens.widgets.SkillWidget;
import com.jd_skill_tree.skills.ModSkills;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.utils.ExperienceUtils;
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

import java.util.*;

public class AltarScreen extends Screen {

    private static final Identifier WINDOW_TEXTURE = new Identifier("textures/gui/advancements/window.png");
    private static final String ALTAR_TEXT = "Skill Altar Tier ";
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 140;

    private static int tier = 0;
    public static int x = 0;
    public static int y = 0;

    private final List<SkillWidget> skillWidgets = new ArrayList<>();
    private final Map<Identifier, SkillWidget> skillWidgetMap = new HashMap<>();

    // Layout configuration
    // Increased base spacing to 60 for better breathing room near center
    private static final int RADIAL_SPACING = 60;

    private double panX = 0;
    private double panY = 0;
    private int treeMinX, treeMinY, treeMaxX, treeMaxY;

    private double scale = 1.0;
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 2.0;
    private static final double ZOOM_SENSITIVITY = 0.1;

    public AltarScreen(Text title, int Tier) {
        super(title);
        tier = Tier;
    }

    @Override
    protected void init() {
        this.skillWidgets.clear();
        this.skillWidgetMap.clear();

        List<Skill> skillsToDisplay = ModSkills.getSkillsForTier(tier);

        // Organize skills by their tree structure
        Map<Identifier, SkillNode> skillNodes = buildSkillTree(skillsToDisplay);

        // Calculate positions for all skills using new weighted algorithm
        calculateCircularPositions(skillNodes);

        // --- CALCULATE TREE BOUNDARIES ---
        calculateTreeBounds();

        // --- CENTER THE INITIAL VIEW ---
        double screenCenterX = x + WINDOW_WIDTH / 2.0;
        double screenCenterY = y + WINDOW_HEIGHT / 2.0;

        this.panX = screenCenterX - 13;
        this.panY = screenCenterY - 9;
    }

    /**
     * Represents a node in the skill tree with position and hierarchy info
     */
    private static class SkillNode {
        Skill skill;
        int depth;
        List<SkillNode> children = new ArrayList<>();
        List<SkillNode> parents = new ArrayList<>();
        int x, y;
        double angle;

        // NEW: Tracks how "heavy" this branch is to allocate enough space
        int leavesCount = 0;

        SkillNode(Skill skill) {
            this.skill = skill;
        }
    }

    /**
     * Build a tree structure from the flat skill list
     */
    private Map<Identifier, SkillNode> buildSkillTree(List<Skill> skills) {
        Map<Identifier, SkillNode> nodes = new HashMap<>();
        List<SkillNode> rootNodes = new ArrayList<>();

        for (Skill skill : skills) {
            nodes.put(skill.getId(), new SkillNode(skill));
        }

        for (SkillNode node : nodes.values()) {
            List<Skill> requiredSkills = node.skill.getRequiredSkills();

            if (!requiredSkills.isEmpty()) {
                for (Skill requiredSkill : requiredSkills) {
                    SkillNode parent = nodes.get(requiredSkill.getId());
                    if (parent != null) {
                        node.parents.add(parent);
                        parent.children.add(node);
                    }
                }
            } else {
                rootNodes.add(node);
            }
        }

        calculateDepths(rootNodes);
        return nodes;
    }

    private void calculateDepths(List<SkillNode> roots) {
        Queue<SkillNode> queue = new LinkedList<>(roots);
        for (SkillNode root : roots) {
            root.depth = 0;
        }
        while (!queue.isEmpty()) {
            SkillNode current = queue.poll();
            for (SkillNode child : current.children) {
                child.depth = current.depth + 1;
                queue.add(child);
            }
        }
    }

    /**
     * REPLACED: New Subtree-Aware Layout Algorithm
     */
    private void calculateCircularPositions(Map<Identifier, SkillNode> nodes) {
        // 1. Identify Root Nodes
        List<SkillNode> rootNodes = new ArrayList<>();
        for (SkillNode node : nodes.values()) {
            if (node.parents.isEmpty()) {
                rootNodes.add(node);
            }
        }

        // 2. Calculate Weights
        for (SkillNode root : rootNodes) {
            calculateLeafCounts(root, new HashSet<>());
        }

        // 3. Assign Angles (Modified with Logarithmic Dampening)
        double currentAngle = 0;
        double totalWeight = 0;

        // First pass: Calculate total dampened weight
        for (SkillNode root : rootNodes) {
            // Logarithm flattens the curve.
            // A tree of size 1 = 1.0 weight.
            // A tree of size 50 = ~4.9 weight (instead of 50.0).
            // This prevents massive gaps on the first ring.
            double weight = Math.log(root.leavesCount) + 1.5;
            totalWeight += weight;
        }

        for (SkillNode root : rootNodes) {
            double weight = Math.log(root.leavesCount) + 1.5;

            // Allocate wedge
            double angleWedge = (weight / totalWeight) * (Math.PI * 2);

            double startAngle = currentAngle;
            double endAngle = currentAngle + angleWedge;

            // Recursively position
            assignNodePosition(root, startAngle, endAngle, new HashSet<>());

            currentAngle += angleWedge;
        }

        // 4. Create Widgets
        this.skillWidgets.clear();
        for (SkillNode node : nodes.values()) {
            SkillWidget widget = new SkillWidget(node.skill, node.x, node.y, this);
            this.skillWidgets.add(widget);
            this.skillWidgetMap.put(node.skill.getId(), widget);
        }
    }

    /**
     * Recursive Pass 1: Count endpoints in subtree
     */
    private void calculateLeafCounts(SkillNode node, Set<SkillNode> visited) {
        if (!visited.add(node)) return;

        if (node.children.isEmpty()) {
            node.leavesCount = 1;
        } else {
            node.leavesCount = 0;
            for (SkillNode child : node.children) {
                calculateLeafCounts(child, visited);
                node.leavesCount += child.leavesCount;
            }
        }
    }

    /**
     * Recursive Pass 2: Assign positions using Quadratic Spacing
     */
    private void assignNodePosition(SkillNode node, double startAngle, double endAngle, Set<SkillNode> visited) {
        if (!visited.add(node)) return;

        // 1. Position self in the center of the assigned wedge
        node.angle = (startAngle + endAngle) / 2.0;

        // --- FIXED RADIUS FORMULA ---
        // 1. (node.depth + 1): Ensures Roots (Depth 0) are not at 0,0.
        //    They now start at 1x Spacing.
        // 2. Removed Quadratic (Math.pow): Fixes "too much spacing" at outer layers.
        // 3. Added small linear buffer (+10 * depth): Gives just enough extra room
        //    for outer nodes without exploding the distance.
        double radius = ((node.depth + 1) * RADIAL_SPACING) + (node.depth * 10);

        node.x = (int) (Math.cos(node.angle) * radius);
        node.y = (int) (Math.sin(node.angle) * radius);

        // 2. Position Children
        if (!node.children.isEmpty()) {
            double currentChildStart = startAngle;

            double totalChildLeaves = 0;
            for (SkillNode child : node.children) totalChildLeaves += child.leavesCount;

            double availableArc = endAngle - startAngle;

            for (SkillNode child : node.children) {
                // Give child a percentage of the parent's wedge
                double percent = (double) child.leavesCount / totalChildLeaves;
                double childWedge = availableArc * percent;

                assignNodePosition(child, currentChildStart, currentChildStart + childWedge, visited);

                currentChildStart += childWedge;
            }
        }
    }

    private void calculateTreeBounds() {
        if (skillWidgets.isEmpty()) {
            return;
        }
        this.treeMinX = skillWidgets.get(0).getWorldX();
        this.treeMinY = skillWidgets.get(0).getWorldY();
        this.treeMaxX = skillWidgets.get(0).getWorldX() + 26;
        this.treeMaxY = skillWidgets.get(0).getWorldY() + 26;

        for (SkillWidget widget : skillWidgets) {
            this.treeMinX = Math.max(this.treeMinX, widget.getWorldX());
            this.treeMinY = Math.max(this.treeMinY, widget.getWorldY());
            this.treeMaxX = Math.min(this.treeMaxX, widget.getWorldX() + 26);
            this.treeMaxY = Math.min(this.treeMaxY, widget.getWorldY() + 26);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
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
        if (!isMouseInWindow((int)mouseX, (int)mouseY)) return false;

        double worldMouseX = (mouseX - this.panX) / this.scale;
        double worldMouseY = (mouseY - this.panY) / this.scale;

        for (SkillWidget widget : this.skillWidgets) {
            if (widget.mouseClicked(worldMouseX, worldMouseY, button, 0, 0)) {
                if (widget.getSkill().getId().toString().equals("jd_skill_tree:reset_skills")) {
                    if (widget.getState() == SkillWidget.SkillState.CAN_UNLOCK) {
                        sendResetRequest();
                        assert this.client != null;
                        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0F));
                        return true;
                    }
                } else if (widget.getState() == SkillWidget.SkillState.CAN_UNLOCK) {
                    sendUnlockRequest(widget.getSkill());
                    assert this.client != null;
                    this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // Helper to ensure we don't show tooltips if the mouse is outside the main window area
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

    private void sendUnlockRequest(Skill skill) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeIdentifier(skill.getId());
        ClientPlayNetworking.send(SkillNetworking.UNLOCK_SKILL_PACKET_ID, buf);
    }

    private void sendResetRequest() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(SkillNetworking.RESET_SKILLS_PACKET_ID, buf);
    }

    private void clampPan() {
        int viewX1 = x + 5;
        int viewY1 = y + 18;
        int viewX2 = x + WINDOW_WIDTH - 5;
        int viewY2 = y + WINDOW_HEIGHT - 5;

        int treeWidth = this.treeMaxX - this.treeMinX;
        int viewWidth = viewX2 - viewX1;

        if (treeWidth > viewWidth) {
            this.panX = Math.max(this.panX, viewX2 - this.treeMaxX);
            this.panX = Math.min(this.panX, viewX1 - this.treeMinX);
        } else {
            this.panX = Math.max(this.panX, viewX1 - this.treeMinX);
            this.panX = Math.min(this.panX, viewX2 - this.treeMaxX);
        }

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

        this.drawWindow(context, x, y);

        context.enableScissor(x + 9, y + 17, x + WINDOW_WIDTH - 9, y + WINDOW_HEIGHT - 9);
        this.renderBackground(context);

        context.getMatrices().push();
        context.getMatrices().translate(this.panX, this.panY, 0);
        context.getMatrices().scale((float)this.scale, (float)this.scale, 1.0f);

        // --- 1. DETERMINE HOVERED PATH ---
        Set<Identifier> highlightedPath = new HashSet<>();

        // Convert mouse to world coords for detection
        double worldMouseX = (mouseX - this.panX) / this.scale;
        double worldMouseY = (mouseY - this.panY) / this.scale;

        for (SkillWidget widget : this.skillWidgets) {
            if (widget.isMouseOver(0, 0, (int) worldMouseX, (int) worldMouseY)) {
                // Found the hovered skill, calculate path to root
                getAncestors(widget.getSkill(), highlightedPath);
                break;
            }
        }

        // --- 2. DRAW LINES (With Highlighting) ---
        drawSkillLines(context, 0, 0, highlightedPath);

        // --- 3. DRAW WIDGETS ---
        for (SkillWidget widget : this.skillWidgets) {
            widget.render(context, 0, 0, (int)worldMouseX, (int)worldMouseY, delta);
        }

        context.getMatrices().pop();
        context.disableScissor();

        drawTooltips(context, mouseX, mouseY);
    }

    // Helper to find all parents recursively
    private void getAncestors(Skill skill, Set<Identifier> path) {
        path.add(skill.getId());
        for (Skill parent : skill.getRequiredSkills()) {
            // Avoid infinite loops if circular deps exist
            if (!path.contains(parent.getId())) {
                getAncestors(parent, path);
            }
        }
    }

    private void drawSkillLines(DrawContext context, int offsetX, int offsetY, Set<Identifier> highlightedPath) {
        // Colors
        int colorNormal = 0xFF808080; // Gray
        int colorFade   = 0x40404040; // Dark/Transparent Gray
        int colorHigh   = 0xFFFFFF00; // Bright Yellow/Gold for path

        for (SkillWidget widget : this.skillWidgets) {
            List<Skill> requiredSkills = widget.getSkill().getRequiredSkills();
            for (Skill requiredSkill : requiredSkills) {
                SkillWidget requiredWidget = skillWidgetMap.get(requiredSkill.getId());
                if (requiredWidget != null) {
                    int startX = widget.getWorldX() + 13 + offsetX;
                    int startY = widget.getWorldY() + 13 + offsetY;
                    int endX = requiredWidget.getWorldX() + 13 + offsetX;
                    int endY = requiredWidget.getWorldY() + 13 + offsetY;

                    int color = colorNormal;

                    // Logic:
                    // 1. If nothing is highlighted, draw Normal.
                    // 2. If something IS highlighted:
                    //    a. If THIS connection is part of the path, draw High.
                    //    b. Else, draw Fade.
                    if (!highlightedPath.isEmpty()) {
                        if (highlightedPath.contains(widget.getSkill().getId()) &&
                                highlightedPath.contains(requiredSkill.getId())) {
                            color = colorHigh;
                        } else {
                            color = colorFade;
                        }
                    }

                    drawLine(context, startX, startY, endX, endY, color);
                }
            }
        }
    }

    private void drawTooltips(DrawContext context, int mouseX, int mouseY) {
        if (!isMouseInWindow(mouseX, mouseY)) return;

        // Convert Screen Mouse -> World Mouse
        double worldMouseX = (mouseX - this.panX) / this.scale;
        double worldMouseY = (mouseY - this.panY) / this.scale;

        for (SkillWidget widget : this.skillWidgets) {
            // Use the Converted World Mouse positions to check for hover
            if (widget.isMouseOver(0, 0, (int) worldMouseX, (int) worldMouseY)) {
                // Draw the tooltip at the REAL screen mouseX/Y
                widget.renderTooltip(context, mouseX, mouseY);
                break;
            }
        }
    }

    private void drawSkillLines(DrawContext context, int offsetX, int offsetY) {
        int lineColor = 0xFF808080;
        for (SkillWidget widget : this.skillWidgets) {
            List<Skill> requiredSkills = widget.getSkill().getRequiredSkills();
            for (Skill requiredSkill : requiredSkills) {
                SkillWidget requiredWidget = skillWidgetMap.get(requiredSkill.getId());
                if (requiredWidget != null) {
                    int startX = widget.getWorldX() + 13 + offsetX;
                    int startY = widget.getWorldY() + 13 + offsetY;
                    int endX = requiredWidget.getWorldX() + 13 + offsetX;
                    int endY = requiredWidget.getWorldY() + 13 + offsetY;
                    AltarScreen.drawLine(context, startX, startY, endX, endY, lineColor);
                }
            }
        }
    }

    public static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
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

    public void drawWindow(DrawContext context, int x, int y) {
        RenderSystem.enableBlend();
        context.drawTexture(WINDOW_TEXTURE, x, y, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        context.drawText(this.textRenderer, ALTAR_TEXT + tier, x + 8, y + 6, 4210752, false);

        if (this.client != null && this.client.player != null) {
            int currentXp = ExperienceUtils.getPlayerTotalXp(this.client.player);
            String xpText = "XP: " + currentXp;
            int textWidth = this.textRenderer.getWidth(xpText);
            int xpX = x + WINDOW_WIDTH - textWidth - 10;
            int xpY = y + 6;
            context.drawText(this.textRenderer, xpText, xpX, xpY, 0x80FF20, true);
        }
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
}