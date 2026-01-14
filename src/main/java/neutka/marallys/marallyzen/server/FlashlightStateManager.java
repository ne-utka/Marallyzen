package neutka.marallys.marallyzen.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.FlashlightStatePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side manager that tracks flashlight states for all players
 * and broadcasts updates to tracking clients.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class FlashlightStateManager {
    
    private static final Map<UUID, FlashlightState> FLASHLIGHT_STATES = new HashMap<>();
    
    /**
     * Record to store flashlight state for a player
     */
    public record FlashlightState(
        UUID playerId,
        boolean enabled,
        float yaw,
        float pitch
    ) {}
    
    /**
     * Updates the flashlight state for a player and broadcasts to all tracking players
     */
    public static void updateFlashlightState(ServerPlayer player, boolean enabled) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        
        FlashlightState state = new FlashlightState(player.getUUID(), enabled, yaw, pitch);
        FLASHLIGHT_STATES.put(player.getUUID(), state);
        
        // Broadcast to all players tracking this player
        FlashlightStatePacket packet = new FlashlightStatePacket(
            player.getUUID(),
            enabled,
            yaw,
            pitch
        );
        
        // Send to all players in the same dimension
        NetworkHelper.sendToAll(packet);
    }
    
    /**
     * Updates player rotation for an active flashlight
     */
    public static void updatePlayerRotation(ServerPlayer player) {
        FlashlightState state = FLASHLIGHT_STATES.get(player.getUUID());
        if (state != null && state.enabled) {
            updateFlashlightState(player, true);
        }
    }
    
    /**
     * Removes flashlight state when player disconnects
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FLASHLIGHT_STATES.remove(player.getUUID());
            
            // Notify all clients that this flashlight is disabled
            FlashlightStatePacket packet = new FlashlightStatePacket(
                player.getUUID(),
                false,
                0.0f,
                0.0f
            );
            NetworkHelper.sendToAll(packet);
        }
    }
    
    // Note: Rotation updates happen automatically when flashlight is toggled
    // This is more efficient than updating on every server tick
    // If smooth rotation updates are needed, they can be added later with proper server access
    
    /**
     * Gets the current flashlight state for a player
     */
    public static FlashlightState getState(UUID playerId) {
        return FLASHLIGHT_STATES.get(playerId);
    }
    
    /**
     * Checks if a player has an active flashlight
     */
    public static boolean isFlashlightActive(UUID playerId) {
        FlashlightState state = FLASHLIGHT_STATES.get(playerId);
        return state != null && state.enabled;
    }
}

