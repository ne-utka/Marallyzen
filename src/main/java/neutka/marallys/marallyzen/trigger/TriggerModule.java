package neutka.marallys.marallyzen.trigger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.activity.ActivityRegistry;
import neutka.marallys.marallyzen.door.DoorEngine;
import neutka.marallys.marallyzen.denizen.commands.CommandScriptRegistry;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.TriggerBindSyncPacket;
import neutka.marallys.marallyzen.npc.NpcClickHandler;
import neutka.marallys.marallyzen.npc.replay.NpcReplayLoader;
import neutka.marallys.marallyzen.npc.replay.NpcReplayScript;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprintLoader;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlockStateCodec;
import neutka.marallys.marallyzen.trigger.command.SelectionManager;
import neutka.marallys.marallyzen.trigger.engine.TriggerEngine;
import neutka.marallys.marallyzen.trigger.engine.TriggerZoneManager;
import neutka.marallys.marallyzen.trigger.event.AdminHudNotifier;
import neutka.marallys.marallyzen.trigger.instance.TriggerInstance;
import neutka.marallys.marallyzen.trigger.instance.TriggerInstanceManager;
import neutka.marallys.marallyzen.trigger.storage.TriggerSavedData;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TriggerModule {
    public record OperationResult(boolean success, String message) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final TriggerBlueprintLoader blueprintLoader = new TriggerBlueprintLoader();
    private final SelectionManager selectionManager = new SelectionManager();
    private final TriggerRegistry triggerRegistry = new TriggerRegistry();
    private final TriggerZoneManager zoneManager = new TriggerZoneManager();
    private final TriggerInstanceManager instanceManager = new TriggerInstanceManager(blueprintLoader);
    private final ActivityRegistry activityRegistry = new ActivityRegistry();
    private final TriggerEngine engine = new TriggerEngine(blueprintLoader, instanceManager, triggerRegistry, zoneManager, activityRegistry);

    private MinecraftServer server;
    private boolean initialized;

    public void initialize(MinecraftServer server) {
        this.server = server;
        blueprintLoader.reloadBlueprints();
        activityRegistry.reload();
        if (server == null || server.overworld() == null) {
            initialized = false;
            return;
        }
        TriggerSavedData savedData = TriggerSavedData.get(server.overworld());
        instanceManager.attachSavedData(savedData);
        instanceManager.rebuildFromSaved();
        engine.rebuildZones();
        syncTriggerBindingsToAll();
        initialized = true;
        Marallyzen.LOGGER.info("TriggerModule initialized: {} blueprint(s), {} instance(s)",
                blueprintLoader.getLoadedBlueprintIds().size(),
                instanceManager.allIds().size());
    }

    public void reload(MinecraftServer server) {
        if (server != null) {
            this.server = server;
        }
        blueprintLoader.reloadBlueprints();
        activityRegistry.reload();
        if (this.server == null || this.server.overworld() == null) {
            initialized = false;
            return;
        }
        TriggerSavedData savedData = TriggerSavedData.get(this.server.overworld());
        instanceManager.attachSavedData(savedData);
        instanceManager.rebuildFromSaved();
        engine.rebuildZones();
        syncTriggerBindingsToAll();
        initialized = true;
        Marallyzen.LOGGER.info("TriggerModule reloaded: {} blueprint(s), {} instance(s)",
                blueprintLoader.getLoadedBlueprintIds().size(),
                instanceManager.allIds().size());
    }

    public void shutdown() {
        zoneManager.clear();
        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public TriggerBlueprintLoader blueprintLoader() {
        return blueprintLoader;
    }

    public ActivityRegistry activityRegistry() {
        return activityRegistry;
    }

    public TriggerInstanceManager instanceManager() {
        return instanceManager;
    }

    public TriggerEngine engine() {
        return engine;
    }

    public SelectionManager selectionManager() {
        return selectionManager;
    }

    public void onServerTick() {
        if (!initialized || server == null) {
            return;
        }
        engine.onServerTick(server);
    }

    public void onPlayerTick(ServerPlayer player) {
        if (!initialized || player == null) {
            return;
        }
        engine.onPlayerTick(player);
    }

    public boolean onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!initialized || event == null || event.getEntity().level().isClientSide()) {
            return false;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!selectionManager.isWand(stack)) {
            return false;
        }
        String dimensionId = player.level().dimension().identifier().toString();
        SelectionManager.SelectionResult result = selectionManager.setPos1(player, dimensionId, event.getPos());
        if (result.valid()) {
            AdminHudNotifier.firstPoint(player);
        } else {
            player.displayClientMessage(Component.literal(result.message()), true);
        }
        return true;
    }

    public boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!initialized || event == null || event.getLevel().isClientSide()) {
            return false;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return false;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (selectionManager.isWand(stack)) {
            String dimensionId = player.level().dimension().identifier().toString();
            SelectionManager.SelectionResult result = selectionManager.setPos2(player, dimensionId, event.getPos());
            SelectionManager.Selection selection = selectionManager.getSelection(player.getUUID());
            if (result.valid() && selection != null && selection.isComplete()) {
                AdminHudNotifier.secondPoint(player, selection.sizeX(), selection.sizeY(), selection.sizeZ());
            } else {
                player.displayClientMessage(Component.literal(result.message()), true);
            }
            return true;
        }

        TriggerInstanceManager.PendingBind pending = instanceManager.consumePendingBind(player.getUUID());
        if (pending != null) {
            TriggerInstance instance = instanceManager.createBoundInstance(player, event.getPos(), pending);
            if (instance == null) {
                player.sendSystemMessage(Component.literal("Failed to bind trigger. Blueprint not found or invalid."));
                return true;
            }
            engine.rebuildZones();
            syncTriggerBindingsToAll();
            AdminHudNotifier.bound(player);
            return true;
        }

        boolean activated = engine.onBlockUse(player, event.getPos());
        if (activated) {
            return true;
        }
        return false;
    }

    public OperationResult giveWand(ServerPlayer player) {
        if (player == null) {
            return new OperationResult(false, "This command must be executed by a player.");
        }
        ItemStack wand = selectionManager.createWand();
        if (!player.getInventory().add(wand)) {
            player.drop(wand, false);
        }
        return new OperationResult(true, "Trigger wand given.");
    }

    public OperationResult saveSelectionAsBlueprint(ServerPlayer player, String rawId) {
        if (player == null) {
            return new OperationResult(false, "This command must be executed by a player.");
        }
        String id = normalizeId(rawId);
        if (id.isBlank()) {
            return new OperationResult(false, "Blueprint id is required.");
        }
        SelectionManager.Selection selection = selectionManager.getSelection(player.getUUID());
        if (selection == null || !selection.isComplete()) {
            return new OperationResult(false, "Selection is incomplete. Use wand to set pos1/pos2.");
        }
        if (!selection.dimensionId().equals(player.level().dimension().identifier().toString())) {
            return new OperationResult(false, "Selection is from another dimension.");
        }
        if (selection.sizeX() > 20 || selection.sizeY() > 20 || selection.sizeZ() > 20) {
            return new OperationResult(false, "Selection too large: max 20x20x20.");
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return new OperationResult(false, "Server level is unavailable.");
        }

        BlockPos min = selection.min();
        BlockPos max = selection.max();
        List<TriggerBlueprint.BlockEntry> entries = new ArrayList<>();
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(worldPos);
                    if (state.isAir()) {
                        continue;
                    }
                    BlockPos localPos = new BlockPos(x - min.getX(), y - min.getY(), z - min.getZ());
                    String stateString = TriggerBlockStateCodec.encode(state);
                    String blockEntitySnbt = "";
                    BlockEntity blockEntity = level.getBlockEntity(worldPos);
                    if (blockEntity != null) {
                        CompoundTag tag = blockEntity.saveWithFullMetadata(level.registryAccess());
                        tag.putInt("x", 0);
                        tag.putInt("y", 0);
                        tag.putInt("z", 0);
                        blockEntitySnbt = tag.toString();
                    }
                    entries.add(new TriggerBlueprint.BlockEntry(localPos, stateString, blockEntitySnbt));
                }
            }
        }

        TriggerBlueprint blueprint = new TriggerBlueprint(
                id,
                new BlockPos(selection.sizeX(), selection.sizeY(), selection.sizeZ()),
                min,
                entries,
                TriggerBlueprint.Settings.defaults()
        );
        Path file = blueprintLoader.getBlueprintDirectory().resolve(id + ".json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(blueprint.toJson(), writer);
            }
        } catch (Exception e) {
            return new OperationResult(false, "Failed to save blueprint: " + e.getMessage());
        }
        blueprintLoader.reloadBlueprints();
        selectionManager.clearSelection(player.getUUID());
        clearWorldArea(level, min, max);
        AdminHudNotifier.saved(player);
        return new OperationResult(true, "Blueprint saved: " + id + " (" + entries.size() + " blocks).");
    }

    public OperationResult createDoor(ServerPlayer player, String rawId) {
        if (player == null) {
            return new OperationResult(false, "This command must be executed by a player.");
        }
        String id = normalizeId(rawId);
        if (id.isBlank()) {
            return new OperationResult(false, "Door id is required.");
        }
        SelectionManager.Selection selection = selectionManager.getSelection(player.getUUID());
        if (selection == null || !selection.isComplete()) {
            return new OperationResult(false, "Selection is incomplete. Use wand to set pos1/pos2.");
        }
        if (!selection.dimensionId().equals(player.level().dimension().identifier().toString())) {
            return new OperationResult(false, "Selection is from another dimension.");
        }
        if (selection.sizeX() > 20 || selection.sizeY() > 20 || selection.sizeZ() > 20) {
            return new OperationResult(false, "Selection too large: max 20x20x20.");
        }
        boolean created = DoorEngine.getInstance().createDoor(player, selection, id);
        if (!created) {
            return new OperationResult(false, "Failed to create door: " + id);
        }
        selectionManager.clearSelection(player.getUUID());
        OperationResult activityResult = ensureDoorActivityScript(id);
        if (!activityResult.success()) {
            return activityResult;
        }
        return new OperationResult(true, "Door saved: " + id);
    }

    public OperationResult deleteDoor(ServerPlayer player, String rawId) {
        if (server == null) {
            return new OperationResult(false, "Trigger system is not ready.");
        }
        String id = normalizeId(rawId);
        if (id.isBlank()) {
            return new OperationResult(false, "Door id is required.");
        }
        boolean deleted = DoorEngine.getInstance().deleteDoor(server, id);
        if (!deleted) {
            return new OperationResult(false, "Door not found: " + id);
        }
        return new OperationResult(true, "Door deleted: " + id);
    }

    public OperationResult reloadDoors() {
        if (server == null) {
            return new OperationResult(false, "Trigger system is not ready.");
        }
        DoorEngine.getInstance().reload(server);
        return new OperationResult(true, "Doors reloaded.");
    }

    public OperationResult armBind(ServerPlayer player, String blueprintId, String triggerType) {
        return armBind(player, blueprintId, triggerType, "");
    }

    public OperationResult armBind(ServerPlayer player, String blueprintId, String triggerType, String chainId) {
        if (player == null) {
            return new OperationResult(false, "This command must be executed by a player.");
        }
        String id = normalizeId(blueprintId);
        TriggerBlueprint blueprint = blueprintLoader.getBlueprint(id);
        if (blueprint == null) {
            // If admin edited files manually, try to refresh loader before failing.
            blueprintLoader.reloadBlueprints();
            blueprint = blueprintLoader.getBlueprint(id);
        }
        if (blueprint == null) {
            if (CommandScriptRegistry.hasCommandScript(id)) {
                // Simple UX: bind directly by scene id via the same command.
                return armNpcSceneBind(player, id, triggerType, chainId);
            }
            NpcReplayScript replayScript = NpcReplayLoader.getInstance().get(id);
            if (replayScript != null) {
                // Simple UX: bind directly by replay id if recorder metadata has source npc.
                String npcId = replayScript.sourceNpcId();
                if (npcId == null || npcId.isBlank()) {
                    npcId = resolveNearestNpcId(player);
                }
                if (npcId == null || npcId.isBlank()) {
                    return new OperationResult(
                            false,
                            "Replay '" + id + "' has no source_npc metadata. " +
                                    "Use '/marallyzen trigger npc bind_replay <npc_id> " + id + "' once, " +
                                    "or re-record replay with current build."
                    );
                }
                return armNpcReplayBind(player, npcId, id, triggerType, chainId);
            }
            if (DoorEngine.getInstance().getDoor(player.level().getServer(), id) != null) {
                return armDoorBind(player, id, triggerType, chainId);
            }
            return new OperationResult(false, "Blueprint not found: " + id);
        }
        String normalizedType = normalizeType(triggerType);
        if (triggerRegistry.get(normalizedType) == null) {
            return new OperationResult(false, "Unknown trigger type: " + normalizedType);
        }
        String normalizedPreviousId = normalizeId(chainId);
        if (!normalizedPreviousId.isBlank()) {
            TriggerInstance previous = instanceManager.get(normalizedPreviousId);
            if (previous == null) {
                return new OperationResult(false, "Previous trigger instance not found: " + normalizedPreviousId);
            }
            String existingNext = instanceManager.getChainNextInstanceId(previous);
            if (!existingNext.isBlank() && instanceManager.get(existingNext) != null) {
                return new OperationResult(
                        false,
                        "Trigger '" + normalizedPreviousId + "' already has next step: '" + existingNext + "'."
                );
            }
        }
        instanceManager.setPendingBind(player.getUUID(), id, normalizedType, normalizedPreviousId);
        if (normalizedPreviousId.isBlank()) {
            return new OperationResult(true, "Bind armed for blueprint '" + id + "'. Right-click a block to bind.");
        }
        return new OperationResult(
                true,
                "Bind armed for blueprint '" + id + "' after trigger '" + normalizedPreviousId + "'. Right-click a block to bind."
        );
    }

    public OperationResult armNpcSceneBind(ServerPlayer player, String sceneId, String triggerType) {
        return armNpcSceneBind(player, sceneId, triggerType, "");
    }

    public OperationResult armNpcSceneBind(ServerPlayer player, String sceneId, String triggerType, String chainId) {
        if (player == null) {
            return new OperationResult(false, "This command must be executed by a player.");
        }
        String normalizedScene = normalizeId(sceneId);
        if (normalizedScene.isBlank()) {
            return new OperationResult(false, "Scene id is required.");
        }
        if (!CommandScriptRegistry.hasCommandScript(normalizedScene)) {
            if (NpcReplayLoader.getInstance().get(normalizedScene) != null) {
                return new OperationResult(
                        false,
                        "ID '" + normalizedScene + "' is a replay script, not a scene script. " +
                                "Use: /marallyzen trigger npc bind_replay <npc_id> " + normalizedScene
                );
            }
            return new OperationResult(false, "NPC scene script not found: " + normalizedScene);
        }
        String blueprintId = buildNpcSceneBlueprintId(normalizedScene);
        OperationResult ensureResult = ensureNpcSceneBlueprint(blueprintId, normalizedScene);
        if (!ensureResult.success()) {
            return ensureResult;
        }
        return armBind(player, blueprintId, triggerType, chainId);
    }

    public OperationResult armNpcReplayBind(ServerPlayer player, String npcId, String replayId, String triggerType) {
        return armNpcReplayBind(player, npcId, replayId, triggerType, "");
    }

    public OperationResult armNpcReplayBind(ServerPlayer player, String npcId, String replayId, String triggerType, String chainId) {
        if (player == null) {
            return new OperationResult(false, "This command must be executed by a player.");
        }
        String normalizedNpc = normalizeId(npcId);
        String normalizedReplay = normalizeId(replayId);
        if (normalizedNpc.isBlank() || normalizedReplay.isBlank()) {
            return new OperationResult(false, "Both npc_id and replay_id are required.");
        }
        if (NpcReplayLoader.getInstance().get(normalizedReplay) == null) {
            return new OperationResult(false, "Replay script not found: " + normalizedReplay);
        }
        String blueprintId = buildNpcReplayBlueprintId(normalizedNpc, normalizedReplay);
        OperationResult ensureResult = ensureNpcReplayBlueprint(blueprintId, normalizedNpc, normalizedReplay);
        if (!ensureResult.success()) {
            return ensureResult;
        }
        return armBind(player, blueprintId, triggerType, chainId);
    }

    public OperationResult armDoorBind(ServerPlayer player, String doorId, String triggerType) {
        return armDoorBind(player, doorId, triggerType, "");
    }

    public OperationResult armDoorBind(ServerPlayer player, String doorId, String triggerType, String chainId) {
        if (player == null) {
            return new OperationResult(false, "This command must be executed by a player.");
        }
        String normalizedDoor = normalizeId(doorId);
        if (normalizedDoor.isBlank()) {
            return new OperationResult(false, "Door id is required.");
        }
        if (DoorEngine.getInstance().getDoor(player.level().getServer(), normalizedDoor) == null) {
            return new OperationResult(false, "Door not found: " + normalizedDoor);
        }
        String blueprintId = buildDoorBlueprintId(normalizedDoor);
        OperationResult ensureResult = ensureDoorBlueprint(blueprintId, normalizedDoor);
        if (!ensureResult.success()) {
            return ensureResult;
        }
        return armBind(player, blueprintId, triggerType, chainId);
    }

    public OperationResult removeInstance(String instanceId, ServerPlayer actor) {
        if (!initialized || server == null) {
            return new OperationResult(false, "Trigger system is not ready.");
        }
        TriggerInstance instance = instanceManager.get(instanceId);
        if (instance == null) {
            return new OperationResult(false, "Instance not found: " + normalizeId(instanceId));
        }
        ServerLevel level = resolveLevel(instance.dimensionId());
        if (level != null) {
            engine.clearPlacedStructure(instance, level);
        }
        instanceManager.remove(instance.id());
        engine.rebuildZones();
        syncTriggerBindingsToAll();
        if (actor != null) {
            AdminHudNotifier.removed(actor);
        }
        return new OperationResult(true, "Trigger instance removed: " + instance.id());
    }

    public OperationResult activateInstance(ServerPlayer player, String instanceId) {
        if (!initialized || server == null) {
            return new OperationResult(false, "Trigger system is not ready.");
        }
        boolean activated = engine.activateManual(instanceId, player);
        if (!activated) {
            return new OperationResult(false, "Failed to activate instance: " + normalizeId(instanceId));
        }
        return new OperationResult(true, "Trigger activated: " + normalizeId(instanceId));
    }

    public OperationResult editTriggerType(String instanceId, String triggerType) {
        TriggerInstance instance = instanceManager.get(instanceId);
        if (instance == null) {
            return new OperationResult(false, "Instance not found: " + normalizeId(instanceId));
        }
        String normalized = normalizeType(triggerType);
        if (triggerRegistry.get(normalized) == null) {
            return new OperationResult(false, "Unknown trigger type: " + normalized);
        }
        instance.setTriggerType(normalized);
        instanceManager.persistInstance(instance);
        instanceManager.refreshActiveFile(instance);
        syncTriggerBindingsToAll();
        return new OperationResult(true, "Instance trigger updated: " + instance.id() + " -> " + normalized);
    }

    public OperationResult editCooldown(String instanceId, int ticks) {
        TriggerInstance instance = instanceManager.get(instanceId);
        if (instance == null) {
            return new OperationResult(false, "Instance not found: " + normalizeId(instanceId));
        }
        instance.setCooldownTicks(ticks);
        instanceManager.persistInstance(instance);
        instanceManager.refreshActiveFile(instance);
        return new OperationResult(true, "Instance cooldown updated: " + instance.id() + " -> " + ticks + " ticks");
    }

    public OperationResult linkSequence(List<String> rawInstanceIds) {
        if (rawInstanceIds == null || rawInstanceIds.isEmpty()) {
            return new OperationResult(false, "At least 2 instance ids are required.");
        }
        Set<String> orderedUnique = new LinkedHashSet<>();
        for (String raw : rawInstanceIds) {
            String id = normalizeId(raw);
            if (!id.isBlank()) {
                orderedUnique.add(id);
            }
        }
        if (orderedUnique.size() < 2) {
            return new OperationResult(false, "At least 2 unique instance ids are required.");
        }
        List<String> ids = new ArrayList<>(orderedUnique);
        for (String id : ids) {
            if (instanceManager.get(id) == null) {
                return new OperationResult(false, "Instance not found: " + id);
            }
        }
        instanceManager.linkSequence(ids);
        return new OperationResult(true, "Sequence linked: " + String.join(" -> ", ids));
    }

    public OperationResult reloadTriggers(boolean hardReset) {
        if (server == null) {
            return new OperationResult(false, "Trigger system is not ready.");
        }
        reload(server);
        if (!hardReset) {
            return new OperationResult(true, "Triggers reloaded (soft).");
        }

        int resetCount = 0;
        for (TriggerInstance instance : instanceManager.allInstances()) {
            if (instance == null) {
                continue;
            }
            ServerLevel level = resolveLevel(instance.dimensionId());
            if (level != null && instance.structureState() != TriggerInstance.StructureState.HIDDEN) {
                engine.clearPlacedStructure(instance, level);
            }
            instance.setStructureState(TriggerInstance.StructureState.HIDDEN);
            instance.setAnimationTick(0);
            instance.setCooldownRemaining(0);
            instance.setNextTimerGameTime(0L);
            instance.setLastPowered(false);
            instance.playersInside().clear();
            instance.persistentData().remove("chain_wait_activity_unlock");

            int chainStep = instanceManager.getChainStep(instance);
            if (chainStep > 0) {
                instanceManager.setChainLocked(instance, chainStep > 1);
            } else {
                instanceManager.setChainLocked(instance, false);
            }

            instanceManager.persistInstance(instance);
            instanceManager.refreshActiveFile(instance);
            resetCount++;
        }
        engine.rebuildZones();
        syncTriggerBindingsToAll();
        return new OperationResult(true, "Triggers reloaded (hard): reset " + resetCount + " instance(s).");
    }

    public ServerLevel resolveLevel(String dimensionId) {
        if (server == null) {
            return null;
        }
        try {
            ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    Identifier.parse(dimensionId)
            );
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                return level;
            }
        } catch (Exception ignored) {
        }
        return server.overworld();
    }

    public boolean isProtected(ServerLevel level, BlockPos pos, net.minecraft.world.entity.Entity actor) {
        return zoneManager.isProtectedPosition(level, pos, actor);
    }

    public List<String> blueprintIds() {
        return new ArrayList<>(blueprintLoader.getLoadedBlueprintIds());
    }

    public List<String> instanceIds() {
        return new ArrayList<>(instanceManager.allIds());
    }

    public void syncTriggerBindings(ServerPlayer player) {
        if (player == null || server == null) {
            return;
        }
        NetworkHelper.sendToPlayer(player, buildBindSyncPacket());
    }

    public void syncTriggerBindingsToAll() {
        if (server == null) {
            return;
        }
        TriggerBindSyncPacket packet = buildBindSyncPacket();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkHelper.sendToPlayer(player, packet);
        }
    }

    private TriggerBindSyncPacket buildBindSyncPacket() {
        Collection<TriggerInstance> all = instanceManager.allInstances();
        List<TriggerBindSyncPacket.TriggerBindEntry> entries = new ArrayList<>(all.size());
        for (TriggerInstance instance : all) {
            if (instance == null) {
                continue;
            }
            entries.add(new TriggerBindSyncPacket.TriggerBindEntry(instance.dimensionId(), instance.bindPos()));
        }
        return new TriggerBindSyncPacket(entries);
    }

    private String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String buildNpcSceneBlueprintId(String sceneId) {
        String clean = sceneId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        if (clean.isBlank()) {
            clean = "scene";
        }
        return "npc_scene_" + clean;
    }

    private String buildNpcReplayBlueprintId(String npcId, String replayId) {
        String npc = npcId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        String replay = replayId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        if (npc.isBlank()) {
            npc = "npc";
        }
        if (replay.isBlank()) {
            replay = "replay";
        }
        return "npc_replay_" + npc + "_" + replay;
    }

    private String buildDoorBlueprintId(String doorId) {
        String clean = doorId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        if (clean.isBlank()) {
            clean = "door";
        }
        return "door_" + clean;
    }

    private OperationResult ensureNpcSceneBlueprint(String blueprintId, String sceneId) {
        TriggerBlueprint existing = blueprintLoader.getBlueprint(blueprintId);
        if (existing != null) {
            return new OperationResult(true, "Blueprint ready: " + blueprintId);
        }

        OperationResult activityResult = ensureNpcSceneActivityScript(sceneId);
        if (!activityResult.success()) {
            return activityResult;
        }

        TriggerBlueprint.Settings defaults = TriggerBlueprint.Settings.defaults();
        TriggerBlueprint.Settings.ActionSettings action = new TriggerBlueprint.Settings.ActionSettings(
                "activity",
                "",
                "",
                "",
                "",
                "",
                "",
                "npc",
                sceneId,
                "",
                java.util.Map.of("scene_id", normalizeId(sceneId))
        );
        TriggerBlueprint blueprint = new TriggerBlueprint(
                blueprintId,
                new BlockPos(1, 1, 1),
                null,
                List.of(),
                new TriggerBlueprint.Settings(
                        defaults.animation(),
                        defaults.cooldownTicks(),
                        defaults.timerIntervalTicks(),
                        defaults.zoneRadius(),
                        defaults.sound(),
                        defaults.particle(),
                        defaults.condition(),
                        action
                )
        );
        Path file = blueprintLoader.getBlueprintDirectory().resolve(blueprintId + ".json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(blueprint.toJson(), writer);
            }
            blueprintLoader.reloadBlueprints();
        } catch (Exception e) {
            return new OperationResult(false, "Failed to create npc_scene blueprint: " + e.getMessage());
        }
        return new OperationResult(true, "Blueprint created: " + blueprintId);
    }

    private OperationResult ensureNpcReplayBlueprint(String blueprintId, String npcId, String replayId) {
        TriggerBlueprint existing = blueprintLoader.getBlueprint(blueprintId);
        if (existing != null) {
            return new OperationResult(true, "Blueprint ready: " + blueprintId);
        }

        OperationResult activityResult = ensureNpcReplayActivityScript(replayId);
        if (!activityResult.success()) {
            return activityResult;
        }

        TriggerBlueprint.Settings defaults = TriggerBlueprint.Settings.defaults();
        TriggerBlueprint.Settings.ActionSettings action = new TriggerBlueprint.Settings.ActionSettings(
                "activity",
                npcId,
                "",
                "",
                "",
                "",
                "",
                "npc",
                replayId,
                "",
                java.util.Map.of("npc_id", normalizeId(npcId), "replay_id", normalizeId(replayId))
        );
        TriggerBlueprint blueprint = new TriggerBlueprint(
                blueprintId,
                new BlockPos(1, 1, 1),
                null,
                List.of(),
                new TriggerBlueprint.Settings(
                        defaults.animation(),
                        defaults.cooldownTicks(),
                        defaults.timerIntervalTicks(),
                        defaults.zoneRadius(),
                        defaults.sound(),
                        defaults.particle(),
                        defaults.condition(),
                        action
                )
        );
        Path file = blueprintLoader.getBlueprintDirectory().resolve(blueprintId + ".json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(blueprint.toJson(), writer);
            }
            blueprintLoader.reloadBlueprints();
        } catch (Exception e) {
            return new OperationResult(false, "Failed to create npc_replay blueprint: " + e.getMessage());
        }
        return new OperationResult(true, "Blueprint created: " + blueprintId);
    }

    private OperationResult ensureDoorBlueprint(String blueprintId, String doorId) {
        TriggerBlueprint existing = blueprintLoader.getBlueprint(blueprintId);
        if (existing != null) {
            return new OperationResult(true, "Blueprint ready: " + blueprintId);
        }

        OperationResult activityResult = ensureDoorActivityScript(doorId);
        if (!activityResult.success()) {
            return activityResult;
        }

        TriggerBlueprint.Settings defaults = TriggerBlueprint.Settings.defaults();
        TriggerBlueprint.Settings.ActionSettings action = new TriggerBlueprint.Settings.ActionSettings(
                "activity",
                "",
                "",
                "",
                "",
                "",
                "",
                "door",
                doorId,
                "",
                java.util.Map.of("door_id", normalizeId(doorId))
        );
        TriggerBlueprint blueprint = new TriggerBlueprint(
                blueprintId,
                new BlockPos(1, 1, 1),
                null,
                List.of(),
                new TriggerBlueprint.Settings(
                        defaults.animation(),
                        defaults.cooldownTicks(),
                        defaults.timerIntervalTicks(),
                        defaults.zoneRadius(),
                        "",
                        "",
                        defaults.condition(),
                        action
                )
        );
        Path file = blueprintLoader.getBlueprintDirectory().resolve(blueprintId + ".json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(blueprint.toJson(), writer);
            }
            blueprintLoader.reloadBlueprints();
        } catch (Exception e) {
            return new OperationResult(false, "Failed to create door blueprint: " + e.getMessage());
        }
        return new OperationResult(true, "Blueprint created: " + blueprintId);
    }

    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "block_use";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private OperationResult ensureDoorActivityScript(String doorId) {
        String id = normalizeId(doorId);
        if (id.isBlank()) {
            return new OperationResult(false, "Door id is required for activity script.");
        }
        Path root = activityRegistry.loader().getRootDirectory();
        Path file = root.resolve("door").resolve(id + ".json");
        if (Files.exists(file)) {
            return new OperationResult(true, "Activity script ready: door/" + id);
        }
        Path template = root.resolve("door").resolve("example.json");
        if (Files.exists(template)) {
            try {
                Files.createDirectories(file.getParent());
                Files.copy(template, file);
                Marallyzen.LOGGER.warn("TriggerModule: door activity script '{}' created from template {}", id, template);
                activityRegistry.reload();
                return new OperationResult(true, "Activity script created from template: door/" + id);
            } catch (Exception e) {
                return new OperationResult(false, "Failed to create door activity script from template: " + e.getMessage());
            }
        }
        return new OperationResult(false, "Activity script missing: door/" + id + ". Create JSON under config/marallyzen/activities/door/");
    }

    private OperationResult ensureNpcReplayActivityScript(String replayId) {
        String id = normalizeId(replayId);
        if (id.isBlank()) {
            return new OperationResult(false, "Replay id is required for activity script.");
        }
        Path root = activityRegistry.loader().getRootDirectory();
        Path file = root.resolve("npc").resolve(id + ".json");
        if (Files.exists(file)) {
            return new OperationResult(true, "Activity script ready: npc/" + id);
        }
        return new OperationResult(false, "Activity script missing: npc/" + id + ". Create JSON under config/marallyzen/activities/npc/");
    }

    private OperationResult ensureNpcSceneActivityScript(String sceneId) {
        String id = normalizeId(sceneId);
        if (id.isBlank()) {
            return new OperationResult(false, "Scene id is required for activity script.");
        }
        Path root = activityRegistry.loader().getRootDirectory();
        Path file = root.resolve("npc").resolve(id + ".json");
        if (Files.exists(file)) {
            return new OperationResult(true, "Activity script ready: npc/" + id);
        }
        return new OperationResult(false, "Activity script missing: npc/" + id + ". Create JSON under config/marallyzen/activities/npc/");
    }

    private String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private void clearWorldArea(ServerLevel level, BlockPos min, BlockPos max) {
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private String resolveNearestNpcId(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        var registry = NpcClickHandler.getRegistry();
        double bestDistSq = Double.MAX_VALUE;
        String bestId = "";
        for (var entity : registry.getSpawnedNpcs()) {
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                continue;
            }
            if (entity.level() != player.level()) {
                continue;
            }
            double distSq = entity.distanceToSqr(player);
            if (distSq > (16.0D * 16.0D)) {
                continue;
            }
            String npcId = registry.getNpcId(entity);
            if (npcId == null || npcId.isBlank()) {
                continue;
            }
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestId = npcId;
            }
        }
        return bestId;
    }
}
