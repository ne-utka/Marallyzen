package neutka.marallys.marallyzen.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of trigger type handlers.
 */
public final class TriggerRegistry {
    private final Map<String, TriggerHandler> handlers = new ConcurrentHashMap<>();

    public TriggerRegistry() {
        registerDefaults();
    }

    public void register(String id, TriggerHandler handler) {
        if (id == null || id.isBlank() || handler == null) {
            return;
        }
        handlers.put(id.trim().toLowerCase(Locale.ROOT), handler);
    }

    public TriggerHandler get(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return handlers.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public boolean shouldActivate(String id, ServerPlayer player, BlockPos pos, Level level) {
        TriggerHandler handler = get(id);
        if (handler == null) {
            return false;
        }
        return handler.shouldActivate(player, pos, level);
    }

    private void registerDefaults() {
        register("block_use", (player, pos, level) -> player != null && pos != null && level != null);
        register("redstone_power", (player, pos, level) -> pos != null && level != null && level.hasNeighborSignal(pos));
        register("pressure_plate", (player, pos, level) -> pos != null && level != null && player != null);
        register("zone_enter", (player, pos, level) -> pos != null && level != null && player != null);
        register("manual", (player, pos, level) -> level != null && pos != null);
        register("timer", (player, pos, level) -> level != null && pos != null);
    }
}
