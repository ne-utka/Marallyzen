package neutka.marallys.marallyzen.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles default animation playback for NPCs.
 * Tracks when NPCs should return to their default animation after other animations finish.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class NpcDefaultAnimationHandler {
    
    // Track NPCs that should play default animation
    // Map: NPC UUID -> (last animation time, default animation name)
    private static final Map<UUID, DefaultAnimationState> npcDefaultAnimationStates = new HashMap<>();
    
    // Cooldown before restarting default animation (in ticks)
    // This prevents spamming animations when they finish
    private static final int DEFAULT_ANIMATION_COOLDOWN_TICKS = 20; // 1 second
    
    private static class DefaultAnimationState {
        final String defaultAnimation;
        int cooldownTicks = 0;
        boolean isPlayingOtherAnimation = false;
        
        DefaultAnimationState(String defaultAnimation) {
            this.defaultAnimation = defaultAnimation;
        }
    }
    
    /**
     * Registers an NPC to play a default animation.
     * The animation will be played when the NPC spawns and when other animations finish.
     */
    public static void registerNpcDefaultAnimation(Entity npc, String defaultAnimation) {
        if (npc == null || defaultAnimation == null || defaultAnimation.isEmpty()) {
            return;
        }
        npcDefaultAnimationStates.put(npc.getUUID(), new DefaultAnimationState(defaultAnimation));
    }
    
    /**
     * Unregisters an NPC's default animation.
     */
    public static void unregisterNpcDefaultAnimation(Entity npc) {
        if (npc == null) {
            return;
        }
        npcDefaultAnimationStates.remove(npc.getUUID());
    }
    
    /**
     * Marks that an NPC is playing a non-default animation.
     * This prevents the default animation from playing until the other animation finishes.
     */
    public static void markNpcPlayingOtherAnimation(Entity npc) {
        if (npc == null) {
            return;
        }
        DefaultAnimationState state = npcDefaultAnimationStates.get(npc.getUUID());
        if (state != null) {
            state.isPlayingOtherAnimation = true;
            state.cooldownTicks = DEFAULT_ANIMATION_COOLDOWN_TICKS;
        }
    }
    
    /**
     * Server tick event handler.
     * Periodically checks if NPCs need to return to their default animation.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() == null) {
            return;
        }
        
        // Process each registered NPC
        npcDefaultAnimationStates.entrySet().removeIf(entry -> {
            UUID npcUuid = entry.getKey();
            DefaultAnimationState state = entry.getValue();
            
            // Find the NPC entity
            Entity npc = null;
            for (ServerLevel level : event.getServer().getAllLevels()) {
                npc = level.getEntity(npcUuid);
                if (npc != null) {
                    break;
                }
            }
            
            // Remove if NPC no longer exists
            if (npc == null || !npc.isAlive() || npc.isRemoved()) {
                return true; // Remove from map
            }
            
            // If NPC is playing another animation, wait for cooldown
            if (state.isPlayingOtherAnimation) {
                state.cooldownTicks--;
                if (state.cooldownTicks <= 0) {
                    // Cooldown finished, assume other animation has ended
                    state.isPlayingOtherAnimation = false;
                    // Play default animation
                    NpcAnimationHandler.sendAnimationToNearbyPlayers(npc, state.defaultAnimation, 32);
                    state.cooldownTicks = DEFAULT_ANIMATION_COOLDOWN_TICKS;
                }
            } else {
                // Not playing other animation, periodically restart default animation
                // This ensures the animation loops if it's a looping animation
                state.cooldownTicks--;
                if (state.cooldownTicks <= 0) {
                    // Restart default animation
                    NpcAnimationHandler.sendAnimationToNearbyPlayers(npc, state.defaultAnimation, 32);
                    state.cooldownTicks = DEFAULT_ANIMATION_COOLDOWN_TICKS * 5; // Longer cooldown for default animation restarts
                }
            }
            
            return false; // Keep in map
        });
    }
}


































