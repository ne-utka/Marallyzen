package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import neutka.marallys.marallyzen.entity.PosterEntity;
import neutka.marallys.marallyzen.client.ClientPosterManager;
import neutka.marallys.marallyzen.client.narration.NarrationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles player interactions with PosterEntity.
 * Allows flipping posterfull and paperposterfull posters when right-clicked in VIEWING state.
 */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class PosterEntityInteractionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PosterEntityInteractionHandler.class);
    
    /**
     * Handle block interaction - prevent interaction with PosterBlock if PosterEntity is active
     * and immediately hide the block if it became visible
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().level().isClientSide) {
            return;
        }
        
        BlockPos pos = event.getPos();
        
        // Check if there's an active PosterEntity at this position
        if (ClientPosterManager.hasClientPoster(pos)) {
            // Block should be hidden, but if it became visible (e.g., server synced it back),
            // hide it immediately
            if (event.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                net.minecraft.world.level.block.state.BlockState currentState = clientLevel.getBlockState(pos);
                if (currentState.getBlock() != Blocks.AIR) {
                    clientLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                    LOGGER.debug("PosterEntityInteractionHandler: Hid block at {} (was visible during click)", pos);
                }
            }
            
            // Cancel the event to prevent block interaction
            event.setCanceled(true);
        }
    }
    
    /**
     * Handle entity interaction via PlayerInteractEvent (more reliable than InputEvent)
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof PosterEntity posterEntity)) {
            return;
        }
        
        // Only handle on client side
        if (!event.getEntity().level().isClientSide) {
            return;
        }
        
        LOGGER.debug("PosterEntityInteractionHandler: EntityInteract event, state = {}, posterNumber = {}", 
            posterEntity.getCurrentState(), posterEntity.getPosterNumber());
        
        // Only allow flipping in VIEWING state
        if (posterEntity.getCurrentState() != PosterEntity.State.VIEWING) {
            LOGGER.debug("PosterEntityInteractionHandler: Poster is not in VIEWING state, ignoring");
            return;
        }
        
        int posterNumber = posterEntity.getPosterNumber();
        // Only allow flipping for posterfull (1-10) and paperposterfull (12-13)
        boolean canFlip = (posterNumber >= 1 && posterNumber <= 10) || (posterNumber == 12 || posterNumber == 13);
        
        if (!canFlip) {
            LOGGER.debug("PosterEntityInteractionHandler: Poster {} cannot be flipped", posterNumber);
            return;
        }
        
        // Toggle flipped state (this will trigger animation)
        boolean newFlipped = !posterEntity.isFlipped();
        posterEntity.setFlipped(newFlipped);
        
        LOGGER.warn("PosterEntityInteractionHandler: Flipped poster {} from {} to {} (with animation)", 
            posterNumber, !newFlipped, newFlipped);
        
        event.setCanceled(true);
    }
    
    /**
     * Fallback handler via InputEvent (in case EntityInteract doesn't fire)
     * Uses raycast to find PosterEntity in front of player instead of relying on hitResult
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST) // Use HIGHEST priority to catch before other handlers
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        // Only handle right click (button 1, action 1 = press)
        if (event.getButton() != 1 || event.getAction() != 1) {
            return;
        }
        
        // Use raycast to find entities in front of player
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();
        double reachDistance = 5.0; // Maximum interaction distance
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));
        
        // Check all PosterEntity instances in the level
        PosterEntity closestPoster = null;
        double closestDistance = Double.MAX_VALUE;
        
        // Use getEntitiesOfClass to get all PosterEntity instances
        for (PosterEntity posterEntity : mc.level.getEntitiesOfClass(PosterEntity.class, 
                new AABB(eyePos.x - reachDistance, eyePos.y - reachDistance, eyePos.z - reachDistance,
                         eyePos.x + reachDistance, eyePos.y + reachDistance, eyePos.z + reachDistance))) {
            
            // Only check posters in VIEWING state
            if (posterEntity.getCurrentState() != PosterEntity.State.VIEWING) {
                continue;
            }
            
            // Check if poster can be flipped
            int posterNumber = posterEntity.getPosterNumber();
            boolean canFlip = (posterNumber >= 1 && posterNumber <= 10) || (posterNumber == 12 || posterNumber == 13);
            if (!canFlip) {
                continue;
            }
            
            // Calculate distance from player's look vector to poster center
            Vec3 posterPos = posterEntity.position();
            Vec3 toPoster = posterPos.subtract(eyePos);
            double distance = toPoster.length();
            
            // Check if poster is within reach distance
            if (distance > reachDistance) {
                continue;
            }
            
            // Check if poster is roughly in front of player (dot product check)
            Vec3 normalizedLook = lookVec.normalize();
            Vec3 normalizedToPoster = toPoster.normalize();
            double dot = normalizedLook.dot(normalizedToPoster);
            
            // Only consider posters that are in front (dot > 0.7 means roughly within 45 degrees)
            if (dot < 0.7) {
                continue;
            }
            
            // Check if ray intersects with poster's bounding box
            AABB posterBox = new AABB(
                posterPos.x - 0.5,
                posterPos.y - 0.5,
                posterPos.z - 0.5,
                posterPos.x + 0.5,
                posterPos.y + 0.5,
                posterPos.z + 0.5
            );
            Vec3 hitVec = posterBox.clip(eyePos, endPos).orElse(null);
            if (hitVec == null) {
                continue;
            }
            
            // Found a valid poster, check if it's the closest one
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPoster = posterEntity;
            }
        }
        
        if (closestPoster != null) {
            LOGGER.warn("PosterEntityInteractionHandler: Found closest PosterEntity at distance {}, state = {}, posterNumber = {}", 
                closestDistance, closestPoster.getCurrentState(), closestPoster.getPosterNumber());
            
            // Toggle flipped state (this will trigger animation)
            boolean newFlipped = !closestPoster.isFlipped();
            closestPoster.setFlipped(newFlipped);
            
            LOGGER.warn("PosterEntityInteractionHandler: Flipped poster {} from {} to {} (with animation)", 
                closestPoster.getPosterNumber(), !newFlipped, newFlipped);
            
            event.setCanceled(true);
        }
    }
    
    /**
     * Shows narration when poster transitions to VIEWING state.
     * Called from ClientPosterManager or PosterEntity tick.
     * Narration will stay visible while poster is in VIEWING state.
     * @param posterNumber The poster number (11 = oldposter, which doesn't show narration)
     */
    public static void showFlipNarration(PosterEntity posterEntity) {
        if (posterEntity == null) {
            return;
        }
        int posterNumber = posterEntity.getPosterNumber();
        // Don't show narration for oldposter (ID 11)
        if (posterNumber == 11) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean flipped = posterEntity.isFlipped();
        Component narrationText = flipped
            ? neutka.marallys.marallyzen.blocks.InteractiveBlockNarrations.posterFlipToFrontMessage()
            : neutka.marallys.marallyzen.blocks.InteractiveBlockNarrations.posterFlipToBackMessage();
        NarrationManager manager = NarrationManager.getInstance();
        // Use very long stay time (999999 ticks) - will be cleared when poster leaves VIEWING state
        manager.startNarration(narrationText, null, 5, 999999, 3);
    }
    
    /**
     * Shows narration when poster transitions to VIEWING state (backward compatibility).
     * @deprecated Use showFlipNarration(PosterEntity) instead
     */
    @Deprecated
    public static void showFlipNarration() {
        // Default to showing narration (for non-oldposter posters)
        showFlipNarration(null);
    }
    
    /**
     * Clears narration when poster leaves VIEWING state.
     */
    public static void clearFlipNarration() {
        NarrationManager manager = NarrationManager.getInstance();
        manager.clearNarration();
    }
}
