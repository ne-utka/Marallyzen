package neutka.marallys.marallyzen.goals;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/**
 * /marallyzen goal ...
 */
public final class GoalCommand {
    private GoalCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("goal")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("add")
                        .then(Commands.argument("script_id", StringArgumentType.string())
                                .suggests(GoalCommand::suggestScriptIdsForAdd)
                                .executes(GoalCommand::addGoal)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("script_id", StringArgumentType.string())
                                .suggests(GoalCommand::suggestScriptIdsForRemove)
                                .executes(GoalCommand::removeGoal)));
    }

    private static CompletableFuture<Suggestions> suggestScriptIdsForAdd(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        GoalScriptLoader loader = GoalScriptLoader.getInstance();
        loader.ensureLoaded();
        Set<String> ids = new TreeSet<>();
        ids.addAll(loader.getLoadedScriptIds());
        ids.addAll(loader.discoverScriptIds());
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestScriptIdsForRemove(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                GoalProgressEngine.getInstance().getPersistedGoalIds(),
                builder
        );
    }

    private static int addGoal(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String scriptId = StringArgumentType.getString(context, "script_id");
        if (!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be executed by a player."));
            return 0;
        }
        GoalProgressEngine.GoalOperationResult result = GoalProgressEngine.getInstance().addGoal(player, scriptId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int removeGoal(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String scriptId = StringArgumentType.getString(context, "script_id");
        GoalProgressEngine.GoalOperationResult result = GoalProgressEngine.getInstance().removeGoal(scriptId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }
}
