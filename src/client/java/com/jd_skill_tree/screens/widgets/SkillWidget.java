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
import java.util.Optional;

public class SkillWidget {

    // An enum to make the different states clear and manageable
    public enum SkillState {
        LOCKED,       // Cannot be unlocked
        CAN_UNLOCK,   // Requirements met
        UNLOCKED      // Already unlocked
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

        // Determine the skill's state
        SkillState state = this.getState();

        // Determine the texture v-offset based on state and hover
        int v;
        if (isMouseOver(panX, panY, mouseX, mouseY)) {
            v = 52; // Hover texture
        } else {
            switch (state) {
                case UNLOCKED:
                    v = 78; // Unlocked texture
                    break;
                case CAN_UNLOCK:
                    v = 0;  // Can-unlock texture
                    break;
                case LOCKED:
                default:
                    v = 26; // Locked texture
                    break;
            }
        }

        context.drawTexture(WIDGETS_TEXTURE, screenX, screenY, 0, v, 26, 26, 78, 104);
        context.drawItem(skill.getIcon(), screenX + 5, screenY + 5);

        if (isMouseOver(panX, panY, mouseX, mouseY)) {
            drawTooltip(context, mouseX, mouseY);
        }
    }

    /**
     * Determines the current state of the skill for the player.
     * This is the core logic for deciding how the skill is displayed.
     * @return The current SkillState.
     */
    public SkillState getState() {
        assert this.client.player != null;

        // State 1: Already unlocked
        if (ClientSkillData.isSkillUnlocked(this.skill)) {
            return SkillState.UNLOCKED;
        }

        // State 2: Check if requirements are met to unlock
        boolean hasEnoughLevels = this.client.player.experienceLevel >= this.skill.getCost();

        // Check for ALL prerequisite skills
        boolean hasAllRequiredSkills = true;
        List<Skill> requiredSkills = this.skill.getRequiredSkills();

        for (Skill requiredSkill : requiredSkills) {
            if (!ClientSkillData.isSkillUnlocked(requiredSkill)) {
                hasAllRequiredSkills = false;
                break;
            }
        }

        if (hasEnoughLevels && hasAllRequiredSkills) {
            return SkillState.CAN_UNLOCK;
        }

        // State 3: If none of the above, it's locked
        return SkillState.LOCKED;
    }

    // Pass the state to the tooltip to color the cost text
    private void drawTooltip(DrawContext context, int mouseX, int mouseY) {
        // ... (Tooltip background code is the same)
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 28;
        int tooltipWidth = 160;
        int tooltipHeight = 8 + (description.size() * 10) + 12;

        context.fillGradient(tooltipX - 3, tooltipY - 4, tooltipX + tooltipWidth + 3, tooltipY - 3, -267386864, -267386864);
        context.fillGradient(tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 4, -267386864, -267386864);
        context.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 3, -267386864, -267386864);
        context.fillGradient(tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, -267386864, -267386864);
        context.fillGradient(tooltipX + tooltipWidth + 3, tooltipY - 3, tooltipX + tooltipWidth + 4, tooltipY + tooltipHeight + 3, -267386864, -267386864);
        context.fillGradient(tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, 1347420415, 1344798847);
        context.fillGradient(tooltipX + tooltipWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 3 - 1, 1347420415, 1344798847);
        context.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + tooltipWidth + 3, tooltipY - 3 + 1, 1347420415, 1347420415);
        context.fillGradient(tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 3, 1344798847, 1344798847);

        context.drawText(this.client.textRenderer, this.title, tooltipX, tooltipY, -1, true);

        int descY = tooltipY + 12;
        for (OrderedText line : this.description) {
            context.drawText(this.client.textRenderer, line, tooltipX, descY, -1, true);
            descY += 10;
        }

        Text costText = Text.of("Cost: " + skill.getCost());
        assert client.player != null;

        // Use the skill's state to determine text color
        SkillState state = getState();
        int color = 0xFF5555; // Default to red (locked)
        if (state == SkillState.UNLOCKED) {
            color = 0x66FF55; // Green
        } else if (client.player.experienceLevel >= skill.getCost()) {
            color = 0xFFFF55; // Yellow if they have enough levels, but not prereq
        }

        context.drawText(this.client.textRenderer, costText, tooltipX, descY, color, true);
    }

    // Unchanged methods
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