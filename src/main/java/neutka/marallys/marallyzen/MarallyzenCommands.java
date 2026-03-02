package neutka.marallys.marallyzen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.OldTvBindModePacket;
import neutka.marallys.marallyzen.npc.DialogScriptLoader;
import neutka.marallys.marallyzen.npc.NpcClickHandler;
import neutka.marallys.marallyzen.npc.NpcData;
import neutka.marallys.marallyzen.npc.NpcLoader;
import neutka.marallys.marallyzen.npc.NpcSavedData;
import neutka.marallys.marallyzen.npc.NpcStateStore;
import neutka.marallys.marallyzen.npc.NpcSpawner;
import neutka.marallys.marallyzen.npc.replay.NpcReplayEngine;
import neutka.marallys.marallyzen.npc.replay.NpcReplayLoader;
import neutka.marallys.marallyzen.npc.replay.NpcReplayRecorder;
import neutka.marallys.marallyzen.npc.replay.NpcReplayScript;
import neutka.marallys.marallyzen.goals.GoalCommand;
import neutka.marallys.marallyzen.trigger.TriggerModuleRegistry;
import neutka.marallys.marallyzen.trigger.command.TriggerCommand;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@EventBusSubscriber(modid = Marallyzen.MODID)
public class MarallyzenCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("marallyzen")
                .executes(MarallyzenCommands::infoCommand)
                .then(Commands.literal("reload")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) // OP level 2
                        .executes(MarallyzenCommands::reloadCommand))
                .then(Commands.literal("spawnnpc")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(MarallyzenCommands::listNpcsCommand)
                        .then(Commands.argument("npcId", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        NpcClickHandler.getRegistry().getAllNpcData().stream().map(NpcData::getId),
                                        builder
                                ))
                                .executes(MarallyzenCommands::spawnNpcCommand)
                                .then(Commands.literal("--keep")
                                        .executes(context -> spawnNpcCommand(context, true)))))
                .then(Commands.literal("removenpc")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("npcId", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        NpcClickHandler.getRegistry().getSpawnedNpcs().stream()
                                                .map(entity -> NpcClickHandler.getRegistry().getNpcId(entity))
                                                .filter(Objects::nonNull),
                                        builder
                                ))
                                .executes(MarallyzenCommands::removeNpcCommand)))
                .then(Commands.literal("waypoint")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("npcId", StringArgumentType.string())
                                .then(Commands.literal("loop")
                                        .then(Commands.argument("enabled", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                                .executes(MarallyzenCommands::setWaypointLoopCommand)))
                                .then(Commands.literal("move")
                                        .then(Commands.argument("waypointIndex", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                                .executes(MarallyzenCommands::moveToWaypointCommand)))))
                .then(Commands.literal("npc")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("replay")
                                .then(Commands.literal("record")
                                        .then(Commands.argument("npcId", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        NpcClickHandler.getRegistry().getAllNpcData().stream().map(NpcData::getId),
                                                        builder
                                                ))
                                                .then(Commands.argument("scriptId", StringArgumentType.string())
                                                        .executes(MarallyzenCommands::npcReplayRecordCommand))))
                                .then(Commands.literal("stop")
                                        .executes(MarallyzenCommands::npcReplayStopCommand)
                                        .then(Commands.argument("npcId", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        NpcClickHandler.getRegistry().getAllNpcData().stream().map(NpcData::getId),
                                                        builder
                                                ))
                                                .executes(MarallyzenCommands::npcReplayStopNpcCommand)))
                                .then(Commands.literal("list")
                                        .executes(MarallyzenCommands::npcReplayListCommand))
                                .then(Commands.literal("status")
                                        .then(Commands.argument("npcId", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        NpcClickHandler.getRegistry().getAllNpcData().stream().map(NpcData::getId),
                                                        builder
                                                ))
                                                .executes(MarallyzenCommands::npcReplayStatusCommand)))
                                .then(Commands.literal("debug")
                                        .then(Commands.argument("enabled", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                                .executes(MarallyzenCommands::npcReplayDebugCommand)))
                                .then(Commands.literal("debugvis")
                                        .then(Commands.argument("npcId", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        NpcClickHandler.getRegistry().getAllNpcData().stream().map(NpcData::getId),
                                                        builder
                                                ))
                                                .executes(MarallyzenCommands::npcReplayDebugVisCommand)))
                                .then(Commands.literal("play")
                                        .then(Commands.argument("npcId", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        NpcClickHandler.getRegistry().getAllNpcData().stream().map(NpcData::getId),
                                                        builder
                                                ))
                                                .then(Commands.argument("scriptId", StringArgumentType.string())
                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                                NpcReplayEngine.loadedReplayIds(),
                                                                builder
                                                        ))
                                                        .executes(MarallyzenCommands::npcReplayPlayCommand))))
                                .then(Commands.literal("preview")
                                        .then(Commands.argument("npcId", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        NpcClickHandler.getRegistry().getAllNpcData().stream().map(NpcData::getId),
                                                        builder
                                                ))
                                                .then(Commands.argument("scriptId", StringArgumentType.string())
                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                                NpcReplayEngine.loadedReplayIds(),
                                                                builder
                                                        ))
                                                        .executes(MarallyzenCommands::npcReplayPreviewCommand))))
                                .then(Commands.literal("delete")
                                        .then(Commands.argument("scriptId", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        NpcReplayEngine.loadedReplayIds(),
                                                        builder
                                                ))
                                                .executes(MarallyzenCommands::npcReplayDeleteCommand)))))
                .then(GoalCommand.build())
                .then(TriggerCommand.build())
        );
    }

    private static int infoCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String version = ModList.get().getModContainerById(Marallyzen.MODID)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("unknown");

        Component title = Component.literal("Marallyzen ").append(Component.literal(version))
            .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GOLD));
        source.sendSuccess(() -> title, false);

        net.minecraft.network.chat.MutableComponent github = Component.literal("\uE000")
            .withStyle(style -> style.withFont(new FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath(Marallyzen.MODID, "icons"))))
            .append(Component.literal(" Github").withStyle(style -> style
                .withFont(FontDescription.DEFAULT)
                .withColor(net.minecraft.ChatFormatting.AQUA)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create("https://github.com/ne-utka/Marallyzen")))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Discord: mengion")))
            ));
        source.sendSuccess(() -> github, false);

        Component author = Component.literal("РђРІС‚РѕСЂ РјРѕРґР° - loneliness")
            .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY));
        source.sendSuccess(() -> author, false);

        net.minecraft.network.chat.MutableComponent discord = Component.literal("\uE001")
            .withStyle(style -> style.withFont(new FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath(Marallyzen.MODID, "icons"))))
            .append(Component.literal(" Discord - mengion").withStyle(style -> style
                .withFont(FontDescription.DEFAULT)
                .withColor(net.minecraft.ChatFormatting.DARK_AQUA)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Нажмите для открытия страницы на Github.")))
            ));
        source.sendSuccess(() -> discord, false);

        Component contributors = Component.literal("Р’Р»РѕР¶РёР»Рё РІРєР»Р°Рґ:")
            .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.YELLOW));
        source.sendSuccess(() -> contributors, false);
        source.sendSuccess(() -> Component.literal(" - Yl7oPoTblU_KoT").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal(" - Bumchik_ (РРІР°РЅ РљР°Р·РјРёСЂРµРЅРєРѕ)").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal(" - ItsReizy").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY)), false);
        return 1;
    }
private static int reloadCommand(CommandContext<CommandSourceStack> context) {
        try {
            // Reload DenizenCore scripts
            DenizenService.reload();
            
            // Clear dialog script cache
            DialogScriptLoader.clearCache();

            // Reload AI config
            neutka.marallys.marallyzen.ai.NpcAiManager.reload();
            
            // Reload NPCs from JSON files
            var registry = NpcClickHandler.getRegistry();
            registry.clearNpcData();
            NpcLoader.loadNpcsFromDirectory(registry);
            int removed = registry.despawnMissingNpcs();
            registry.refreshNpcAis();

            int respawned = 0;
            ServerLevel serverLevel = context.getSource().getServer().overworld();
            if (serverLevel != null) {
                NpcSpawner.bootstrap(serverLevel, registry);
            } else {
                Marallyzen.LOGGER.warn("Reload: overworld is not available, skipping NPC auto-spawn.");
            }
            NpcReplayEngine.reload();
            NpcReplayEngine.initialize(context.getSource().getServer());
            final int respawnedCount = respawned;

            // Reload quests and zones
            neutka.marallys.marallyzen.quest.QuestManager.getInstance().reload(context.getSource().getServer());
            neutka.marallys.marallyzen.goals.GoalProgressEngine.getInstance().reload(context.getSource().getServer());
            var triggerModule = TriggerModuleRegistry.get(context.getSource().getServer());
            if (triggerModule != null) {
                triggerModule.reload(context.getSource().getServer());
            }

            int npcCount = registry.getAllNpcData().size();
            context.getSource().sendSuccess(
                    () -> Component.literal("Marallyzen reloaded successfully. Loaded " + npcCount + " NPC(s), respawned " + respawnedCount + ", removed " + removed + "."),
                    true
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("Failed to reload: " + e.getMessage())
            );
            Marallyzen.LOGGER.error("Failed to reload", e);
            return 0;
        }
    }


    private static int listNpcsCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        
        // Get all NPC IDs from registry
        var registry = NpcClickHandler.getRegistry();
        var allNpcs = registry.getAllNpcData();
        
        if (allNpcs.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("No NPCs found. Place JSON files in config/marallyzen/npcs/"),
                    false
            );
            return 0;
        }
        
        // Build list message
        StringBuilder message = new StringBuilder("Available NPCs (" + allNpcs.size() + "):\n");
        for (NpcData npc : allNpcs) {
            String name = npc.getName();
            if (name != null && !name.isEmpty()) {
                // Try to extract plain text from JSON name
                if (name.startsWith("{")) {
                    try {
                        var gson = new com.google.gson.Gson();
                        var json = gson.fromJson(name, com.google.gson.JsonObject.class);
                        if (json.has("text")) {
                            name = json.get("text").getAsString();
                        }
                    } catch (Exception e) {
                        // Keep original name if parsing fails
                    }
                }
                message.append("  - ").append(npc.getId()).append(" (").append(name).append(")\n");
            } else {
                message.append("  - ").append(npc.getId()).append("\n");
            }
        }
        message.append("\nUsage: /marallyzen spawnnpc <npcId>");
        
        source.sendSuccess(
                () -> Component.literal(message.toString()),
                false
        );
        return 1;
    }
    
    private static int spawnNpcCommand(CommandContext<CommandSourceStack> context) {
        return spawnNpcCommand(context, false);
    }

    private static int spawnNpcCommand(CommandContext<CommandSourceStack> context, boolean keepExisting) {
        String npcId = StringArgumentType.getString(context, "npcId");
        var source = context.getSource();
        var level = source.getLevel();
        
        if (!(level instanceof ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("This command can only be used on a server"));
            return 0;
        }

        NpcData npcData = NpcClickHandler.getRegistry().getNpcData(npcId);
        if (npcData == null) {
            source.sendFailure(Component.literal("NPC not found: " + npcId + ". Use /marallyzen spawnnpc to see available NPCs."));
            return 0;
        }

        var registry = NpcClickHandler.getRegistry();
        if (!keepExisting && registry.getNpc(npcId) != null) {
            registry.despawnNpc(npcId);
        }

        // Always use command sender's position when spawning via command
        // (spawnPos from JSON is only used for automatic spawning on server start)
        BlockPos spawnPos = BlockPos.containing(source.getPosition());
        final BlockPos finalSpawnPos = spawnPos; // Make final for lambda
        
        // Get the player who executed the command (if it's a player)
        ServerPlayer sourcePlayer = null;
        if (source.getEntity() instanceof ServerPlayer player) {
            sourcePlayer = player;
        }
        
          try {
              final String spawnId;
              if (keepExisting) {
                  spawnId = registry.createNpcCopyId(npcId);
              } else {
                  spawnId = npcId;
              }
              registry.spawnNpc(spawnId, serverLevel, finalSpawnPos, sourcePlayer);
              NpcStateStore.removeDisabled(spawnId);
              NpcData spawnedData = registry.getNpcData(spawnId);
              if (spawnedData != null) {
                  spawnedData.setSpawnPos(BlockPos.containing(source.getPosition()));
                  NpcLoader.saveNpcToFile(spawnedData);
              }
              NpcStateStore.save(registry.captureNpcStates());
              source.sendSuccess(
                      () -> Component.literal("Spawned NPC: " + spawnId + " at " + finalSpawnPos),
                      true
            );
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to spawn NPC: " + e.getMessage()));
            Marallyzen.LOGGER.error("Failed to spawn NPC: " + npcId, e);
            return 0;
        }
    }

    private static int removeNpcCommand(CommandContext<CommandSourceStack> context) {
        String npcId = StringArgumentType.getString(context, "npcId");
        var source = context.getSource();
        var registry = NpcClickHandler.getRegistry();
        var npc = registry.getNpc(npcId);
        if (npc == null) {
            source.sendFailure(Component.literal("NPC not found in world: " + npcId));
            return 0;
        }
        registry.despawnNpc(npcId);
        if (source.getLevel() instanceof ServerLevel serverLevel) {
            NpcSavedData.get(serverLevel).removeState(npcId);
        }
        NpcStateStore.addDisabled(npcId);
        NpcStateStore.save(registry.captureNpcStates());
        source.sendSuccess(
                () -> Component.literal("Removed NPC: " + npcId),
                true
        );
        return 1;
    }

    private static int setWaypointLoopCommand(CommandContext<CommandSourceStack> context) {
        String npcId = StringArgumentType.getString(context, "npcId");
        boolean enabled = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "enabled");
        var source = context.getSource();

        if (!NpcClickHandler.getRegistry().hasWaypoints(npcId)) {
            source.sendFailure(Component.literal("NPC '" + npcId + "' has no waypoints"));
            return 0;
        }

        NpcClickHandler.getRegistry().setWaypointLoop(npcId, enabled);
        source.sendSuccess(
                () -> Component.literal("Waypoint loop " + (enabled ? "enabled" : "disabled") + " for NPC: " + npcId),
                true
        );
        return 1;
    }

    private static int moveToWaypointCommand(CommandContext<CommandSourceStack> context) {
        String npcId = StringArgumentType.getString(context, "npcId");
        int waypointIndex = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "waypointIndex");
        var source = context.getSource();

        if (!NpcClickHandler.getRegistry().hasWaypoints(npcId)) {
            source.sendFailure(Component.literal("NPC '" + npcId + "' has no waypoints"));
            return 0;
        }

        try {
            NpcClickHandler.getRegistry().moveNpcToWaypoint(npcId, waypointIndex);
            source.sendSuccess(
                    () -> Component.literal("Moving NPC '" + npcId + "' to waypoint " + waypointIndex),
                    true
            );
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to move NPC to waypoint: " + e.getMessage()));
            Marallyzen.LOGGER.error("Failed to move NPC to waypoint", e);
            return 0;
        }
    }

    private static int npcReplayRecordCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        String npcId = StringArgumentType.getString(context, "npcId");
        String scriptId = StringArgumentType.getString(context, "scriptId");
        var result = NpcReplayRecorder.getInstance().startRecording(player, npcId, scriptId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int npcReplayStopCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        var result = NpcReplayRecorder.getInstance().stopRecording(player);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int npcReplayStopNpcCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String npcId = StringArgumentType.getString(context, "npcId");
        var result = NpcReplayEngine.stop(npcId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int npcReplayListCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var scripts = NpcReplayEngine.loadedReplayScripts();
        if (scripts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No replay scripts found in config/marallyzen/npc_replay/"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Replay scripts (" + scripts.size() + "):"), false);
        scripts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    NpcReplayScript script = entry.getValue();
                    int ticks = script.expandedLength();
                    double seconds = ticks / 20.0D;
                    String loopState = script.loop() ? "loop" : "once";
                    source.sendSuccess(
                            () -> Component.literal(
                                    " - " + entry.getKey()
                                            + " | " + loopState
                                            + " | mode=" + script.mode().name().toLowerCase(Locale.ROOT)
                                            + " | frames=" + script.frames().size()
                                            + " | duration=" + ticks + "t (" + String.format(Locale.ROOT, "%.2f", seconds) + "s)"
                            ),
                            false
                    );
                });
        return 1;
    }

    private static int npcReplayStatusCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String npcId = StringArgumentType.getString(context, "npcId");
        NpcReplayEngine.Status status = NpcReplayEngine.status(npcId);

        source.sendSuccess(() -> Component.literal("Replay status for npc=" + status.npcId() + ":"), false);
        source.sendSuccess(() -> Component.literal(" - replayRunning: " + status.replayRunning()), false);
        source.sendSuccess(() -> Component.literal(" - currentReplayId: " + (status.currentReplayId().isBlank() ? "<none>" : status.currentReplayId())), false);
        source.sendSuccess(() -> Component.literal(" - currentFrameIndex: " + status.currentFrameIndex() + "/" + Math.max(0, status.totalFrames() - 1)), false);
        source.sendSuccess(() -> Component.literal(" - loop: " + status.loop()), false);
        source.sendSuccess(() -> Component.literal(" - mode: " + status.mode()), false);
        source.sendSuccess(() -> Component.literal(" - interpolate: " + status.interpolate()), false);
        source.sendSuccess(() -> Component.literal(" - lastStopReason: " + status.lastStopReason().name()), false);
        source.sendSuccess(() -> Component.literal(" - uptime: " + status.uptimeTicks() + "t (" + String.format(Locale.ROOT, "%.2f", status.uptimeSeconds()) + "s)"), false);
        source.sendSuccess(() -> Component.literal(" - npcPosition: " + formatPos(status.npcX(), status.npcY(), status.npcZ())), false);
        source.sendSuccess(() -> Component.literal(" - scriptStartPosition: " + formatPos(status.scriptStartX(), status.scriptStartY(), status.scriptStartZ())), false);
        source.sendSuccess(() -> Component.literal(" - perf: last=" + status.lastTickCostNanos() + "ns, avg=" + status.averageTickCostNanos() + "ns"), false);
        source.sendSuccess(() -> Component.literal(" - scriptMemory: " + status.scriptMemoryBytes() + " bytes"), false);
        return 1;
    }

    private static int npcReplayDebugCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        boolean enabled = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "enabled");
        NpcReplayEngine.setDebugEnabled(enabled);
        source.sendSuccess(() -> Component.literal("Npc replay debug is now " + enabled), true);
        return 1;
    }

    private static int npcReplayDebugVisCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        String npcId = StringArgumentType.getString(context, "npcId");
        var result = NpcReplayEngine.toggleDebugVisual(player, npcId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int npcReplayPlayCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String npcId = StringArgumentType.getString(context, "npcId");
        String scriptId = StringArgumentType.getString(context, "scriptId");
        var result = NpcReplayEngine.play(npcId, scriptId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int npcReplayPreviewCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String npcId = StringArgumentType.getString(context, "npcId");
        String scriptId = StringArgumentType.getString(context, "scriptId");
        var result = NpcReplayEngine.preview(npcId, scriptId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int npcReplayDeleteCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        String scriptId = StringArgumentType.getString(context, "scriptId").trim().toLowerCase(Locale.ROOT);
        if (scriptId.isBlank()) {
            source.sendFailure(Component.literal("scriptId is required."));
            return 0;
        }

        if (NpcReplayEngine.getInstance().isReplayRunningByScript(scriptId)) {
            source.sendFailure(Component.literal("Cannot delete replay while it is running: " + scriptId));
            return 0;
        }
        ServerLevel overworld = source.getServer().overworld();
        if (overworld != null) {
            boolean persistedUse = NpcSavedData.get(overworld).getNpcStates().values().stream()
                    .anyMatch(state -> scriptId.equalsIgnoreCase(state.currentReplayId()));
            if (persistedUse) {
                source.sendFailure(Component.literal("Cannot delete replay referenced by NPC state: " + scriptId));
                return 0;
            }
        }
        if (isReplayReferencedInTriggers(source, scriptId)) {
            source.sendFailure(Component.literal("Cannot delete replay referenced by trigger blueprints: " + scriptId));
            return 0;
        }

        boolean removed = NpcReplayLoader.getInstance().delete(scriptId);
        if (!removed) {
            source.sendFailure(Component.literal("Replay not found or failed to delete: " + scriptId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Replay deleted: " + scriptId), true);
        return 1;
    }

    private static String formatPos(Double x, Double y, Double z) {
        if (x == null || y == null || z == null) {
            return "<unavailable>";
        }
        return String.format(Locale.ROOT, "%.3f, %.3f, %.3f", x, y, z);
    }

    private static boolean isReplayReferencedInTriggers(CommandSourceStack source, String replayId) {
        var module = TriggerModuleRegistry.get(source.getServer());
        if (module == null || replayId == null || replayId.isBlank()) {
            return false;
        }
        for (String blueprintId : module.blueprintIds()) {
            TriggerBlueprint blueprint = module.blueprintLoader().getBlueprint(blueprintId);
            if (blueprint == null) {
                continue;
            }
            TriggerBlueprint.Settings.ActionSettings action = blueprint.settings().action();
            if (action == null) {
                continue;
            }
            if (replayId.equalsIgnoreCase(action.replay())) {
                return true;
            }
            String activityType = action.resolveActivityType();
            if ("npc".equalsIgnoreCase(activityType)) {
                String activityScript = action.resolveActivityScript();
                if (replayId.equalsIgnoreCase(activityScript)) {
                    return true;
                }
                var script = module.activityRegistry().loader().getScript("npc", activityScript);
                if (script != null) {
                    String scriptedReplay = script.getString("replay_id", "");
                    if (replayId.equalsIgnoreCase(scriptedReplay)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private static int onTvCommand(CommandContext<CommandSourceStack> context) {
        String mediaName = StringArgumentType.getString(context, "mediaName");
        var source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player"));
            return 0;
        }
        NetworkHelper.sendToPlayer(player, new OldTvBindModePacket(true, mediaName));
        source.sendSuccess(
                () -> Component.literal("TV bind mode enabled for media: " + mediaName),
                true
        );
        return 1;
    }

    private static int leaveTvCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player"));
            return 0;
        }
        NetworkHelper.sendToPlayer(player, new OldTvBindModePacket(false, null));
        source.sendSuccess(
                () -> Component.literal("TV bind mode disabled"),
                true
        );
        return 1;
    }
}




