package neutka.marallys.marallyzen.door;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.TriggerAnimationStartPacket;
import neutka.marallys.marallyzen.network.TriggerAnimationStopPacket;
import neutka.marallys.marallyzen.network.TriggerScreenShakePacket;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlockStateCodec;
import neutka.marallys.marallyzen.trigger.command.SelectionManager;
import neutka.marallys.marallyzen.trigger.event.AdminHudNotifier;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DoorEngine {
    private static final DoorEngine INSTANCE = new DoorEngine();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DOOR_ANIMATION_PREFIX = "door:";

    private final DoorRegistry registry = new DoorRegistry();
    private final DoorZoneManager zoneManager = new DoorZoneManager();
    private DoorSavedData savedData;
    private boolean initialized;

    private DoorEngine() {
    }

    public static DoorEngine getInstance() {
        return INSTANCE;
    }

    public void ensureLoaded(MinecraftServer server) {
        if (initialized || server == null || server.overworld() == null) {
            return;
        }
        registry.reload();
        savedData = DoorSavedData.get(server.overworld());
        rebuildZones(server);
        initialized = true;
    }

    public void reload(MinecraftServer server) {
        if (server == null) {
            return;
        }
        registry.reload();
        if (server.overworld() != null) {
            savedData = DoorSavedData.get(server.overworld());
        }
        rebuildZones(server);
        initialized = true;
    }

    public boolean createDoor(ServerPlayer player, SelectionManager.Selection selection, String doorId) {
        if (player == null || selection == null || !selection.isComplete()) {
            return false;
        }
        String id = normalizeId(doorId);
        if (id.isBlank()) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        ensureLoaded(server);
        if (registry.getDoor(id) != null) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        BlockPos min = selection.min();
        BlockPos max = selection.max();
        List<DoorDefinition.BlockEntry> blocks = new ArrayList<>();
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    blocks.add(new DoorDefinition.BlockEntry(x, y, z, TriggerBlockStateCodec.encode(state)));
                }
            }
        }

        BlockPos anchor = new BlockPos(
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2
        );
        DoorDefinition definition = new DoorDefinition(
                id,
                level.dimension().identifier().toString(),
                new DoorDefinition.Bounds(min, max),
                anchor,
                blocks,
                DoorDefinition.AnimationSettings.defaults(),
                DoorDefinition.ShakeSettings.defaults()
        );

        Path file = registry.getDoorDirectory().resolve(id + ".json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(definition.toJson(), writer);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("DoorEngine: failed to write door definition {}", id, e);
            return false;
        }

        registry.reload();
        saveState(id, new DoorSavedData.DoorState(true, false, 0, DoorAnimationDirection.CLOSING.name()));
        clearArea(level, blocks);
        zoneManager.removeZone(id);
        if (player != null) {
            AdminHudNotifier.doorSaved(player);
        }
        return true;
    }

    public boolean deleteDoor(MinecraftServer server, String doorId) {
        if (server == null) {
            return false;
        }
        ensureLoaded(server);
        String id = normalizeId(doorId);
        DoorDefinition definition = registry.getDoor(id);
        if (definition == null) {
            return false;
        }
        ServerLevel level = resolveLevel(server, definition.dimensionId());
        if (level != null) {
            clearArea(level, definition.blocks());
        }
        NetworkHelper.sendToAll(new TriggerAnimationStopPacket(buildAnimationId(id)));
        registry.deleteDoorFile(id);
        registry.reload();
        removeState(id);
        zoneManager.removeZone(id);
        return true;
    }

    public boolean activate(MinecraftServer server, String doorId) {
        if (server == null) {
            return false;
        }
        ensureLoaded(server);
        String id = normalizeId(doorId);
        DoorDefinition definition = registry.getDoor(id);
        if (definition == null) {
            return false;
        }
        DoorSavedData.DoorState state = getOrCreateState(id);
        if (state.animating()) {
            return false;
        }
        if (state.opened()) {
            DoorSavedData.DoorState next = new DoorSavedData.DoorState(
                    false,
                    true,
                    0,
                    DoorAnimationDirection.CLOSING.name()
            );
            saveState(id, next);
            zoneManager.registerZone(id, definition.dimensionId(), definition.bounds().min(), definition.bounds().max());
            sendDoorAnimationStart(definition);
            broadcastShake(server, definition);
            return true;
        }

        ServerLevel level = resolveLevel(server, definition.dimensionId());
        if (level != null) {
            clearArea(level, definition.blocks());
        }
        saveState(id, new DoorSavedData.DoorState(true, false, 0, DoorAnimationDirection.OPENING.name()));
        zoneManager.removeZone(id);
        return true;
    }

    public void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        ensureLoaded(server);
        for (String doorId : registry.getDoorIds()) {
            DoorDefinition definition = registry.getDoor(doorId);
            if (definition == null) {
                continue;
            }
            DoorSavedData.DoorState state = getOrCreateState(doorId);
            if (!state.animating()) {
                continue;
            }
            if (state.direction() != DoorAnimationDirection.CLOSING) {
                saveState(doorId, new DoorSavedData.DoorState(true, false, 0, DoorAnimationDirection.OPENING.name()));
                zoneManager.removeZone(doorId);
                continue;
            }
            ServerLevel level = resolveLevel(server, definition.dimensionId());
            if (level == null) {
                continue;
            }

            int tick = state.animationTick();
            int duration = Math.max(1, definition.animation().durationTicks());
            if (tick + 1 >= duration) {
                placeAllBlocks(level, definition);
                NetworkHelper.sendToAll(new TriggerAnimationStopPacket(buildAnimationId(doorId)));
                playCloseSound(level, definition.anchor());
                saveState(doorId, new DoorSavedData.DoorState(false, false, 0, DoorAnimationDirection.CLOSING.name()));
            } else {
                saveState(doorId, new DoorSavedData.DoorState(false, true, tick + 1, DoorAnimationDirection.CLOSING.name()));
            }
        }
    }

    public boolean isProtected(ServerLevel level, BlockPos pos, net.minecraft.world.entity.Entity actor) {
        return zoneManager.isProtectedPosition(level, pos, actor);
    }

    public List<String> doorIds(MinecraftServer server) {
        ensureLoaded(server);
        return new ArrayList<>(registry.getDoorIds());
    }

    public DoorDefinition getDoor(MinecraftServer server, String doorId) {
        if (doorId == null || doorId.isBlank()) {
            return null;
        }
        ensureLoaded(server);
        return registry.getDoor(doorId);
    }

    private void rebuildZones(MinecraftServer server) {
        zoneManager.clear();
        if (server == null) {
            return;
        }
        for (String doorId : registry.getDoorIds()) {
            DoorDefinition definition = registry.getDoor(doorId);
            if (definition == null) {
                continue;
            }
            DoorSavedData.DoorState state = getOrCreateState(doorId);
            if (!state.opened()) {
                zoneManager.registerZone(doorId, definition.dimensionId(), definition.bounds().min(), definition.bounds().max());
            }
        }
    }

    private void sendDoorAnimationStart(DoorDefinition definition) {
        List<TriggerAnimationStartPacket.BlockVisual> visuals = new ArrayList<>(definition.blocks().size());
        BlockPos origin = definition.bounds().min();
        for (DoorDefinition.BlockEntry entry : definition.blocks()) {
            BlockPos local = new BlockPos(
                    entry.x() - origin.getX(),
                    entry.y() - origin.getY(),
                    entry.z() - origin.getZ()
            );
            visuals.add(new TriggerAnimationStartPacket.BlockVisual(local, entry.state()));
        }
        int height = Math.max(1, definition.bounds().max().getY() - definition.bounds().min().getY() + 1);
        BlockPos spawnOffset = new BlockPos(0, height + 2, 0);
        TriggerAnimationStartPacket packet = new TriggerAnimationStartPacket(
                buildAnimationId(definition.id()),
                definition.dimensionId(),
                origin,
                spawnOffset,
                Math.max(1, definition.animation().durationTicks()),
                "door_drop",
                definition.animation().easing(),
                visuals
        );
        NetworkHelper.sendToAll(packet);
    }

    private void broadcastShake(MinecraftServer server, DoorDefinition definition) {
        if (server == null) {
            return;
        }
        DoorDefinition.ShakeSettings shake = definition.shake();
        int radius = Math.max(1, shake.radius());
        float maxIntensity = Math.max(0.0F, shake.maxIntensity());
        int duration = 15;
        BlockPos anchor = definition.anchor();
        ServerLevel level = resolveLevel(server, definition.dimensionId());
        if (level == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(anchor);
        double radiusSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            double distSq = player.distanceToSqr(center);
            if (distSq > radiusSq) {
                continue;
            }
            double dist = Math.sqrt(distSq);
            float intensity = (float) (maxIntensity * (1.0D - (dist / radius)));
            if (intensity <= 0.0F) {
                continue;
            }
            NetworkHelper.sendToPlayer(player, new TriggerScreenShakePacket(intensity, duration));
        }
    }


    private void playCloseSound(ServerLevel level, BlockPos anchor) {
        if (level == null || anchor == null) {
            return;
        }
        float pitch = 0.8F + (level.getRandom().nextFloat() * 0.4F);
        level.playSound(null, anchor, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8F, pitch);
    }

    private void clearArea(ServerLevel level, List<DoorDefinition.BlockEntry> blocks) {
        for (DoorDefinition.BlockEntry entry : blocks) {
            BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
            level.removeBlockEntity(pos);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void placeAllBlocks(ServerLevel level, DoorDefinition definition) {
        for (DoorDefinition.BlockEntry entry : definition.blocks()) {
            BlockPos target = new BlockPos(entry.x(), entry.y(), entry.z());
            BlockState expected = TriggerBlockStateCodec.decode(entry.state());
            level.setBlock(target, expected, 3);
        }
    }

    private DoorSavedData.DoorState getOrCreateState(String doorId) {
        DoorSavedData.DoorState state = savedData == null ? null : savedData.getDoor(doorId);
        if (state == null) {
            state = new DoorSavedData.DoorState(true, false, 0, DoorAnimationDirection.CLOSING.name());
            saveState(doorId, state);
        }
        return state;
    }

    private void saveState(String doorId, DoorSavedData.DoorState state) {
        if (savedData == null || doorId == null || doorId.isBlank()) {
            return;
        }
        savedData.putDoor(doorId, state);
    }

    private void removeState(String doorId) {
        if (savedData == null || doorId == null || doorId.isBlank()) {
            return;
        }
        savedData.removeDoor(doorId);
    }

    private ServerLevel resolveLevel(MinecraftServer server, String dimensionId) {
        if (server == null) {
            return null;
        }
        try {
            var key = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.Identifier.parse(dimensionId)
            );
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                return level;
            }
        } catch (Exception ignored) {
        }
        return server.overworld();
    }

    private String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String buildAnimationId(String doorId) {
        return DOOR_ANIMATION_PREFIX + normalizeId(doorId);
    }
}
