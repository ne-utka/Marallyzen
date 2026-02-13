package neutka.marallys.marallyzen.goals;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Global persistent goal state.
 */
public class GoalSavedData extends SavedData {
    public static final String DATA_NAME = "marallyzen_goals";
    private static final String KEY_GOALS = "goals";
    private static final Codec<Set<Long>> LONG_SET_CODEC = Codec.LONG.listOf().xmap(HashSet::new, ArrayList::new);

    public static final Codec<GoalSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, GoalProgressData.CODEC)
                    .fieldOf(KEY_GOALS)
                    .forGetter(data -> data.goals)
    ).apply(instance, map -> {
        GoalSavedData data = new GoalSavedData();
        data.goals.putAll(map);
        return data;
    }));

    public static final SavedDataType<GoalSavedData> TYPE = new SavedDataType<>(DATA_NAME, GoalSavedData::new, CODEC);

    private final Map<String, GoalProgressData> goals = new HashMap<>();

    public static GoalSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<String, GoalProgressData> goals() {
        return Collections.unmodifiableMap(goals);
    }

    public GoalProgressData getGoal(String goalId) {
        return goals.get(goalId);
    }

    public boolean hasGoal(String goalId) {
        return goals.containsKey(goalId);
    }

    public void putGoal(String goalId, GoalProgressData data) {
        if (goalId == null || goalId.isBlank() || data == null) {
            return;
        }
        goals.put(goalId, data);
        setDirty();
    }

    public void removeGoal(String goalId) {
        if (goalId == null || goalId.isBlank()) {
            return;
        }
        goals.remove(goalId);
        setDirty();
    }

    public Set<String> ids() {
        return new HashSet<>(goals.keySet());
    }

    public static class GoalProgressData {
        public static final Codec<GoalProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("progress").forGetter(GoalProgressData::progress),
                Codec.LONG.fieldOf("createdAt").forGetter(GoalProgressData::createdAt),
                BlockPos.CODEC.fieldOf("spawnPos").forGetter(GoalProgressData::spawnPos),
                Codec.STRING.fieldOf("dimension").forGetter(GoalProgressData::dimensionId),
                LONG_SET_CODEC.optionalFieldOf("triggeredMilestones", Set.of()).forGetter(GoalProgressData::triggeredMilestones),
                CompoundTag.CODEC.optionalFieldOf("customData", new CompoundTag()).forGetter(GoalProgressData::customData),
                Codec.BOOL.optionalFieldOf("completed", false).forGetter(GoalProgressData::completed),
                Codec.INT.optionalFieldOf("zoneRadius", 3).forGetter(GoalProgressData::zoneRadius),
                Codec.BOOL.optionalFieldOf("protect", true).forGetter(GoalProgressData::protect),
                Codec.STRING.optionalFieldOf("entityUuid", "").forGetter(GoalProgressData::entityUuid)
        ).apply(instance, GoalProgressData::new));

        private long progress;
        private long createdAt;
        private BlockPos spawnPos;
        private String dimensionId;
        private Set<Long> triggeredMilestones;
        private CompoundTag customData;
        private boolean completed;
        private int zoneRadius;
        private boolean protect;
        private String entityUuid;

        public GoalProgressData(
                long progress,
                long createdAt,
                BlockPos spawnPos,
                String dimensionId,
                Set<Long> triggeredMilestones,
                CompoundTag customData,
                boolean completed,
                int zoneRadius,
                boolean protect,
                String entityUuid
        ) {
            this.progress = Math.max(0L, progress);
            this.createdAt = createdAt;
            this.spawnPos = spawnPos == null ? BlockPos.ZERO : spawnPos.immutable();
            this.dimensionId = dimensionId == null ? "minecraft:overworld" : dimensionId;
            this.triggeredMilestones = triggeredMilestones == null ? new HashSet<>() : new HashSet<>(triggeredMilestones);
            this.customData = customData == null ? new CompoundTag() : customData.copy();
            this.completed = completed;
            this.zoneRadius = Math.max(1, zoneRadius);
            this.protect = protect;
            this.entityUuid = entityUuid == null ? "" : entityUuid;
        }

        public static GoalProgressData create(BlockPos spawnPos, String dimensionId, int zoneRadius, boolean protect) {
            return new GoalProgressData(
                    0L,
                    System.currentTimeMillis(),
                    spawnPos,
                    dimensionId,
                    Set.of(),
                    new CompoundTag(),
                    false,
                    zoneRadius,
                    protect,
                    ""
            );
        }

        public long progress() {
            return progress;
        }

        public void setProgress(long progress) {
            this.progress = Math.max(0L, progress);
        }

        public long createdAt() {
            return createdAt;
        }

        public BlockPos spawnPos() {
            return spawnPos;
        }

        public String dimensionId() {
            return dimensionId;
        }

        public Set<Long> triggeredMilestones() {
            return triggeredMilestones;
        }

        public CompoundTag customData() {
            return customData;
        }

        public boolean completed() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public int zoneRadius() {
            return zoneRadius;
        }

        public boolean protect() {
            return protect;
        }

        public String entityUuid() {
            return entityUuid;
        }

        public void setEntityUuid(String entityUuid) {
            this.entityUuid = entityUuid == null ? "" : entityUuid;
        }
    }
}
