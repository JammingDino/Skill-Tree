package com.jd_skill_tree.command;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class SkillCommand {

    // This provides tab-completion for skill IDs! It's a huge quality-of-life feature.
    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(SkillManager.getAllSkills().stream().map(Skill::getId), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skill")
                // Require OP level 2 (like /gamemode, /give) to use the command
                .requires(source -> source.hasPermissions(2))

                // --- /skill grant <target> <skill> ---
                .then(Commands.literal("grant")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(SkillCommand::executeGrant))))

                // --- /skill revoke <target> <skill> ---
                .then(Commands.literal("revoke")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(SkillCommand::executeRevoke))))

                // --- /skill list <target> ---
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(SkillCommand::executeList)))
        );
    }

    private static int executeGrant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        ResourceLocation skillId = ResourceLocationArgument.getId(context, "skill");
        CommandSourceStack source = context.getSource();

        // Validate that the skill exists
        Optional<Skill> skillOpt = SkillManager.getSkill(skillId);
        if (skillOpt.isEmpty()) {
            source.sendFailure(Component.literal("Unknown skill: ").append(Component.literal(skillId.toString()).withStyle(ChatFormatting.RED)));
            return 0;
        }

        IUnlockedSkillsData skillData = (IUnlockedSkillsData) target;
        if (skillData.hasSkill(skillId.toString())) {
            source.sendFailure(Component.literal(target.getName().getString() + " already has the skill ").append(Component.literal(skillId.toString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        skillData.unlockSkill(skillId.toString());
        SkillNetworking.syncSkillsToClient(target); // Sync changes to the player!
        source.sendSuccess(() -> Component.literal("Granted skill ").append(Component.literal(skillId.toString()).withStyle(ChatFormatting.GREEN)).append(" to " + target.getName().getString()), true);
        return 1;
    }

    private static int executeRevoke(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        ResourceLocation skillId = ResourceLocationArgument.getId(context, "skill");
        CommandSourceStack source = context.getSource();

        if (SkillManager.getSkill(skillId).isEmpty()) {
            source.sendFailure(Component.literal("Unknown skill: ").append(Component.literal(skillId.toString()).withStyle(ChatFormatting.RED)));
            return 0;
        }

        IUnlockedSkillsData skillData = (IUnlockedSkillsData) target;
        if (!skillData.hasSkill(skillId.toString())) {
            source.sendFailure(Component.literal(target.getName().getString() + " does not have the skill ").append(Component.literal(skillId.toString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        skillData.revokeSkill(skillId.toString());
        SkillNetworking.syncSkillsToClient(target);
        source.sendSuccess(() -> Component.literal("Revoked skill ").append(Component.literal(skillId.toString()).withStyle(ChatFormatting.RED)).append(" from " + target.getName().getString()), true);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        CommandSourceStack source = context.getSource();

        Set<String> unlockedSkills = ((IUnlockedSkillsData) target).getUnlockedSkills();

        if (unlockedSkills.isEmpty()) {
            source.sendSuccess(() -> Component.literal(target.getName().getString() + " has no unlocked skills."), false);
        } else {
            source.sendSuccess(() -> Component.literal("--- Skills for " + target.getName().getString() + " ---").withStyle(ChatFormatting.GRAY), false);
            unlockedSkills.forEach(skillId -> {
                source.sendSuccess(() -> Component.literal("- ").append(Component.literal(skillId).withStyle(ChatFormatting.GREEN)), false);
            });
        }
        return unlockedSkills.size();
    }
}