package com.jd_skill_tree.screens;

import com.jd_skill_tree.networking.SkillNetworking;
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
    private static final int RADIAL_SPACING = 50; // Distance between skill tiers
    private static final double CHILD_ANGLE_SPREAD = Math.PI / 6; // 30 degrees spread per child
    private static final int MIN_NODE_DISTANCE = 40; // Minimum distance between any two nodes

    private double panX = 0;
    private double panY = 0;
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

        // Organize skills by their tree structure
        Map<Identifier, SkillNode> skillNodes = buildSkillTree(skillsToDisplay);

        // Calculate positions for all skills
        calculateCircularPositions(skillNodes);

        // --- CALCULATE TREE BOUNDARIES ---
        calculateTreeBounds();

        // --- CENTER THE INITIAL VIEW ---
        int treeWidth = this.treeMaxX - this.treeMinX;
        int treeHeight = this.treeMaxY - this.treeMinY;
        x = (this.width - WINDOW_WIDTH) / 2;
        y = (this.height - WINDOW_HEIGHT) / 2;
        this.panX = (x + WINDOW_WIDTH / 2.0) - (this.treeMinX + treeWidth / 2.0);
        this.panY = (y + WINDOW_HEIGHT / 2.0) - (this.treeMinY + treeHeight / 2.0);

        clampPan();
    }

    /**
     * Represents a node in the skill tree with position and hierarchy info
     */
    private static class SkillNode {
        Skill skill;
        int depth; // Distance from root (0 for root skills)
        List<SkillNode> children = new ArrayList<>();
        List<SkillNode> parents = new ArrayList<>(); // Changed to support multiple parents
        int x, y; // Calculated position
        double angle; // Angle from parent (in radians)

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

        // Create nodes for all skills
        for (Skill skill : skills) {
            nodes.put(skill.getId(), new SkillNode(skill));
        }

        // Link parents and children (now supports multiple parents)
        for (SkillNode node : nodes.values()) {
            List<Skill> requiredSkills = node.skill.getRequiredSkills();

            if (!requiredSkills.isEmpty()) {
                // This skill has prerequisites
                for (Skill requiredSkill : requiredSkills) {
                    SkillNode parent = nodes.get(requiredSkill.getId());
                    if (parent != null) {
                        node.parents.add(parent);
                        parent.children.add(node);
                    }
                }
            } else {
                // This is a root skill
                rootNodes.add(node);
            }
        }

        // Calculate depth for each node
        calculateDepths(rootNodes);

        return nodes;
    }

    /**
     * Calculate the depth of each node (distance from root)
     */
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
     * Calculate circular positions for all skills
     */
    private void calculateCircularPositions(Map<Identifier, SkillNode> nodes) {
        // Find root nodes
        List<SkillNode> rootNodes = new ArrayList<>();
        for (SkillNode node : nodes.values()) {
            if (node.parents.isEmpty()) {
                rootNodes.add(node);
            }
        }

        // Position root nodes in a circle around the center
        int numRoots = rootNodes.size();
        for (int i = 0; i < numRoots; i++) {
            SkillNode root = rootNodes.get(i);
            double angle = (2 * Math.PI * i) / numRoots;

            // Place roots at the first radius level
            root.x = (int)(Math.cos(angle) * RADIAL_SPACING);
            root.y = (int)(Math.sin(angle) * RADIAL_SPACING);
            root.angle = angle;
            root.depth = 0;

            // Create widget for root
            SkillWidget widget = new SkillWidget(root.skill, root.x, root.y, this);
            this.skillWidgets.add(widget);
            this.skillWidgetMap.put(root.skill.getId(), widget);
        }

        // Position all other nodes level by level
        Set<SkillNode> positioned = new HashSet<>(rootNodes);
        boolean changed = true;

        while (changed) {
            changed = false;

            for (SkillNode node : nodes.values()) {
                if (positioned.contains(node)) {
                    continue;
                }

                // Check if all parents are positioned
                boolean allParentsPositioned = true;
                for (SkillNode parent : node.parents) {
                    if (!positioned.contains(parent)) {
                        allParentsPositioned = false;
                        break;
                    }
                }

                if (allParentsPositioned && !node.parents.isEmpty()) {
                    positionNodeWithMultipleParents(node, positioned);
                    positioned.add(node);

                    // Create widget
                    SkillWidget widget = new SkillWidget(node.skill, node.x, node.y, this);
                    this.skillWidgets.add(widget);
                    this.skillWidgetMap.put(node.skill.getId(), widget);

                    changed = true;
                }
            }
        }
    }

    /**
     * Position a node that may have multiple parents
     */
    private void positionNodeWithMultipleParents(SkillNode node, Set<SkillNode> positioned) {
        if (node.parents.isEmpty()) {
            return;
        }

        if (node.parents.size() == 1) {
            // Single parent - position relative to it
            SkillNode parent = node.parents.get(0);
            node.depth = parent.depth + 1;

            // Find all siblings (other children of the same parent that share only this parent)
            List<SkillNode> siblings = new ArrayList<>();
            for (SkillNode child : parent.children) {
                if (child.parents.size() == 1) {
                    siblings.add(child);
                }
            }

            int siblingIndex = siblings.indexOf(node);
            int numSiblings = siblings.size();

            if (numSiblings == 1) {
                // Only child - place directly in line
                node.angle = parent.angle;
            } else {
                // Multiple siblings - spread them out
                double depthFactor = 1.0 / Math.max(1, parent.depth * 0.5);
                double angularSpread = CHILD_ANGLE_SPREAD * depthFactor;
                double totalSpread = angularSpread * Math.min(numSiblings - 1, 4);
                double offset = totalSpread * ((siblingIndex - (numSiblings - 1) / 2.0) / Math.max(1, numSiblings - 1));
                node.angle = parent.angle + offset;
            }

            node.x = parent.x + (int)(Math.cos(node.angle) * RADIAL_SPACING);
            node.y = parent.y + (int)(Math.sin(node.angle) * RADIAL_SPACING);

        } else {
            // Multiple parents - calculate position between them
            // Find the deepest parent to determine our depth
            int maxParentDepth = 0;
            for (SkillNode parent : node.parents) {
                maxParentDepth = Math.max(maxParentDepth, parent.depth);
            }
            node.depth = maxParentDepth + 1;

            // Calculate centroid of all parents
            double centroidX = 0;
            double centroidY = 0;

            for (SkillNode parent : node.parents) {
                centroidX += parent.x;
                centroidY += parent.y;
            }

            centroidX /= node.parents.size();
            centroidY /= node.parents.size();

            // Calculate angle from origin to centroid
            double centroidAngle = Math.atan2(centroidY, centroidX);
            node.angle = centroidAngle;

            // Position the node further out from the centroid
            // Use a multiplier to ensure it's clearly beyond the parents
            double distanceFromCentroid = RADIAL_SPACING * 1.2;

            node.x = (int)(centroidX + Math.cos(node.angle) * distanceFromCentroid);
            node.y = (int)(centroidY + Math.sin(node.angle) * distanceFromCentroid);

            // Check for overlaps and adjust if necessary
            resolveOverlap(node, positioned);
        }
    }

    /**
     * Check if a node overlaps with any positioned nodes and adjust position if needed
     */
    private void resolveOverlap(SkillNode node, Set<SkillNode> positioned) {
        boolean hasOverlap = true;
        int maxAttempts = 36; // Try 36 different angles (10 degrees each)
        int attempt = 0;

        while (hasOverlap && attempt < maxAttempts) {
            hasOverlap = false;

            for (SkillNode other : positioned) {
                double dist = Math.sqrt(Math.pow(node.x - other.x, 2) + Math.pow(node.y - other.y, 2));

                if (dist < MIN_NODE_DISTANCE) {
                    hasOverlap = true;

                    // Rotate the node slightly and try again
                    double angleAdjust = Math.PI / 18; // 10 degrees
                    node.angle += angleAdjust;

                    // Recalculate position with new angle
                    double centroidX = 0;
                    double centroidY = 0;
                    for (SkillNode parent : node.parents) {
                        centroidX += parent.x;
                        centroidY += parent.y;
                    }
                    centroidX /= node.parents.size();
                    centroidY /= node.parents.size();

                    double distanceFromCentroid = RADIAL_SPACING * 1.2;
                    node.x = (int)(centroidX + Math.cos(node.angle) * distanceFromCentroid);
                    node.y = (int)(centroidY + Math.sin(node.angle) * distanceFromCentroid);

                    attempt++;
                    break;
                }
            }
        }
    }

    /**
     * Recursively position child nodes in a circular pattern
     * NOTE: This is now only used internally and has been largely replaced by
     * positionNodeWithMultipleParents for the main positioning logic
     */
    private void positionChildren(SkillNode parent) {
        if (parent.children.isEmpty()) {
            return;
        }

        int numChildren = parent.children.size();

        // Calculate angular span for children based on parent's angle
        double baseAngle = parent.angle;

        // Dynamic angular spread based on number of children and depth
        // Deeper nodes get tighter spacing to prevent extreme spreading
        double depthFactor = 1.0 / Math.max(1, parent.depth * 0.5);
        double angularSpread = CHILD_ANGLE_SPREAD * depthFactor;

        // If there's only one child, place it directly in line with parent
        if (numChildren == 1) {
            SkillNode child = parent.children.get(0);
            child.angle = baseAngle;
            // Position relative to parent, not origin
            child.x = parent.x + (int)(Math.cos(child.angle) * RADIAL_SPACING);
            child.y = parent.y + (int)(Math.sin(child.angle) * RADIAL_SPACING);
        } else {
            // Distribute children evenly within the angular spread
            // Total spread increases with more children
            double totalSpread = angularSpread * Math.min(numChildren - 1, 4);

            for (int i = 0; i < numChildren; i++) {
                SkillNode child = parent.children.get(i);
                // Center the spread around the parent's angle
                double offset = totalSpread * ((i - (numChildren - 1) / 2.0) / Math.max(1, numChildren - 1));
                child.angle = baseAngle + offset;

                // Position relative to parent, not origin
                child.x = parent.x + (int)(Math.cos(child.angle) * RADIAL_SPACING);
                child.y = parent.y + (int)(Math.sin(child.angle) * RADIAL_SPACING);
            }
        }

        // Create widgets for children and recurse
        for (SkillNode child : parent.children) {
            SkillWidget widget = new SkillWidget(child.skill, child.x, child.y, this);
            this.skillWidgets.add(widget);
            this.skillWidgetMap.put(child.skill.getId(), widget);

            positionChildren(child);
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
        for (SkillWidget widget : this.skillWidgets) {
            if (widget.mouseClicked(mouseX, mouseY, button, (int)this.panX, (int)this.panY)) {
                // CHANGED: Compare the skill's ID to our "reset_skills" identifier.
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

    private void sendUnlockRequest(Skill skill) {
        System.out.println("CLIENT: Sending unlock request for skill: " + skill.getId());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeIdentifier(skill.getId());
        // CORRECTED: Reference the ID from the SkillNetworking class
        ClientPlayNetworking.send(SkillNetworking.UNLOCK_SKILL_PACKET_ID, buf);
    }

    private void sendResetRequest() {
        System.out.println("CLIENT: Sending skill reset request.");
        PacketByteBuf buf = PacketByteBufs.create();
        // CORRECTED: Reference the ID from the SkillNetworking class
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

        // 1. Enable Scissor (Clips content to the window)
        context.enableScissor(x + 9, y + 17, x + WINDOW_WIDTH - 9, y + WINDOW_HEIGHT - 9);

        this.renderBackground(context);

        // 2. Draw Lines (Behind widgets)
        drawSkillLines(context);

        // 3. Draw Widgets (Frames and Icons only)
        for (SkillWidget widget : this.skillWidgets) {
            widget.render(context, (int)this.panX, (int)this.panY, mouseX, mouseY, delta);
        }

        // 4. Disable Scissor (Allow drawing outside the window bounds)
        context.disableScissor();

        // 5. Draw Tooltips (Top layer, outside clip)
        // We assume only one widget can be hovered at a time.
        for (SkillWidget widget : this.skillWidgets) {
            if (widget.isMouseOver((int)this.panX, (int)this.panY, mouseX, mouseY)) {
                // Determine if the mouse is actually within the window bounds
                // This prevents tooltips from appearing if the widget is hidden by the scissor logic
                // but the mouse is technically over its coordinates.
                if (isMouseInWindow(mouseX, mouseY)) {
                    widget.renderTooltip(context, mouseX, mouseY);
                }
                break; // Stop after finding the hovered widget
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // Helper to ensure we don't show tooltips if the mouse is outside the main window area
    private boolean isMouseInWindow(int mouseX, int mouseY) {
        return mouseX >= x + 10 && mouseX <= x + WINDOW_WIDTH - 10 &&
                mouseY >= y + 2 && mouseY <= y + WINDOW_HEIGHT - 10;
    }

    private void drawSkillLines(DrawContext context) {
        int lineColor = 0xFF808080;
        for (SkillWidget widget : this.skillWidgets) {
            // Draw lines to ALL required skills
            List<Skill> requiredSkills = widget.getSkill().getRequiredSkills();
            for (Skill requiredSkill : requiredSkills) {
                SkillWidget requiredWidget = skillWidgetMap.get(requiredSkill.getId());
                if (requiredWidget != null) {
                    int startX = widget.getWorldX() + 13 + (int)this.panX;
                    int startY = widget.getWorldY() + 13 + (int)this.panY;
                    int endX = requiredWidget.getWorldX() + 13 + (int)this.panX;
                    int endY = requiredWidget.getWorldY() + 13 + (int)this.panY;

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