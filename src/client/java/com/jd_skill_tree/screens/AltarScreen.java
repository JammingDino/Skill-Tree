package com.jd_skill_tree.screens;

import com.jd_skill_tree.screens.widgets.SkillWidget;
import com.jd_skill_tree.skills.ModSkills;
import com.jd_skill_tree.skills.Skill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class AltarScreen extends Screen {

    private static final Identifier WINDOW_TEXTURE = new Identifier("textures/gui/advancements/window.png");
    private static final String ALTAR_TEXT = "Skill Altar Tier ";
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 140;
    private static int tier = 0;

    public static int x = 0;
    public static int y = 0;

    private final List<SkillWidget> skillWidgets = new ArrayList<>();

    public AltarScreen(Text title, int Tier) {
        super(title);
        tier = Tier;
    }

    @Override
    protected void init() {
        // TODO add all of required elements that make the skill tree a skill tree
        this.skillWidgets.clear(); // Clear the list on re-init
        List<Skill> skillsToDisplay = ModSkills.getSkillsForTier(tier);

        int startX = this.width / 2 - (skillsToDisplay.size() * 30 / 2); // Center the skills
        int startY = this.height / 2 - 13; // Vertically centered

        for (int i = 0; i < skillsToDisplay.size(); i++) {
            Skill skill = skillsToDisplay.get(i);
            this.skillWidgets.add(new SkillWidget(skill, startX + (i * 30), startY, this));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        x = (this.width - WINDOW_WIDTH) / 2;
        y = (this.height - WINDOW_HEIGHT) / 2;

        this.renderBackground(context);
        this.drawWindow(context, x, y);
        super.render(context, mouseX, mouseY, delta);

        for (SkillWidget widget : this.skillWidgets) {
            widget.render(context, mouseX, mouseY, delta);
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
            context.fillGradient(x+5, y+5, x+WINDOW_WIDTH-5, y+WINDOW_HEIGHT-5, -1072689136, -804253680);
        } else {
            this.renderBackgroundTexture(context);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
