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
    public static final String KEY_CHAIN_ID = "chain_id";
    public static final String KEY_CHAIN_STEP = "chain_step";
    public static final String KEY_CHAIN_NEXT_INSTANCE_ID = "chain_next_instance_id";
    public static final String KEY_CHAIN_PREV_INSTANCE_ID = "chain_prev_instance_id";
    public static final String KEY_CHAIN_LOCKED = "chain_locked";

    public record PendingBind(String blueprintId, String triggerType, String previousInstanceId) {
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
        setPendingBind(playerId, blueprintId, triggerType, "");
    }

    public void setPendingBind(UUID playerId, String blueprintId, String triggerType, String previousInstanceId) {
        if (playerId == null || blueprintId == null || blueprintId.isBlank()) {
            return;
        }
        String normalizedType = triggerType == null || triggerType.isBlank() ? "block_use" : triggerType.trim().toLowerCase(Locale.ROOT);
        String normalizedPrev = previousInstanceId == null ? "" : normalizeId(previousInstanceId);
        pendingBinds.put(playerId, new PendingBind(blueprintId.trim().toLowerCase(Locale.ROOT), normalizedType, normalizedPrev));
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

        String previousInstanceId = pending.previousInstanceId() == null ? "" : normalizeId(pending.previousInstanceId());
        TriggerInstance previousInChain = null;
        if (!previousInstanceId.isBlank()) {
            previousInChain = get(previousInstanceId);
            if (previousInChain == null) {
                return null;
            }
            String existingNextId = getChainNextInstanceId(previousInChain);
            if (!existingNextId.isBlank() && get(existingNextId) != null) {
                return null;
            }
            String chainId = getChainId(previousInChain);
            if (chainId.isBlank()) {
                chainId = previousInChain.id();
            }
            int step = Math.max(1, getChainStep(previousInChain));
            applyChainMetadata(
                    instance,
                    chainId,
                    step + 1,
                    previousInChain.id(),
                    "",
                    true
            );
        }

        register(instance);
        if (previousInChain != null) {
            previousInChain.persistentData().putString(KEY_CHAIN_NEXT_INSTANCE_ID, instance.id());
            persistInstance(previousInChain);
            refreshActiveFile(previousInChain);
        }
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

    public String getChainId(TriggerInstance instance) {
        if (instance == null) {
            return "";
        }
        return normalizeChainId(instance.persistentData().getString(KEY_CHAIN_ID).orElse(""));
    }

    public int getChainStep(TriggerInstance instance) {
        if (instance == null) {
            return 0;
        }
        return Math.max(0, instance.persistentData().getInt(KEY_CHAIN_STEP).orElse(0));
    }

    public String getChainNextInstanceId(TriggerInstance instance) {
        if (instance == null) {
            return "";
        }
        String nextId = instance.persistentData().getString(KEY_CHAIN_NEXT_INSTANCE_ID).orElse("");
        return nextId == null ? "" : nextId.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isChainLocked(TriggerInstance instance) {
        if (instance == null) {
            return false;
        }
        return instance.persistentData().getBoolean(KEY_CHAIN_LOCKED).orElse(false);
    }

    public void setChainLocked(TriggerInstance instance, boolean locked) {
        if (instance == null) {
            return;
        }
        instance.persistentData().putBoolean(KEY_CHAIN_LOCKED, locked);
    }

    public void linkSequence(List<String> sequenceIds) {
        if (sequenceIds == null || sequenceIds.size() < 2) {
            return;
        }
        List<TriggerInstance> sequence = new ArrayList<>();
        for (String rawId : sequenceIds) {
            if (rawId == null || rawId.isBlank()) {
                continue;
            }
            TriggerInstance instance = get(rawId);
            if (instance != null) {
                sequence.add(instance);
            }
        }
        if (sequence.size() < 2) {
            return;
        }
        String chainId = normalizeId(sequence.get(0).id());
        for (int i = 0; i < sequence.size(); i++) {
            TriggerInstance current = sequence.get(i);
            String prevId = i > 0 ? sequence.get(i - 1).id() : "";
            String nextId = (i + 1) < sequence.size() ? sequence.get(i + 1).id() : "";
            boolean locked = i > 0;
            applyChainMetadata(current, chainId, i + 1, prevId, nextId, locked);
            persistInstance(current);
            refreshActiveFile(current);
        }
    }

    public Set<String> chainIds() {
        Set<String> ids = new TreeSet<>();
        for (TriggerInstance instance : instances.values()) {
            String chainId = getChainId(instance);
            if (!chainId.isBlank()) {
                ids.add(chainId);
            }
        }
        return ids;
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
            String chainId = getChainId(instance);
            if (!chainId.isBlank()) {
                root.addProperty("chain_id", chainId);
                root.addProperty("chain_step", getChainStep(instance));
                String nextId = getChainNextInstanceId(instance);
                if (!nextId.isBlank()) {
                    root.addProperty("chain_next_instance_id", nextId);
                }
                String prevId = instance.persistentData().getString(KEY_CHAIN_PREV_INSTANCE_ID).orElse("");
                if (prevId != null && !prevId.isBlank()) {
                    root.addProperty("chain_prev_instance_id", prevId);
                }
                root.addProperty("chain_locked", isChainLocked(instance));
            }
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

    private void applyChainMetadata(
            TriggerInstance instance,
            String chainId,
            int step,
            String prevInstanceId,
            String nextInstanceId,
            boolean locked
    ) {
        if (instance == null) {
            return;
        }
        var data = instance.persistentData();
        String normalizedChain = normalizeChainId(chainId);
        if (normalizedChain.isBlank()) {
            data.remove(KEY_CHAIN_ID);
            data.remove(KEY_CHAIN_STEP);
            data.remove(KEY_CHAIN_NEXT_INSTANCE_ID);
            data.remove(KEY_CHAIN_PREV_INSTANCE_ID);
            data.remove(KEY_CHAIN_LOCKED);
            return;
        }
        data.putString(KEY_CHAIN_ID, normalizedChain);
        data.putInt(KEY_CHAIN_STEP, Math.max(1, step));
        if (prevInstanceId != null && !prevInstanceId.isBlank()) {
            data.putString(KEY_CHAIN_PREV_INSTANCE_ID, normalizeId(prevInstanceId));
        } else {
            data.remove(KEY_CHAIN_PREV_INSTANCE_ID);
        }
        if (nextInstanceId != null && !nextInstanceId.isBlank()) {
            data.putString(KEY_CHAIN_NEXT_INSTANCE_ID, normalizeId(nextInstanceId));
        } else {
            data.remove(KEY_CHAIN_NEXT_INSTANCE_ID);
        }
        data.putBoolean(KEY_CHAIN_LOCKED, locked);
    }

    private String normalizeChainId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
