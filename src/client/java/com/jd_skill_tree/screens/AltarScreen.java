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
    private static final int RADIAL_SPACING = 50; // Distance between skill tiers
    private static final double CHILD_ANGLE_SPREAD = Math.PI / 6; // 30 degrees spread per child
    private static final int MIN_NODE_DISTANCE = 40; // Minimum distance between any two nodes

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

        // Calculate positions for all skills
        calculateCircularPositions(skillNodes);

        // --- CALCULATE TREE BOUNDARIES ---
        calculateTreeBounds();

        // --- CENTER THE INITIAL VIEW ---
        // Calculate the absolute center of the UI window
        double screenCenterX = x + WINDOW_WIDTH / 2.0;
        double screenCenterY = y + WINDOW_HEIGHT / 2.0;

        // We want the logic coordinate (0,0) to be at screenCenterX, screenCenterY.
        // Since widgets are drawn from their top-left corner, and appear to be roughly 26x26,
        // we offset by half the widget size (13) to make the visual center of the node
        // land exactly on the center of the screen.
        this.panX = screenCenterX - 13;
        this.panY = screenCenterY - 9;

        // IMPORTANT: Do NOT call clampPan() here.
        // If the tree is unbalanced (e.g., only grows to the right), clampPan()
        // would immediately detect that (0,0) is at the edge of the bounds
        // and shove the camera over to fit the nodes, undoing your centering work.
        // clampPan();
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
        if (!isMouseInWindow((int)mouseX, (int)mouseY)) return false;

        double worldMouseX = (mouseX - this.panX) / this.scale;
        double worldMouseY = (mouseY - this.panY) / this.scale;

        for (SkillWidget widget : this.skillWidgets) {
            if (widget.mouseClicked(worldMouseX, worldMouseY, button, 0, 0)) {
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) { // In 1.20.2+ use double verticalAmount
        // Calculate the new scale
        double newScale = this.scale + (amount * ZOOM_SENSITIVITY);
        newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

        if (newScale != this.scale) {
            // 1. Calculate where the mouse is in the "World" (relative to tree origin) before zooming
            double worldMouseX = (mouseX - this.panX) / this.scale;
            double worldMouseY = (mouseY - this.panY) / this.scale;

            // 2. Apply the new scale
            this.scale = newScale;

            // 3. Recalculate panX/Y so the mouse remains over the same "World" point
            this.panX = mouseX - (worldMouseX * this.scale);
            this.panY = mouseY - (worldMouseY * this.scale);

            // Optional: Re-clamp if you want to prevent zooming the tree off-screen
            // clampPan();

            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount); // or verticalAmount
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

        // 1. Enable Scissor (Keep the drawing inside the window)
        context.enableScissor(x + 9, y + 17, x + WINDOW_WIDTH - 9, y + WINDOW_HEIGHT - 9);

        this.renderBackground(context);

        // --- START MATRIX TRANSFORMATION ---
        context.getMatrices().push();

        // A. Translate to the Pan location (The camera position)
        context.getMatrices().translate(this.panX, this.panY, 0);

        // B. Scale the view
        context.getMatrices().scale((float)this.scale, (float)this.scale, 1.0f);

        // 2. Draw Lines
        // Note: pass '0' for offset because the Matrix handles panX/panY now
        drawSkillLines(context, 0, 0);

        // 3. Draw Widgets
        for (SkillWidget widget : this.skillWidgets) {
            // Transform mouse BACK to world space for hover effects inside the widget render
            // (Only if your widget uses mouseX/Y for visual hover states during render)
            int worldMouseX = (int)((mouseX - this.panX) / this.scale);
            int worldMouseY = (int)((mouseY - this.panY) / this.scale);

            // Pass 0,0 as x/y offset, let the Matrix translate it
            widget.render(context, 0, 0, worldMouseX, worldMouseY, delta);
        }

        // --- END MATRIX TRANSFORMATION ---
        context.getMatrices().pop();

        // 4. Disable Scissor
        context.disableScissor();

        // 5. Draw Tooltips (Unscaled, top layer)
        drawTooltips(context, mouseX, mouseY);

        // super.render removed to prevent double rendering if you handled everything above
        // super.render(context, mouseX, mouseY, delta);
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

    // Helper to ensure we don't show tooltips if the mouse is outside the main window area
    private boolean isMouseInWindow(int mouseX, int mouseY) {
        return mouseX >= x + 10 && mouseX <= x + WINDOW_WIDTH - 10 &&
                mouseY >= y + 2 && mouseY <= y + WINDOW_HEIGHT - 10;
    }

    // Change signature to accept offsets
    private void drawSkillLines(DrawContext context, int offsetX, int offsetY) {
        int lineColor = 0xFF808080;
        for (SkillWidget widget : this.skillWidgets) {
            List<Skill> requiredSkills = widget.getSkill().getRequiredSkills();
            for (Skill requiredSkill : requiredSkills) {
                SkillWidget requiredWidget = skillWidgetMap.get(requiredSkill.getId());
                if (requiredWidget != null) {
                    // Do NOT add this.panX here.
                    // We rely on the passed offsetX (which is 0) and the MatrixStack.
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

        // Draw Title
        context.drawText(this.textRenderer, ALTAR_TEXT + tier, x + 8, y + 6, 4210752, false);

        // --- NEW: Draw Current XP Points ---
        if (this.client != null && this.client.player != null) {
            int currentXp = ExperienceUtils.getPlayerTotalXp(this.client.player);
            String xpText = "XP: " + currentXp;

            int textWidth = this.textRenderer.getWidth(xpText);
            // Position: Top right of the window, inside the border
            int xpX = x + WINDOW_WIDTH - textWidth - 10;
            int xpY = y + 6;

            // Draw in a nice Experience Green color
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