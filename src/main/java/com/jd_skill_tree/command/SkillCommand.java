package com.jd_skill_tree.command;

import com.jd_skill_tree.api.IUnlockedSkillsData;
import com.jd_skill_tree.networking.SkillNetworking;
import com.jd_skill_tree.skills.Skill;
import com.jd_skill_tree.skills.SkillManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class SkillCommand {

    // This provides tab-completion for skill IDs! It's a huge quality-of-life feature.
    private static final SuggestionProvider<ServerCommandSource> SKILL_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestIdentifiers(SkillManager.getAllSkills().stream().map(Skill::getId), builder);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("skill")
                // Require OP level 2 (like /gamemode, /give) to use the command
                .requires(source -> source.hasPermissionLevel(2))

                // --- /skill grant <target> <skill> ---
                .then(CommandManager.literal("grant")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("skill", IdentifierArgumentType.identifier())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(SkillCommand::executeGrant))))

                // --- /skill revoke <target> <skill> ---
                .then(CommandManager.literal("revoke")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("skill", IdentifierArgumentType.identifier())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(SkillCommand::executeRevoke))))

                // --- /skill list <target> ---
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(SkillCommand::executeList)))
        );
    }

    private static int executeGrant(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        Identifier skillId = IdentifierArgumentType.getIdentifier(context, "skill");
        ServerCommandSource source = context.getSource();

        // Validate that the skill exists
        Optional<Skill> skillOpt = SkillManager.getSkill(skillId);
        if (skillOpt.isEmpty()) {
            source.sendError(Text.literal("Unknown skill: ").append(Text.literal(skillId.toString()).formatted(Formatting.RED)));
            return 0;
        }

        IUnlockedSkillsData skillData = (IUnlockedSkillsData) target;
        if (skillData.hasSkill(skillId.toString())) {
            source.sendError(Text.literal(target.getName().getString() + " already has the skill ").append(Text.literal(skillId.toString()).formatted(Formatting.YELLOW)));
            return 0;
        }

        skillData.unlockSkill(skillId.toString());
        SkillNetworking.syncSkillsToClient(target); // Sync changes to the player!
        source.sendFeedback(() -> Text.literal("Granted skill ").append(Text.literal(skillId.toString()).formatted(Formatting.GREEN)).append(" to " + target.getName().getString()), true);
        return 1;
    }

    private static int executeRevoke(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        Identifier skillId = IdentifierArgumentType.getIdentifier(context, "skill");
        ServerCommandSource source = context.getSource();

        if (SkillManager.getSkill(skillId).isEmpty()) {
            source.sendError(Text.literal("Unknown skill: ").append(Text.literal(skillId.toString()).formatted(Formatting.RED)));
            return 0;
        }

        IUnlockedSkillsData skillData = (IUnlockedSkillsData) target;
        if (!skillData.hasSkill(skillId.toString())) {
            source.sendError(Text.literal(target.getName().getString() + " does not have the skill ").append(Text.literal(skillId.toString()).formatted(Formatting.YELLOW)));
            return 0;
        }

        skillData.revokeSkill(skillId.toString());
        SkillNetworking.syncSkillsToClient(target);
        source.sendFeedback(() -> Text.literal("Revoked skill ").append(Text.literal(skillId.toString()).formatted(Formatting.RED)).append(" from " + target.getName().getString()), true);
        return 1;
    }

    private static int executeList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        ServerCommandSource source = context.getSource();

        Set<String> unlockedSkills = ((IUnlockedSkillsData) target).getUnlockedSkills();

        if (unlockedSkills.isEmpty()) {
            source.sendFeedback(() -> Text.literal(target.getName().getString() + " has no unlocked skills."), false);
        } else {
            source.sendFeedback(() -> Text.literal("--- Skills for " + target.getName().getString() + " ---").formatted(Formatting.GRAY), false);
            unlockedSkills.forEach(skillId -> {
                source.sendFeedback(() -> Text.literal("- ").append(Text.literal(skillId).formatted(Formatting.GREEN)), false);
            });
        }
        return unlockedSkills.size();
    }
}