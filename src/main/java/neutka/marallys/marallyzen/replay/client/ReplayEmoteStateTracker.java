package neutka.marallys.marallyzen.replay.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReplayEmoteStateTracker {
    private static final Map<UUID, String> ACTIVE = new ConcurrentHashMap<>();

    private ReplayEmoteStateTracker() {
    }

    public static void setActive(UUID entityId, String emoteId) {
        if (entityId == null || emoteId == null || emoteId.isBlank()) {
            return;
        }
        ACTIVE.put(entityId, emoteId);
    }

    public static void clear(UUID entityId) {
        if (entityId == null) {
            return;
        }
        ACTIVE.remove(entityId);
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    public static String getActive(UUID entityId) {
        if (entityId == null) {
            return null;
        }
        return ACTIVE.get(entityId);
    }
}
