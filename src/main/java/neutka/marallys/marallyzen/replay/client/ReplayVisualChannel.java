package neutka.marallys.marallyzen.replay.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public interface ReplayVisualChannel {
    String getId();

    void capture(Entity entity, ReplayClientCaptureContext context, CompoundTag out);

    default void apply(Entity entity, CompoundTag data) {
    }
}
