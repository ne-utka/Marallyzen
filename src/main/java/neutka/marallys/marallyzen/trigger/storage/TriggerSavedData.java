package neutka.marallys.marallyzen.trigger.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TriggerSavedData extends SavedData {
    public static final String DATA_NAME = "marallyzen_triggers";
    private static final String KEY_INSTANCES = "instances";

    public static final Codec<TriggerSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, TriggerInstanceData.CODEC)
                    .fieldOf(KEY_INSTANCES)
                    .forGetter(data -> data.instances)
    ).apply(instance, map -> {
        TriggerSavedData data = new TriggerSavedData();
        data.instances.putAll(map);
        return data;
    }));

    public static final SavedDataType<TriggerSavedData> TYPE = new SavedDataType<>(DATA_NAME, TriggerSavedData::new, CODEC);

    private final Map<String, TriggerInstanceData> instances = new HashMap<>();

    public static TriggerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<String, TriggerInstanceData> instances() {
        return Collections.unmodifiableMap(instances);
    }

    public TriggerInstanceData getInstance(String id) {
        return instances.get(id);
    }

    public void putInstance(String id, TriggerInstanceData data) {
        if (id == null || id.isBlank() || data == null) {
            return;
        }
        instances.put(id, data);
        setDirty();
    }

    public void removeInstance(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        instances.remove(id);
        setDirty();
    }

    public Set<String> ids() {
        return new HashSet<>(instances.keySet());
    }

    public record TriggerInstanceData(
            String blueprintId,
            BlockPos worldPos,
            BlockPos bindPos,
            BlockPos areaMin,
            BlockPos areaMax,
            String dimensionId,
            String triggerType,
            String structureState,
            String activationState,
            int cooldownTicks,
            int cooldownRemaining,
            long timerIntervalTicks,
            long nextTimerGameTime,
            int zoneRadius,
            CompoundTag persistentData
    ) {
        public static final Codec<TriggerInstanceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("blueprintId").forGetter(TriggerInstanceData::blueprintId),
                BlockPos.CODEC.fieldOf("worldPos").forGetter(TriggerInstanceData::worldPos),
                BlockPos.CODEC.optionalFieldOf("bindPos", BlockPos.ZERO).forGetter(TriggerInstanceData::bindPos),
                BlockPos.CODEC.optionalFieldOf("areaMin", BlockPos.ZERO).forGetter(TriggerInstanceData::areaMin),
                BlockPos.CODEC.optionalFieldOf("areaMax", BlockPos.ZERO).forGetter(TriggerInstanceData::areaMax),
                Codec.STRING.optionalFieldOf("dimension", "minecraft:overworld").forGetter(TriggerInstanceData::dimensionId),
                Codec.STRING.optionalFieldOf("triggerType", "block_use").forGetter(TriggerInstanceData::triggerType),
                Codec.STRING.optionalFieldOf("state", "ACTIVE").forGetter(TriggerInstanceData::structureState),
                Codec.STRING.optionalFieldOf("activationState", "IDLE").forGetter(TriggerInstanceData::activationState),
                Codec.INT.optionalFieldOf("cooldownTicks", 20).forGetter(TriggerInstanceData::cooldownTicks),
                Codec.INT.optionalFieldOf("cooldownRemaining", 0).forGetter(TriggerInstanceData::cooldownRemaining),
                Codec.LONG.optionalFieldOf("timerIntervalTicks", 200L).forGetter(TriggerInstanceData::timerIntervalTicks),
                Codec.LONG.optionalFieldOf("nextTimerGameTime", 0L).forGetter(TriggerInstanceData::nextTimerGameTime),
                Codec.INT.optionalFieldOf("zoneRadius", 6).forGetter(TriggerInstanceData::zoneRadius),
                CompoundTag.CODEC.optionalFieldOf("persistentData", new CompoundTag()).forGetter(TriggerInstanceData::persistentData)
        ).apply(instance, TriggerInstanceData::new));
    }
}
