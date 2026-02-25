package neutka.marallys.marallyzen.trigger.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import neutka.marallys.marallyzen.trigger.TriggerModule;
import neutka.marallys.marallyzen.trigger.TriggerModuleRegistry;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public final class TriggerCommand {
    private static final List<String> TRIGGER_TYPES = List.of("block_use", "redstone_power", "pressure_plate", "zone_enter", "manual", "timer");

    private TriggerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("trigger")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("wand")
                        .executes(TriggerCommand::giveWand))
                .then(Commands.literal("save")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(TriggerCommand::saveBlueprint)))
                .then(Commands.literal("bind")
                        .then(Commands.argument("blueprint_id", StringArgumentType.string())
                                .suggests(TriggerCommand::suggestBlueprintIds)
                                .executes(context -> armBind(context, "block_use"))
                                .then(Commands.argument("trigger_type", StringArgumentType.string())
                                        .suggests(TriggerCommand::suggestTriggerTypes)
                                        .executes(TriggerCommand::armBind))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("instance_id", StringArgumentType.string())
                                .suggests(TriggerCommand::suggestInstanceIds)
                                .executes(TriggerCommand::removeInstance)))
                .then(Commands.literal("activate")
                        .then(Commands.argument("instance_id", StringArgumentType.string())
                                .suggests(TriggerCommand::suggestInstanceIds)
                                .executes(TriggerCommand::activateInstance)))
                .then(Commands.literal("edit")
                        .then(Commands.argument("instance_id", StringArgumentType.string())
                                .suggests(TriggerCommand::suggestInstanceIds)
                                .then(Commands.literal("trigger")
                                        .then(Commands.argument("trigger_type", StringArgumentType.string())
                                                .suggests(TriggerCommand::suggestTriggerTypes)
                                                .executes(TriggerCommand::editTriggerType)))
                                .then(Commands.literal("cooldown")
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                                .executes(TriggerCommand::editCooldown)))));
    }

    private static int giveWand(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.giveWand(player);
        return respond(context.getSource(), result);
    }

    private static int saveBlueprint(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String id = StringArgumentType.getString(context, "id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.saveSelectionAsBlueprint(player, id);
        return respond(context.getSource(), result);
    }

    private static int armBind(CommandContext<CommandSourceStack> context) {
        String triggerType = StringArgumentType.getString(context, "trigger_type");
        return armBind(context, triggerType);
    }

    private static int armBind(CommandContext<CommandSourceStack> context, String triggerType) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String blueprintId = StringArgumentType.getString(context, "blueprint_id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.armBind(player, blueprintId, triggerType);
        return respond(context.getSource(), result);
    }

    private static int removeInstance(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String instanceId = StringArgumentType.getString(context, "instance_id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.removeInstance(instanceId, player);
        return respond(context.getSource(), result);
    }

    private static int activateInstance(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String instanceId = StringArgumentType.getString(context, "instance_id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.activateInstance(player, instanceId);
        return respond(context.getSource(), result);
    }

    private static int editTriggerType(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String instanceId = StringArgumentType.getString(context, "instance_id");
        String triggerType = StringArgumentType.getString(context, "trigger_type");
        TriggerModule.OperationResult result = module.editTriggerType(instanceId, triggerType);
        return respond(context.getSource(), result);
    }

    private static int editCooldown(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String instanceId = StringArgumentType.getString(context, "instance_id");
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        TriggerModule.OperationResult result = module.editCooldown(instanceId, ticks);
        return respond(context.getSource(), result);
    }

    private static CompletableFuture<Suggestions> suggestBlueprintIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            return Suggestions.empty();
        }
        Set<String> ids = new TreeSet<>();
        ids.addAll(module.blueprintLoader().getLoadedBlueprintIds());
        ids.addAll(module.blueprintLoader().discoverBlueprintIds());
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestInstanceIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            return Suggestions.empty();
        }
        return SharedSuggestionProvider.suggest(module.instanceIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestTriggerTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(TRIGGER_TYPES, builder);
    }

    private static TriggerModule module(CommandSourceStack source) {
        return TriggerModuleRegistry.get(source.getServer());
    }

    private static int respond(CommandSourceStack source, TriggerModule.OperationResult result) {
        if (result == null) {
            source.sendFailure(Component.literal("Unknown trigger command error."));
            return 0;
        }
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }
}
