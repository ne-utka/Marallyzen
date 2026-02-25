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
import java.util.List;
import java.util.Locale;

public final class TriggerModule {
    public record OperationResult(boolean success, String message) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final TriggerBlueprintLoader blueprintLoader = new TriggerBlueprintLoader();
    private final SelectionManager selectionManager = new SelectionManager();
    private final TriggerRegistry triggerRegistry = new TriggerRegistry();
    private final TriggerZoneManager zoneManager = new TriggerZoneManager();
    private final TriggerInstanceManager instanceManager = new TriggerInstanceManager(blueprintLoader);
    private final TriggerEngine engine = new TriggerEngine(blueprintLoader, instanceManager, triggerRegistry, zoneManager);

    private MinecraftServer server;
    private boolean initialized;

    public void initialize(MinecraftServer server) {
        this.server = server;
        blueprintLoader.reloadBlueprints();
        if (server == null || server.overworld() == null) {
            initialized = false;
            return;
        }
        TriggerSavedData savedData = TriggerSavedData.get(server.overworld());
        instanceManager.attachSavedData(savedData);
        instanceManager.rebuildFromSaved();
        engine.rebuildZones();
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
        if (this.server == null || this.server.overworld() == null) {
            initialized = false;
            return;
        }
        TriggerSavedData savedData = TriggerSavedData.get(this.server.overworld());
        instanceManager.attachSavedData(savedData);
        instanceManager.rebuildFromSaved();
        engine.rebuildZones();
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

    public OperationResult armBind(ServerPlayer player, String blueprintId, String triggerType) {
        if (player == null) {
            return new OperationResult(false, "This command must be executed by a player.");
        }
        String id = normalizeId(blueprintId);
        if (blueprintLoader.getBlueprint(id) == null) {
            return new OperationResult(false, "Blueprint not found: " + id);
        }
        String normalizedType = normalizeType(triggerType);
        if (triggerRegistry.get(normalizedType) == null) {
            return new OperationResult(false, "Unknown trigger type: " + normalizedType);
        }
        instanceManager.setPendingBind(player.getUUID(), id, normalizedType);
        return new OperationResult(true, "Bind armed for blueprint '" + id + "'. Right-click a block to bind.");
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

    private String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "block_use";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
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
}
