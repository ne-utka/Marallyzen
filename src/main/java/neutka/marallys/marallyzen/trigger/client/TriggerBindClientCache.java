package neutka.marallys.marallyzen.trigger.client;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of trigger-bound block positions by dimension.
 */
public final class TriggerBindClientCache {
    public record BindEntry(String dimensionId, BlockPos pos) {
    }

    private static final Map<String, Set<Long>> BINDINGS = new ConcurrentHashMap<>();

    private TriggerBindClientCache() {
    }

    public static void replaceAll(Collection<BindEntry> entries) {
        BINDINGS.clear();
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (BindEntry entry : entries) {
            if (entry == null || entry.pos() == null || entry.dimensionId() == null || entry.dimensionId().isBlank()) {
                continue;
            }
            String key = normalizeDimension(entry.dimensionId());
            BINDINGS.computeIfAbsent(key, unused -> ConcurrentHashMap.newKeySet()).add(entry.pos().asLong());
        }
    }

    public static boolean isBound(String dimensionId, BlockPos pos) {
        if (dimensionId == null || dimensionId.isBlank() || pos == null) {
            return false;
        }
        Set<Long> set = BINDINGS.get(normalizeDimension(dimensionId));
        return set != null && set.contains(pos.asLong());
    }

    public static void clear() {
        BINDINGS.clear();
    }

    private static String normalizeDimension(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
