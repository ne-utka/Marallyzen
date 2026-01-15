package neutka.marallys.marallyzen.denizen.commands;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.MarallyzenScriptEntryData;
import neutka.marallys.marallyzen.denizen.objects.PlayerTag;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CommandScriptRegistry {
    private CommandScriptRegistry() {
    }

    private static final Map<String, CommandScriptContainer> commandScripts = new HashMap<>();
    private static final Set<String> registered = new HashSet<>();
    private static CommandDispatcher<CommandSourceStack> dispatcher;

    static void add(CommandScriptContainer container) {
        String name = container.getCommandName();
        if (name != null) {
            commandScripts.put(name, container);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        dispatcher = event.getDispatcher();
        refresh();
    }

    public static void refresh() {
        if (dispatcher == null) {
            Marallyzen.LOGGER.warn("CommandScriptRegistry refresh skipped: dispatcher not ready.");
            return;
        }
        for (var entry : ScriptRegistry.scriptContainers.values()) {
            if (entry instanceof CommandScriptContainer container) {
                String name = container.getCommandName();
                if (name != null) {
                    commandScripts.put(name, container);
                }
            }
        }
        registerAll();
        syncCommandsToPlayers();
        Marallyzen.LOGGER.info("CommandScriptRegistry refresh: {} command script(s), {} registered.", commandScripts.size(), registered.size());
    }

    private static void registerAll() {
        if (dispatcher == null) {
            return;
        }
        for (CommandScriptContainer container : commandScripts.values()) {
            String name = container.getCommandName();
            if (name == null || registered.contains(name)) {
                continue;
            }
            LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(name)
                .executes(ctx -> runCommand(ctx, container, "", name))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> suggest(ctx, builder, container, name))
                    .executes(ctx -> runCommand(ctx, container, StringArgumentType.getString(ctx, "args"), name)));
            dispatcher.register(literal);
            registered.add(name);
            Marallyzen.LOGGER.info("Registered command script '/{}' from container {}", name, container.getName());
        }
    }

    private static void syncCommandsToPlayers() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    private static int runCommand(CommandContext<CommandSourceStack> ctx, CommandScriptContainer container, String rawArgs, String alias) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        MarallyzenScriptEntryData data = new MarallyzenScriptEntryData();
        data.setPlayer(new PlayerTag(player));
        List<String> args = splitArgs(rawArgs);
        Map<String, ObjectTag> contexts = buildCommandContexts(args, rawArgs, alias);
        var entries = container.getCommandEntries(data);
        if (entries == null) {
            return 0;
        }
        InstantQueue queue = new InstantQueue(container.getName());
        queue.addEntries(entries);
        if (contexts != null) {
            ContextSource.SimpleMap src = new ContextSource.SimpleMap();
            src.contexts = contexts;
            queue.setContextSource(src);
        }
        queue.start();
        return 1;
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(CommandContext<CommandSourceStack> ctx,
                                                                                           SuggestionsBuilder builder,
                                                                                           CommandScriptContainer container,
                                                                                           String alias) {
        String remaining = builder.getRemaining();
        List<String> args = splitArgs(remaining);
        Map<String, ObjectTag> contexts = buildCommandContexts(args, remaining, alias);
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }
        MarallyzenScriptEntryData data = new MarallyzenScriptEntryData();
        data.setPlayer(new PlayerTag(player));
        List<String> suggestions = container.runTabCompleteProcedure(data, contexts);
        String lower = remaining.toLowerCase(Locale.ROOT);
        for (String suggestion : suggestions) {
            if (lower.isEmpty() || suggestion.toLowerCase(Locale.ROOT).startsWith(lower)) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }

    private static Map<String, ObjectTag> buildCommandContexts(List<String> args, String rawArgs, String alias) {
        Map<String, ObjectTag> contexts = new HashMap<>();
        ListTag list = new ListTag();
        for (String arg : args) {
            list.addObject(new ElementTag(arg, true));
        }
        contexts.put("args", list);
        contexts.put("raw_args", new ElementTag(rawArgs == null ? "" : rawArgs, true));
        contexts.put("alias", new ElementTag(alias, true));
        return contexts;
    }

    private static List<String> splitArgs(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank()) {
            return List.of();
        }
        String[] parts = rawArgs.trim().split("\\s+");
        return Arrays.asList(parts);
    }
}
