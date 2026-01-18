package neutka.marallys.marallyzen.replay.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class ReplayEmoteVisualChannel implements ReplayVisualChannel {
    public static final String CHANNEL_ID = "emote";
    private static final String KEY_ID = "id";
    private static final String KEY_STOP = "stop";
    private static final Map<UUID, String> LAST_CAPTURED = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return CHANNEL_ID;
    }

    @Override
    public void capture(Entity entity, ReplayClientCaptureContext context, CompoundTag out) {
        if (entity == null || out == null) {
            return;
        }
        UUID entityId = entity.getUUID();
        String current = ReplayEmoteStateTracker.getActive(entityId);
        String last = LAST_CAPTURED.get(entityId);
        if (current == null) {
            if (last != null) {
                out.putBoolean(KEY_STOP, true);
                LAST_CAPTURED.remove(entityId);
            }
            return;
        }
        if (!current.equals(last)) {
            out.putString(KEY_ID, current);
            LAST_CAPTURED.put(entityId, current);
        }
    }

    public static void reset() {
        LAST_CAPTURED.clear();
    }
}
