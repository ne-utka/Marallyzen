package neutka.marallys.marallyzen.door;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.activity.ActivityScript;
import neutka.marallys.marallyzen.activity.ScreenShakeEngine;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.TriggerAnimationStartPacket;
import neutka.marallys.marallyzen.network.TriggerAnimationStopPacket;
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
        saveState(id, new DoorSavedData.DoorState(true, false, 0, DoorAnimationDirection.CLOSING.name(), 60, "", false, true));
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
        return activate(server, doorId, null);
    }

    public boolean activate(MinecraftServer server, String doorId, ActivityScript script) {
        if (server == null) {
            return false;
        }
        ensureLoaded(server);
        String id = normalizeId(doorId);
        DoorDefinition definition = registry.getDoor(id);
        if (definition == null) {
            return false;
        }
        if (script == null) {
            Marallyzen.LOGGER.error("DoorEngine: activity script is required for door '{}'", id);
            return false;
        }
        DoorSavedData.DoorState state = getOrCreateState(id);
        if (state.animating()) {
            return false;
        }
        boolean protect = script.physics().collision();
        boolean protectOnSpawn = script.physics().solidOnSpawn();
        if (state.opened()) {
            DoorSavedData.DoorState next = new DoorSavedData.DoorState(
                    false,
                    true,
                    0,
                    DoorAnimationDirection.CLOSING.name(),
                    Math.max(1, script.animation().durationTicks()),
                    script.effects().soundEnd(),
                    protect && !protectOnSpawn,
                    protect
            );
            saveState(id, next);
            if (protect && protectOnSpawn) {
                zoneManager.registerZone(id, definition.dimensionId(), definition.bounds().min(), definition.bounds().max());
            } else if (!protect) {
                zoneManager.removeZone(id);
            }
            sendDoorAnimationStart(definition, script);
            applyStartEffects(resolveLevel(server, definition.dimensionId()), definition.anchor(), script);
            return true;
        }

        ServerLevel level = resolveLevel(server, definition.dimensionId());
        if (level != null) {
            clearArea(level, definition.blocks());
        }
        saveState(id, new DoorSavedData.DoorState(true, false, 0, DoorAnimationDirection.OPENING.name(), 0, "", false, state.collision()));
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
                saveState(doorId, new DoorSavedData.DoorState(true, false, 0, DoorAnimationDirection.OPENING.name(), 0, "", false, state.collision()));
                zoneManager.removeZone(doorId);
                continue;
            }
            ServerLevel level = resolveLevel(server, definition.dimensionId());
            if (level == null) {
                continue;
            }

            int tick = state.animationTick();
            int duration = Math.max(1, state.durationTicks());
            if (tick + 1 >= duration) {
                placeAllBlocks(level, definition);
                NetworkHelper.sendToAll(new TriggerAnimationStopPacket(buildAnimationId(doorId)));
                playSound(level, definition.anchor(), state.soundEnd());
                if (state.pendingProtection()) {
                    zoneManager.registerZone(doorId, definition.dimensionId(), definition.bounds().min(), definition.bounds().max());
                }
                saveState(doorId, new DoorSavedData.DoorState(false, false, 0, DoorAnimationDirection.CLOSING.name(), duration, state.soundEnd(), false, state.collision()));
            } else {
                saveState(doorId, new DoorSavedData.DoorState(false, true, tick + 1, DoorAnimationDirection.CLOSING.name(), duration, state.soundEnd(), state.pendingProtection(), state.collision()));
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
            if (!state.opened() && state.collision()) {
                zoneManager.registerZone(doorId, definition.dimensionId(), definition.bounds().min(), definition.bounds().max());
            }
        }
    }

    private void sendDoorAnimationStart(DoorDefinition definition, ActivityScript script) {
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
        BlockPos spawnOffset = resolveSpawnOffset(definition, script);
        TriggerAnimationStartPacket packet = new TriggerAnimationStartPacket(
                buildAnimationId(definition.id()),
                definition.dimensionId(),
                origin,
                spawnOffset,
                Math.max(1, script.animation().durationTicks()),
                script.animation().mode(),
                script.animation().easing(),
                visuals
        );
        NetworkHelper.sendToAll(packet);
    }

    private void applyStartEffects(ServerLevel level, BlockPos anchor, ActivityScript script) {
        if (level == null || anchor == null || script == null) {
            return;
        }
        playSound(level, anchor, script.effects().soundStart());
        spawnParticles(level, anchor, script.effects().particles());
        ScreenShakeEngine.broadcast(level, Vec3.atCenterOf(anchor), script.effects().shake());
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
            state = new DoorSavedData.DoorState(true, false, 0, DoorAnimationDirection.CLOSING.name(), 60, "", false, true);
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

    private BlockPos resolveSpawnOffset(DoorDefinition definition, ActivityScript script) {
        ActivityScript.Animation animation = script.animation();
        if (animation.spawnOffset() != null && !animation.spawnOffset().equals(BlockPos.ZERO)) {
            return animation.spawnOffset();
        }
        int width = Math.max(1, definition.bounds().max().getX() - definition.bounds().min().getX() + 1);
        int height = Math.max(1, definition.bounds().max().getY() - definition.bounds().min().getY() + 1);
        int depth = Math.max(1, definition.bounds().max().getZ() - definition.bounds().min().getZ() + 1);
        DirectionVec dir = DirectionVec.from(animation.direction(), animation.mode());
        double distance = animation.distance() > 0.0D ? animation.distance() : dir.defaultDistance(width, height, depth);
        int ox = (int) Math.round(-dir.x * distance);
        int oy = (int) Math.round(-dir.y * distance);
        int oz = (int) Math.round(-dir.z * distance);
        return new BlockPos(ox, oy, oz);
    }

    private void playSound(ServerLevel level, BlockPos anchor, String soundId) {
        if (level == null || anchor == null || soundId == null || soundId.isBlank()) {
            return;
        }
        Identifier id = Identifier.tryParse(soundId.trim());
        if (id == null) {
            return;
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (event == null) {
            return;
        }
        float pitch = 0.8F + (level.getRandom().nextFloat() * 0.4F);
        level.playSound(null, anchor, event, SoundSource.BLOCKS, 0.8F, pitch);
    }

    private void spawnParticles(ServerLevel level, BlockPos anchor, ActivityScript.ParticleSettings settings) {
        if (level == null || anchor == null || settings == null) {
            return;
        }
        if (settings.count() <= 0 || settings.type() == null || settings.type().isBlank()) {
            return;
        }
        Identifier id = Identifier.tryParse(settings.type().trim());
        if (id == null) {
            return;
        }
        var particleType = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
        if (!(particleType instanceof ParticleOptions options)) {
            return;
        }
        level.sendParticles(
                options,
                anchor.getX() + 0.5D,
                anchor.getY() + 0.5D,
                anchor.getZ() + 0.5D,
                settings.count(),
                settings.spread(),
                settings.spread(),
                settings.spread(),
                settings.speed()
        );
    }

    private record DirectionVec(int x, int y, int z, String mode) {
        static DirectionVec from(String rawDirection, String mode) {
            String dir = rawDirection == null ? "" : rawDirection.trim().toLowerCase(Locale.ROOT);
            return switch (dir) {
                case "up" -> new DirectionVec(0, 1, 0, mode);
                case "down" -> new DirectionVec(0, -1, 0, mode);
                case "north" -> new DirectionVec(0, 0, -1, mode);
                case "south" -> new DirectionVec(0, 0, 1, mode);
                case "east" -> new DirectionVec(1, 0, 0, mode);
                case "west" -> new DirectionVec(-1, 0, 0, mode);
                default -> "horizontal".equalsIgnoreCase(mode)
                        ? new DirectionVec(0, 0, 1, mode)
                        : new DirectionVec(0, -1, 0, mode);
            };
        }

        int defaultDistance(int width, int height, int depth) {
            if (y != 0) {
                return height;
            }
            if (x != 0) {
                return width;
            }
            return depth;
        }
    }
}
