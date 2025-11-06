package com.jd_skill_tree.screens.widgets;

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

    private static final Identifier WIDGETS_TEXTURE = new Identifier("jd_skill_tree", "textures/gui/widgets.png");
    private final Skill skill;
    private final int x;
    private final int y;
    private final Screen parentScreen;
    private final MinecraftClient client;
    private final OrderedText title;
    private final List<OrderedText> description;


    public SkillWidget(Skill skill, int x, int y, Screen parentScreen) {
        this.skill = skill;
        this.x = x;
        this.y = y;
        this.parentScreen = parentScreen;
        this.client = MinecraftClient.getInstance();

        // Prepare the text for rendering
        this.title = Text.of(skill.getTitle()).asOrderedText();
        // Split the description into multiple lines if it's too long
        this.description = this.client.textRenderer.wrapLines(Text.of(skill.getDescription()), 150); // 150 is the max width
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.enableBlend();

        assert client.player != null;
        boolean playerUnlocked = client.player.experienceLevel > skill.getCost();

        // Determine the texture based on whether the skill is unlocked
        int v = 0;
        if (isMouseOver(mouseX, mouseY)) {
            v = 52;
        } else if (playerUnlocked) {
            v = 0;
        } else  {
            v = 26;
        }

        // Draw the background frame for the skill
        // We're borrowing the advancement widget texture here. You could create your own.
        context.drawTexture(WIDGETS_TEXTURE, this.x, this.y, 0, v, 26, 26, 78, 104);

        // Draw the skill's icon
        context.drawItem(skill.getIcon(), this.x + 5, this.y + 5);

        // If the mouse is hovering over the widget, draw the tooltip
        if (isMouseOver(mouseX, mouseY)) {
            drawTooltip(context, mouseX, mouseY);
        }
    }

    private void drawTooltip(DrawContext context, int mouseX, int mouseY) {
        // Calculate tooltip position
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 28;
        int tooltipWidth = 160; // Max width
        int tooltipHeight = 8 + (description.size() * 10) + 12; // Adjusted for title and cost

        // Render the tooltip background
        context.fillGradient(tooltipX - 3, tooltipY - 4, tooltipX + tooltipWidth + 3, tooltipY - 3, -267386864, -267386864);
        context.fillGradient(tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 4, -267386864, -267386864);
        context.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 3, -267386864, -267386864);
        context.fillGradient(tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, -267386864, -267386864);
        context.fillGradient(tooltipX + tooltipWidth + 3, tooltipY - 3, tooltipX + tooltipWidth + 4, tooltipY + tooltipHeight + 3, -267386864, -267386864);
        context.fillGradient(tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, 1347420415, 1344798847);
        context.fillGradient(tooltipX + tooltipWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 3 - 1, 1347420415, 1344798847);
        context.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + tooltipWidth + 3, tooltipY - 3 + 1, 1347420415, 1347420415);
        context.fillGradient(tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 3, 1344798847, 1344798847);

        // Draw skill title
        context.drawText(this.client.textRenderer, this.title, tooltipX, tooltipY, -1, true);

        // Draw skill description
        int descY = tooltipY + 12;
        for (OrderedText line : this.description) {
            context.drawText(this.client.textRenderer, line, tooltipX, descY, -1, true);
            descY += 10;
        }

        // Draw skill cost
        Text costText = Text.of("Cost: " + skill.getCost());
        assert client.player != null;
        boolean playerUnlocked = client.player.experienceLevel > skill.getCost();
        if (playerUnlocked) { // Green if unlocked, red if not
            context.drawText(this.client.textRenderer, costText, tooltipX, descY, 0x66FF55, true);
        } else {
            context.drawText(this.client.textRenderer, costText, tooltipX, descY, 0xFF5555, true);
        };
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX <= this.x + 26 &&
                mouseY >= this.y && mouseY <= this.y + 26;
    }

    // You can add a click handler here if you want to make the widgets clickable
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver((int)mouseX, (int)mouseY)) {
            // Handle the click event (e.g., attempt to unlock the skill)
            System.out.println("Clicked on skill: " + skill.getTitle());
            return true;
        }
        return false;
    }
}