package neutka.marallys.marallyzen.replay;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public final class ReplayClientFrame {
    private final long tick;
    private final ReplayCameraFrame camera;
    private final Map<UUID, CompoundTag> entityVisuals;
    private final CompoundTag worldVisuals;

    public ReplayClientFrame(long tick, ReplayCameraFrame camera,
                             Map<UUID, CompoundTag> entityVisuals,
                             CompoundTag worldVisuals) {
        this.tick = tick;
        this.camera = camera;
        this.entityVisuals = entityVisuals == null ? new HashMap<>() : new HashMap<>(entityVisuals);
        this.worldVisuals = worldVisuals == null ? new CompoundTag() : worldVisuals.copy();
    }

    public long getTick() {
        return tick;
    }

    public ReplayCameraFrame getCamera() {
        return camera;
    }

    public Map<UUID, CompoundTag> getEntityVisuals() {
        return Collections.unmodifiableMap(entityVisuals);
    }

    public CompoundTag getWorldVisuals() {
        return worldVisuals.copy();
    }
}
