package neutka.marallys.marallyzen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.OldTvBindModePacket;
import neutka.marallys.marallyzen.network.PlayScenePacket;
import neutka.marallys.marallyzen.network.ReloadScenesPacket;
import neutka.marallys.marallyzen.npc.DialogScriptLoader;
import neutka.marallys.marallyzen.npc.NpcClickHandler;
import neutka.marallys.marallyzen.npc.NpcData;
import neutka.marallys.marallyzen.npc.NpcLoader;
import neutka.marallys.marallyzen.npc.NpcStateStore;

import java.util.Objects;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class MarallyzenCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("marallyzen")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2)) // OP level 2
                        .executes(MarallyzenCommands::reloadCommand))
                .then(Commands.literal("playscene")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("sceneName", StringArgumentType.string())
                                .executes(MarallyzenCommands::playSceneCommand)))
                .then(Commands.literal("spawnnpc")
                        .requires(source -> source.hasPermission(2))
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
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("npcId", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        NpcClickHandler.getRegistry().getSpawnedNpcs().stream()
                                                .map(entity -> NpcClickHandler.getRegistry().getNpcId(entity))
                                                .filter(Objects::nonNull),
                                        builder
                                ))
                                .executes(MarallyzenCommands::removeNpcCommand)))
                .then(Commands.literal("waypoint")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("npcId", StringArgumentType.string())
                                .then(Commands.literal("loop")
                                        .then(Commands.argument("enabled", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                                .executes(MarallyzenCommands::setWaypointLoopCommand)))
                                .then(Commands.literal("move")
                                        .then(Commands.argument("waypointIndex", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                                .executes(MarallyzenCommands::moveToWaypointCommand)))))
                .then(Commands.literal("editcutscene")
                        .requires(source -> source.hasPermission(2))
                        .executes(MarallyzenCommands::editCutsceneCommand)
                        .then(Commands.argument("sceneId", StringArgumentType.string())
                                .executes(MarallyzenCommands::editCutsceneCommand)))
        );
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
            var states = registry.captureNpcStates();
            NpcStateStore.save(states);
            registry.clearNpcData();
            NpcLoader.loadNpcsFromDirectory(registry);
            int removed = registry.despawnMissingNpcs();
            registry.refreshNpcAis();

            int respawned = 0;
            ServerLevel serverLevel = context.getSource().getServer().overworld();
            if (serverLevel != null) {
                respawned = registry.spawnConfiguredNpcs(serverLevel, states);
            } else {
                Marallyzen.LOGGER.warn("Reload: overworld is not available, skipping NPC auto-spawn.");
            }
            final int respawnedCount = respawned;

            // Reload quests and zones
            neutka.marallys.marallyzen.quest.QuestManager.getInstance().reload(context.getSource().getServer());

            // Reload cutscenes on all clients
            NetworkHelper.sendToAll(new ReloadScenesPacket());
            
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

    private static int playSceneCommand(CommandContext<CommandSourceStack> context) {
        String sceneName = StringArgumentType.getString(context, "sceneName");
        var source = context.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player"));
            return 0;
        }

        // Send scene playback packet to client
        NetworkHelper.sendToPlayer(
                player,
                new PlayScenePacket(sceneName)
        );

        source.sendSuccess(
                () -> Component.literal("Scene playback started: " + sceneName),
                true
        );
        return 1;
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

    private static int editCutsceneCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player"));
            return 0;
        }

        // This command should open the editor on the client side
        // For now, just send a message - the client will handle opening the screen via key binding
        String sceneId = null;
        try {
            sceneId = StringArgumentType.getString(context, "sceneId");
        } catch (IllegalArgumentException e) {
            // No scene ID provided, will create new
        }

        final String finalSceneId = sceneId; // Make final for lambda
        if (finalSceneId != null) {
            source.sendSuccess(
                () -> Component.literal("Opening cutscene editor for: " + finalSceneId + ". Press K key to open editor."),
                false
            );
        } else {
            source.sendSuccess(
                () -> Component.literal("Press K key to open cutscene editor."),
                false
            );
        }

        return 1;
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
