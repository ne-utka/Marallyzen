package neutka.marallys.marallyzen.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side cache of flashlight states for all players.
 * Updated from network packets.
 */
public class FlashlightStateCache {
    
    private static final Map<UUID, FlashlightState> STATES = new HashMap<>();
    
    /**
     * Record to store flashlight state
     */
    public record FlashlightState(
        boolean enabled,
        float yaw,
        float pitch
    ) {}
    
    /**
     * Updates the state for a player
     */
    public static void updateState(UUID playerId, boolean enabled, float yaw, float pitch) {
        if (enabled) {
            STATES.put(playerId, new FlashlightState(true, yaw, pitch));
        } else {
            STATES.remove(playerId);
        }
    }
    
    /**
     * Gets the state for a player
     */
    public static FlashlightState getState(UUID playerId) {
        return STATES.get(playerId);
    }
    
    /**
     * Checks if a player has an active flashlight
     */
    public static boolean isActive(UUID playerId) {
        FlashlightState state = STATES.get(playerId);
        return state != null && state.enabled;
    }
    
    /**
     * Gets all active flashlight states
     */
    public static Map<UUID, FlashlightState> getAllStates() {
        return new HashMap<>(STATES);
    }
    
    /**
     * Clears all states (called on disconnect)
     */
    public static void clear() {
        STATES.clear();
    }
}





















