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
import neutka.marallys.marallyzen.door.DoorEngine;
import neutka.marallys.marallyzen.denizen.commands.CommandScriptRegistry;
import neutka.marallys.marallyzen.npc.NpcClickHandler;
import neutka.marallys.marallyzen.npc.NpcData;
import neutka.marallys.marallyzen.npc.replay.NpcReplayEngine;
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
                .then(Commands.literal("chain")
                        .then(Commands.argument("instance_ids", StringArgumentType.greedyString())
                                .suggests(TriggerCommand::suggestChainIds)
                                .executes(TriggerCommand::linkSequence)))
                .then(Commands.literal("door")
                        .then(Commands.literal("create")
                                .then(Commands.argument("door_id", StringArgumentType.string())
                                        .executes(TriggerCommand::createDoor)))
                        .then(Commands.literal("bind")
                                .then(Commands.argument("door_id", StringArgumentType.string())
                                        .suggests(TriggerCommand::suggestDoorIds)
                                        .executes(context -> armDoorBind(context, "block_use"))
                                        .then(Commands.argument("trigger_type", StringArgumentType.string())
                                                .suggests(TriggerCommand::suggestTriggerTypes)
                                                .executes(TriggerCommand::armDoorBind))))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("door_id", StringArgumentType.string())
                                        .suggests(TriggerCommand::suggestDoorIds)
                                        .executes(TriggerCommand::deleteDoor)))
                        .then(Commands.literal("reload")
                                .executes(TriggerCommand::reloadDoors)))
                .then(Commands.literal("reload")
                        .executes(context -> reloadTriggers(context, true))
                        .then(Commands.literal("soft")
                                .executes(context -> reloadTriggers(context, false)))
                        .then(Commands.literal("hard")
                                .executes(context -> reloadTriggers(context, true))))
                .then(Commands.literal("bind")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(TriggerCommand::suggestBindIds)
                                .executes(context -> armBind(context, "block_use", ""))
                                .then(Commands.literal("chain")
                                        .then(Commands.argument("chain_ids", StringArgumentType.greedyString())
                                                .suggests(TriggerCommand::suggestChainIds)
                                                .executes(context -> armBindWithChainIds(context, "block_use"))))
                                .then(Commands.argument("trigger_type", StringArgumentType.string())
                                        .suggests(TriggerCommand::suggestTriggerTypes)
                                        .executes(TriggerCommand::armBind)
                                        .then(Commands.literal("chain")
                                                .then(Commands.argument("chain_ids", StringArgumentType.greedyString())
                                                        .suggests(TriggerCommand::suggestChainIds)
                                                        .executes(context -> armBindWithChainIds(
                                                                context,
                                                                StringArgumentType.getString(context, "trigger_type")
                                                        )))))))
                .then(Commands.literal("npc")
                        .then(Commands.literal("bind")
                                .then(Commands.argument("scene_id", StringArgumentType.string())
                                        .suggests(TriggerCommand::suggestSceneIds)
                                        .executes(context -> armNpcBind(context, "block_use", ""))
                                        .then(Commands.literal("chain")
                                                .then(Commands.argument("chain_ids", StringArgumentType.greedyString())
                                                        .suggests(TriggerCommand::suggestChainIds)
                                                        .executes(context -> armNpcBindWithChainIds(context, "block_use"))))
                                        .then(Commands.argument("trigger_type", StringArgumentType.string())
                                                .suggests(TriggerCommand::suggestTriggerTypes)
                                                .executes(TriggerCommand::armNpcBind)
                                                .then(Commands.literal("chain")
                                                        .then(Commands.argument("chain_ids", StringArgumentType.greedyString())
                                                                .suggests(TriggerCommand::suggestChainIds)
                                                                .executes(context -> armNpcBindWithChainIds(
                                                                        context,
                                                                        StringArgumentType.getString(context, "trigger_type")
                                                                )))))))
                        .then(Commands.literal("bind_replay")
                                .then(Commands.argument("npc_id", StringArgumentType.string())
                                        .suggests(TriggerCommand::suggestNpcIds)
                                        .then(Commands.argument("replay_id", StringArgumentType.string())
                                                .suggests(TriggerCommand::suggestReplayIds)
                                                .executes(context -> armNpcReplayBind(context, "block_use", ""))
                                                .then(Commands.literal("chain")
                                                        .then(Commands.argument("chain_ids", StringArgumentType.greedyString())
                                                                .suggests(TriggerCommand::suggestChainIds)
                                                                .executes(context -> armNpcReplayBindWithChainIds(context, "block_use"))))
                                                .then(Commands.argument("trigger_type", StringArgumentType.string())
                                                        .suggests(TriggerCommand::suggestTriggerTypes)
                                                        .executes(TriggerCommand::armNpcReplayBind)
                                                        .then(Commands.literal("chain")
                                                                .then(Commands.argument("chain_ids", StringArgumentType.greedyString())
                                                                        .suggests(TriggerCommand::suggestChainIds)
                                                                        .executes(context -> armNpcReplayBindWithChainIds(
                                                                                context,
                                                                                StringArgumentType.getString(context, "trigger_type")
                                                                        )))))))))
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
        return armBind(context, triggerType, "");
    }

    private static int armBind(CommandContext<CommandSourceStack> context, String triggerType, String chainId) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String blueprintId = StringArgumentType.getString(context, "id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.armBind(player, blueprintId, triggerType, chainId);
        return respond(context.getSource(), result);
    }

    private static int armBindWithChainIds(CommandContext<CommandSourceStack> context, String triggerType) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        List<String> chainIds = parseInstanceIdSequence(StringArgumentType.getString(context, "chain_ids"));
        if (chainIds.isEmpty()) {
            return armBind(context, triggerType, "");
        }
        if (chainIds.size() > 1) {
            TriggerModule.OperationResult linkResult = module.linkSequence(chainIds);
            if (!linkResult.success()) {
                return respond(context.getSource(), linkResult);
            }
        }
        String previousId = chainIds.get(chainIds.size() - 1);
        return armBind(context, triggerType, previousId);
    }

    private static int armNpcBind(CommandContext<CommandSourceStack> context) {
        String triggerType = StringArgumentType.getString(context, "trigger_type");
        return armNpcBind(context, triggerType);
    }

    private static int armNpcBind(CommandContext<CommandSourceStack> context, String triggerType) {
        return armNpcBind(context, triggerType, "");
    }

    private static int armNpcBind(CommandContext<CommandSourceStack> context, String triggerType, String chainId) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String sceneId = StringArgumentType.getString(context, "scene_id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.armNpcSceneBind(player, sceneId, triggerType, chainId);
        return respond(context.getSource(), result);
    }

    private static int armNpcBindWithChainIds(CommandContext<CommandSourceStack> context, String triggerType) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        List<String> chainIds = parseInstanceIdSequence(StringArgumentType.getString(context, "chain_ids"));
        if (chainIds.isEmpty()) {
            return armNpcBind(context, triggerType, "");
        }
        if (chainIds.size() > 1) {
            TriggerModule.OperationResult linkResult = module.linkSequence(chainIds);
            if (!linkResult.success()) {
                return respond(context.getSource(), linkResult);
            }
        }
        String previousId = chainIds.get(chainIds.size() - 1);
        return armNpcBind(context, triggerType, previousId);
    }

    private static int armNpcReplayBind(CommandContext<CommandSourceStack> context) {
        String triggerType = StringArgumentType.getString(context, "trigger_type");
        return armNpcReplayBind(context, triggerType);
    }

    private static int armNpcReplayBind(CommandContext<CommandSourceStack> context, String triggerType) {
        return armNpcReplayBind(context, triggerType, "");
    }

    private static int armNpcReplayBind(CommandContext<CommandSourceStack> context, String triggerType, String chainId) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String npcId = StringArgumentType.getString(context, "npc_id");
        String replayId = StringArgumentType.getString(context, "replay_id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.armNpcReplayBind(player, npcId, replayId, triggerType, chainId);
        return respond(context.getSource(), result);
    }

    private static int armNpcReplayBindWithChainIds(CommandContext<CommandSourceStack> context, String triggerType) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        List<String> chainIds = parseInstanceIdSequence(StringArgumentType.getString(context, "chain_ids"));
        if (chainIds.isEmpty()) {
            return armNpcReplayBind(context, triggerType, "");
        }
        if (chainIds.size() > 1) {
            TriggerModule.OperationResult linkResult = module.linkSequence(chainIds);
            if (!linkResult.success()) {
                return respond(context.getSource(), linkResult);
            }
        }
        String previousId = chainIds.get(chainIds.size() - 1);
        return armNpcReplayBind(context, triggerType, previousId);
    }

    private static int linkSequence(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        List<String> ids = parseInstanceIdSequence(StringArgumentType.getString(context, "instance_ids"));
        TriggerModule.OperationResult result = module.linkSequence(ids);
        return respond(context.getSource(), result);
    }

    private static int createDoor(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String doorId = StringArgumentType.getString(context, "door_id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.createDoor(player, doorId);
        return respond(context.getSource(), result);
    }

    private static int armDoorBind(CommandContext<CommandSourceStack> context) {
        String triggerType = StringArgumentType.getString(context, "trigger_type");
        return armDoorBind(context, triggerType);
    }

    private static int armDoorBind(CommandContext<CommandSourceStack> context, String triggerType) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String doorId = StringArgumentType.getString(context, "door_id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.armDoorBind(player, doorId, triggerType);
        return respond(context.getSource(), result);
    }

    private static int deleteDoor(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        String doorId = StringArgumentType.getString(context, "door_id");
        ServerPlayer player = context.getSource().getPlayer();
        TriggerModule.OperationResult result = module.deleteDoor(player, doorId);
        return respond(context.getSource(), result);
    }

    private static int reloadDoors(CommandContext<CommandSourceStack> context) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        TriggerModule.OperationResult result = module.reloadDoors();
        return respond(context.getSource(), result);
    }

    private static int reloadTriggers(CommandContext<CommandSourceStack> context, boolean hardReset) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Trigger module is not initialized."));
            return 0;
        }
        TriggerModule.OperationResult result = module.reloadTriggers(hardReset);
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

    private static CompletableFuture<Suggestions> suggestBindIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            return Suggestions.empty();
        }
        Set<String> ids = new TreeSet<>();
        ids.addAll(module.blueprintLoader().getLoadedBlueprintIds());
        ids.addAll(module.blueprintLoader().discoverBlueprintIds());
        ids.addAll(CommandScriptRegistry.commandScriptNames());
        ids.addAll(NpcReplayEngine.loadedReplayIds());
        ids.addAll(DoorEngine.getInstance().doorIds(context.getSource().getServer()));
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

    private static CompletableFuture<Suggestions> suggestSceneIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(CommandScriptRegistry.commandScriptNames(), builder);
    }

    private static CompletableFuture<Suggestions> suggestNpcIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                NpcClickHandler.getRegistry().getAllNpcData().stream().map(NpcData::getId),
                builder
        );
    }

    private static CompletableFuture<Suggestions> suggestReplayIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(NpcReplayEngine.loadedReplayIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestChainIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        TriggerModule module = module(context.getSource());
        if (module == null) {
            return Suggestions.empty();
        }
        return SharedSuggestionProvider.suggest(module.instanceIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestDoorIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(DoorEngine.getInstance().doorIds(context.getSource().getServer()), builder);
    }

    private static List<String> parseInstanceIdSequence(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] parts = raw.trim().split("\\s+");
        List<String> ids = new java.util.ArrayList<>(parts.length);
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                ids.add(part.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
        return ids;
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
