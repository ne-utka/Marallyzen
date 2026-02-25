package neutka.marallys.marallyzen.trigger.instance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprintLoader;
import neutka.marallys.marallyzen.trigger.storage.TriggerSavedData;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerInstanceManager {
    public record PendingBind(String blueprintId, String triggerType) {
    }

    private record BindKey(String dimensionId, long packedPos) {
        static BindKey of(String dimensionId, BlockPos pos) {
            return new BindKey(dimensionId == null ? "minecraft:overworld" : dimensionId, pos.asLong());
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final TriggerBlueprintLoader blueprintLoader;
    private final Map<String, TriggerInstance> instances = new ConcurrentHashMap<>();
    private final Map<BindKey, Set<String>> instancesByBind = new ConcurrentHashMap<>();
    private final Map<UUID, PendingBind> pendingBinds = new ConcurrentHashMap<>();

    private TriggerSavedData savedData;

    public TriggerInstanceManager(TriggerBlueprintLoader blueprintLoader) {
        this.blueprintLoader = blueprintLoader;
    }

    public void attachSavedData(TriggerSavedData savedData) {
        this.savedData = savedData;
    }

    public void rebuildFromSaved() {
        instances.clear();
        instancesByBind.clear();
        if (savedData == null) {
            return;
        }
        for (Map.Entry<String, TriggerSavedData.TriggerInstanceData> entry : savedData.instances().entrySet()) {
            String id = normalizeId(entry.getKey());
            TriggerSavedData.TriggerInstanceData data = entry.getValue();
            TriggerBlueprint blueprint = blueprintLoader.getBlueprint(data.blueprintId());
            BlockPos fallbackMin = data.worldPos();
            BlockPos fallbackMax = data.worldPos();
            if (blueprint != null) {
                fallbackMax = fallbackMin.offset(
                        Math.max(0, blueprint.size().getX() - 1),
                        Math.max(0, blueprint.size().getY() - 1),
                        Math.max(0, blueprint.size().getZ() - 1)
                );
            }
            TriggerInstance instance = TriggerInstance.fromSavedData(id, data, fallbackMin, fallbackMax);
            if (instance == null) {
                continue;
            }
            instances.put(id, instance);
            indexBind(instance);
        }
    }

    public Collection<TriggerInstance> allInstances() {
        List<TriggerInstance> list = new ArrayList<>(instances.values());
        list.sort(Comparator.comparing(TriggerInstance::id));
        return Collections.unmodifiableList(list);
    }

    public Set<String> allIds() {
        return new TreeSet<>(instances.keySet());
    }

    public TriggerInstance get(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return null;
        }
        return instances.get(normalizeId(instanceId));
    }

    public void setPendingBind(UUID playerId, String blueprintId, String triggerType) {
        if (playerId == null || blueprintId == null || blueprintId.isBlank()) {
            return;
        }
        String normalizedType = triggerType == null || triggerType.isBlank() ? "block_use" : triggerType.trim().toLowerCase(Locale.ROOT);
        pendingBinds.put(playerId, new PendingBind(blueprintId.trim().toLowerCase(Locale.ROOT), normalizedType));
    }

    public PendingBind consumePendingBind(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return pendingBinds.remove(playerId);
    }

    public PendingBind getPendingBind(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return pendingBinds.get(playerId);
    }

    public TriggerInstance createBoundInstance(ServerPlayer player, BlockPos bindPos, PendingBind pending) {
        if (player == null || bindPos == null || pending == null) {
            return null;
        }
        TriggerBlueprint blueprint = blueprintLoader.getBlueprint(pending.blueprintId());
        if (blueprint == null) {
            return null;
        }

        String id = buildInstanceId(blueprint.id());
        String dimensionId = player.level().dimension().identifier().toString();
        BlockPos worldPos = blueprint.origin() == null ? bindPos.above() : blueprint.origin();
        BlockPos areaMin = worldPos;
        BlockPos areaMax = worldPos.offset(
                Math.max(0, blueprint.size().getX() - 1),
                Math.max(0, blueprint.size().getY() - 1),
                Math.max(0, blueprint.size().getZ() - 1)
        );
        TriggerBlueprint.Settings settings = blueprint.settings();
        int zoneRadius = Math.max(
                settings.zoneRadius(),
                Math.max(3, Math.max(blueprint.size().getX(), blueprint.size().getZ()) / 2 + 1)
        );
        TriggerInstance instance = new TriggerInstance(
                id,
                blueprint.id(),
                worldPos,
                bindPos,
                areaMin,
                areaMax,
                dimensionId,
                pending.triggerType(),
                TriggerInstance.StructureState.HIDDEN,
                settings.cooldownTicks(),
                0,
                settings.timerIntervalTicks(),
                0L,
                zoneRadius,
                new net.minecraft.nbt.CompoundTag()
        );
        register(instance);
        return instance;
    }

    public void register(TriggerInstance instance) {
        if (instance == null) {
            return;
        }
        String id = normalizeId(instance.id());
        instances.put(id, instance);
        indexBind(instance);
        persistInstance(instance);
        writeActiveFile(instance);
    }

    public TriggerInstance remove(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return null;
        }
        String id = normalizeId(instanceId);
        TriggerInstance instance = instances.remove(id);
        if (instance == null) {
            return null;
        }
        unindexBind(instance);
        if (savedData != null) {
            savedData.removeInstance(id);
        }
        deleteActiveFile(id);
        return instance;
    }

    public void persistInstance(TriggerInstance instance) {
        if (instance == null || savedData == null) {
            return;
        }
        savedData.putInstance(normalizeId(instance.id()), instance.toSavedData());
    }

    public void refreshActiveFile(TriggerInstance instance) {
        if (instance == null) {
            return;
        }
        writeActiveFile(instance);
    }

    public List<TriggerInstance> getByBind(String dimensionId, BlockPos bindPos) {
        if (bindPos == null || dimensionId == null || dimensionId.isBlank()) {
            return List.of();
        }
        Set<String> ids = instancesByBind.get(BindKey.of(dimensionId, bindPos));
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<TriggerInstance> result = new ArrayList<>();
        for (String id : ids) {
            TriggerInstance instance = instances.get(id);
            if (instance != null) {
                result.add(instance);
            }
        }
        result.sort(Comparator.comparing(TriggerInstance::id));
        return result;
    }

    public List<TriggerInstance> getByDimension(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) {
            return List.of();
        }
        List<TriggerInstance> result = new ArrayList<>();
        for (TriggerInstance instance : instances.values()) {
            if (dimensionId.equals(instance.dimensionId())) {
                result.add(instance);
            }
        }
        return result;
    }

    private void indexBind(TriggerInstance instance) {
        BindKey key = BindKey.of(instance.dimensionId(), instance.bindPos());
        instancesByBind.computeIfAbsent(key, unused -> ConcurrentHashMap.newKeySet()).add(normalizeId(instance.id()));
    }

    private void unindexBind(TriggerInstance instance) {
        BindKey key = BindKey.of(instance.dimensionId(), instance.bindPos());
        Set<String> ids = instancesByBind.get(key);
        if (ids == null) {
            return;
        }
        ids.remove(normalizeId(instance.id()));
        if (ids.isEmpty()) {
            instancesByBind.remove(key);
        }
    }

    private void writeActiveFile(TriggerInstance instance) {
        Path activeDir = blueprintLoader.getActiveDirectory();
        try {
            Files.createDirectories(activeDir);
            Path file = activeDir.resolve(normalizeId(instance.id()) + ".json");
            JsonObject root = new JsonObject();
            root.addProperty("id", instance.id());
            root.addProperty("blueprint_id", instance.blueprintId());
            root.add("world_pos", posToJson(instance.worldPos()));
            root.add("bind_pos", posToJson(instance.bindPos()));
            root.add("area_min", posToJson(instance.areaMin()));
            root.add("area_max", posToJson(instance.areaMax()));
            root.addProperty("dimension", instance.dimensionId());
            root.addProperty("trigger_type", instance.triggerType());
            root.addProperty("state", instance.structureState().name());
            root.addProperty("cooldown_ticks", instance.cooldownTicks());
            root.addProperty("cooldown_remaining", instance.cooldownRemaining());
            root.addProperty("timer_interval_ticks", instance.timerIntervalTicks());
            root.addProperty("zone_radius", instance.zoneRadius());
            root.addProperty("persistent_data_snbt", instance.persistentData().toString());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("TriggerInstanceManager: failed to write active file for {}", instance.id(), e);
        }
    }

    private void deleteActiveFile(String instanceId) {
        Path file = blueprintLoader.getActiveDirectory().resolve(normalizeId(instanceId) + ".json");
        try {
            Files.deleteIfExists(file);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("TriggerInstanceManager: failed to delete active file {}", file, e);
        }
    }

    private JsonArray posToJson(BlockPos pos) {
        JsonArray arr = new JsonArray();
        arr.add(pos.getX());
        arr.add(pos.getY());
        arr.add(pos.getZ());
        return arr;
    }

    private String buildInstanceId(String blueprintId) {
        String clean = blueprintId == null ? "trigger" : blueprintId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        return clean + "_" + Long.toString(System.currentTimeMillis(), 36);
    }

    private String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
