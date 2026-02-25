package neutka.marallys.marallyzen.trigger.instance;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import neutka.marallys.marallyzen.trigger.storage.TriggerSavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TriggerInstance {
    public enum StructureState {
        HIDDEN,
        ANIMATING,
        ACTIVE
    }

    private final String id;
    private final String blueprintId;
    private final BlockPos worldPos;
    private final BlockPos bindPos;
    private final BlockPos areaMin;
    private final BlockPos areaMax;
    private final String dimensionId;
    private String triggerType;
    private StructureState structureState;
    private int cooldownTicks;
    private int cooldownRemaining;
    private long timerIntervalTicks;
    private long nextTimerGameTime;
    private int zoneRadius;
    private final CompoundTag persistentData;

    private int animationTick;
    private boolean lastPowered;
    private final Set<UUID> playersInside = new HashSet<>();

    public TriggerInstance(
            String id,
            String blueprintId,
            BlockPos worldPos,
            BlockPos bindPos,
            BlockPos areaMin,
            BlockPos areaMax,
            String dimensionId,
            String triggerType,
            StructureState structureState,
            int cooldownTicks,
            int cooldownRemaining,
            long timerIntervalTicks,
            long nextTimerGameTime,
            int zoneRadius,
            CompoundTag persistentData
    ) {
        this.id = id;
        this.blueprintId = blueprintId;
        this.worldPos = worldPos == null ? BlockPos.ZERO : worldPos.immutable();
        this.bindPos = bindPos == null ? this.worldPos : bindPos.immutable();
        BlockPos min = areaMin == null ? this.worldPos : areaMin.immutable();
        BlockPos max = areaMax == null ? this.worldPos : areaMax.immutable();
        this.areaMin = new BlockPos(
                Math.min(min.getX(), max.getX()),
                Math.min(min.getY(), max.getY()),
                Math.min(min.getZ(), max.getZ())
        );
        this.areaMax = new BlockPos(
                Math.max(min.getX(), max.getX()),
                Math.max(min.getY(), max.getY()),
                Math.max(min.getZ(), max.getZ())
        );
        this.dimensionId = dimensionId == null ? "minecraft:overworld" : dimensionId;
        this.triggerType = triggerType == null ? "block_use" : triggerType;
        this.structureState = structureState == null ? StructureState.HIDDEN : structureState;
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.cooldownRemaining = Math.max(0, cooldownRemaining);
        this.timerIntervalTicks = Math.max(1L, timerIntervalTicks);
        this.nextTimerGameTime = Math.max(0L, nextTimerGameTime);
        this.zoneRadius = Math.max(1, zoneRadius);
        this.persistentData = persistentData == null ? new CompoundTag() : persistentData.copy();
    }

    public String id() {
        return id;
    }

    public String blueprintId() {
        return blueprintId;
    }

    public BlockPos worldPos() {
        return worldPos;
    }

    public BlockPos bindPos() {
        return bindPos;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public BlockPos areaMin() {
        return areaMin;
    }

    public BlockPos areaMax() {
        return areaMax;
    }

    public String triggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType == null || triggerType.isBlank() ? "block_use" : triggerType;
    }

    public StructureState structureState() {
        return structureState;
    }

    public void setStructureState(StructureState structureState) {
        this.structureState = structureState == null ? StructureState.HIDDEN : structureState;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
    }

    public int cooldownRemaining() {
        return cooldownRemaining;
    }

    public void setCooldownRemaining(int cooldownRemaining) {
        this.cooldownRemaining = Math.max(0, cooldownRemaining);
    }

    public long timerIntervalTicks() {
        return timerIntervalTicks;
    }

    public void setTimerIntervalTicks(long timerIntervalTicks) {
        this.timerIntervalTicks = Math.max(1L, timerIntervalTicks);
    }

    public long nextTimerGameTime() {
        return nextTimerGameTime;
    }

    public void setNextTimerGameTime(long nextTimerGameTime) {
        this.nextTimerGameTime = Math.max(0L, nextTimerGameTime);
    }

    public int zoneRadius() {
        return zoneRadius;
    }

    public void setZoneRadius(int zoneRadius) {
        this.zoneRadius = Math.max(1, zoneRadius);
    }

    public CompoundTag persistentData() {
        return persistentData;
    }

    public int animationTick() {
        return animationTick;
    }

    public void setAnimationTick(int animationTick) {
        this.animationTick = Math.max(0, animationTick);
    }

    public boolean lastPowered() {
        return lastPowered;
    }

    public void setLastPowered(boolean lastPowered) {
        this.lastPowered = lastPowered;
    }

    public Set<UUID> playersInside() {
        return playersInside;
    }

    public BlockPos centerPos() {
        return new BlockPos(
                (areaMin.getX() + areaMax.getX()) / 2,
                (areaMin.getY() + areaMax.getY()) / 2,
                (areaMin.getZ() + areaMax.getZ()) / 2
        );
    }

    public TriggerSavedData.TriggerInstanceData toSavedData() {
        return new TriggerSavedData.TriggerInstanceData(
                blueprintId,
                worldPos,
                bindPos,
                areaMin,
                areaMax,
                dimensionId,
                triggerType,
                structureState.name(),
                switch (structureState) {
                    case ANIMATING -> "RUNNING";
                    case HIDDEN -> "DISABLED";
                    case ACTIVE -> "IDLE";
                },
                cooldownTicks,
                cooldownRemaining,
                timerIntervalTicks,
                nextTimerGameTime,
                zoneRadius,
                persistentData.copy()
        );
    }

    public static TriggerInstance fromSavedData(String id, TriggerSavedData.TriggerInstanceData data) {
        return fromSavedData(id, data, data.worldPos(), data.worldPos());
    }

    public static TriggerInstance fromSavedData(
            String id,
            TriggerSavedData.TriggerInstanceData data,
            BlockPos fallbackAreaMin,
            BlockPos fallbackAreaMax
    ) {
        if (data == null) {
            return null;
        }
        StructureState state;
        try {
            state = StructureState.valueOf(data.structureState());
        } catch (Exception ignored) {
            state = mapLegacyActivationState(data.activationState());
        }
        BlockPos areaMin = data.areaMin();
        BlockPos areaMax = data.areaMax();
        if (isUnsetBounds(areaMin, areaMax)) {
            areaMin = fallbackAreaMin == null ? data.worldPos() : fallbackAreaMin;
            areaMax = fallbackAreaMax == null ? data.worldPos() : fallbackAreaMax;
        }
        return new TriggerInstance(
                id,
                data.blueprintId(),
                data.worldPos(),
                data.bindPos(),
                areaMin,
                areaMax,
                data.dimensionId(),
                data.triggerType(),
                state,
                data.cooldownTicks(),
                data.cooldownRemaining(),
                data.timerIntervalTicks(),
                data.nextTimerGameTime(),
                data.zoneRadius(),
                data.persistentData()
        );
    }

    private static boolean isUnsetBounds(BlockPos areaMin, BlockPos areaMax) {
        return areaMin == null || areaMax == null || (areaMin.equals(BlockPos.ZERO) && areaMax.equals(BlockPos.ZERO));
    }

    private static StructureState mapLegacyActivationState(String raw) {
        if (raw == null) {
            return StructureState.ACTIVE;
        }
        return switch (raw) {
            case "RUNNING" -> StructureState.ANIMATING;
            case "DISABLED" -> StructureState.HIDDEN;
            default -> StructureState.ACTIVE;
        };
    }
}
