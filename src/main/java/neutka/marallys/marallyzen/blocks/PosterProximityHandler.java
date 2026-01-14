package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.util.NarrationIcons;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.NarratePacket;
import neutka.marallys.marallyzen.network.ClearNarrationPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles proximity detection for poster blocks - shows narration text when player approaches and looks at poster.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public class PosterProximityHandler {
    
    private static final int CHECK_INTERVAL = 2; // Check every 2 ticks for smoother detection
    private static final double PROXIMITY_RANGE = 5.0; // 5 blocks
    private static final double LOOK_AT_THRESHOLD = 0.5; // Dot product threshold (cosine of ~60 degrees)
    
    // Narration timing (same as NPC messages for consistency)
    private static final int FADE_IN_TICKS = 5; // Match NPC narration fade-in
    private static final int FADE_OUT_TICKS = 3; // Match NPC narration fade-out
    private static final int STAY_TICKS = 999999; // Very long stay time - will be cleared manually when player leaves
    
    private static int tickCounter = 0;
    
    // Track which players are viewing which poster blocks
    private static final Map<UUID, BlockPos> playerCurrentPoster = new HashMap<>();
    
    // Create narration message with colored "ПКМ"
    private static Component createNarrationMessage() {
        return Component.literal("Используйте ")
            .append(NarrationIcons.rmb())
            .append(Component.literal(" для осмотра"));
    }
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (InteractiveBlockProximityHandlerEnabled.DISABLED) {
            return;
        }
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        // Process each dimension
        for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
            if (serverLevel == null) {
                continue;
            }
            
            // For each player in this dimension
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (player.level() != serverLevel) {
                    continue;
                }
                
                UUID playerId = player.getUUID();
                BlockPos currentPoster = playerCurrentPoster.get(playerId);
                
                // Find closest poster block
                BlockPos closestPoster = null;
                double closestDistance = Double.MAX_VALUE;
                
                // Search in a reasonable area around player (proximity range + buffer)
                BlockPos playerPos = player.blockPosition();
                int searchRadius = (int) Math.ceil(PROXIMITY_RANGE + 1);
                
                for (int x = -searchRadius; x <= searchRadius; x++) {
                    for (int y = -searchRadius; y <= searchRadius; y++) {
                        for (int z = -searchRadius; z <= searchRadius; z++) {
                            BlockPos checkPos = playerPos.offset(x, y, z);
                            BlockState state = serverLevel.getBlockState(checkPos);
                            
                            if (state.getBlock() instanceof PosterBlock) {
                                Vec3 blockCenter = Vec3.atCenterOf(checkPos);
                                double distance = player.position().distanceTo(blockCenter);
                                
                                // Check distance
                                if (distance <= PROXIMITY_RANGE && distance < closestDistance) {
                                    // Check if player is looking at the block (dot product)
                                    if (isPlayerLookingAtBlock(player, blockCenter)) {
                                        closestPoster = checkPos;
                                        closestDistance = distance;
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Update narration overlay
                if (closestPoster != null) {
                    boolean wasInRange = closestPoster.equals(currentPoster);
                    
                    if (!wasInRange) {
                        // Player entered range - show narration
                        onPlayerEnterProximity(player, closestPoster);
                        playerCurrentPoster.put(playerId, closestPoster);
                    }
                    // If player is still in range, narration will continue showing (stayTicks is very large)
                } else {
                    // Player is not near any poster or not looking at it
                    if (currentPoster != null) {
                        // Player left range - clear narration
                        onPlayerExitProximity(player);
                        playerCurrentPoster.remove(playerId);
                    }
                }
            }
        }
    }
    
    /**
     * Checks if player is looking at the block using dot product.
     */
    private static boolean isPlayerLookingAtBlock(ServerPlayer player, Vec3 blockCenter) {
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 directionToBlock = blockCenter.subtract(playerEyePos).normalize();
        
        // Get player's look direction
        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = player.getXRot() * Mth.DEG_TO_RAD;
        
        double lookX = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double lookY = -Mth.sin(pitchRad);
        double lookZ = Mth.cos(yawRad) * Mth.cos(pitchRad);
        Vec3 lookDirection = new Vec3(lookX, lookY, lookZ).normalize();
        
        // Calculate dot product
        double dotProduct = directionToBlock.dot(lookDirection);
        
        return dotProduct >= LOOK_AT_THRESHOLD;
    }
    
    private static void onPlayerEnterProximity(ServerPlayer player, BlockPos posterPos) {
        // Send narration packet with smooth fade-in animation (same as NPC messages)
        Component message = createNarrationMessage();
        NarratePacket packet = new NarratePacket(
            message,
            null, // No UUID for poster blocks
            FADE_IN_TICKS,
            STAY_TICKS, // Very long stay - will be cleared manually when player leaves
            FADE_OUT_TICKS
        );
        NetworkHelper.sendToPlayer(player, packet);
        Marallyzen.LOGGER.debug("[PosterProximityHandler] Sent narration to player {} for poster at {}", 
            player.getName().getString(), posterPos);
    }
    
    private static void onPlayerExitProximity(ServerPlayer player) {
        // Clear narration overlay (will fade out smoothly on client)
        NetworkHelper.sendToPlayer(player, new ClearNarrationPacket());
        Marallyzen.LOGGER.debug("[PosterProximityHandler] Cleared narration for player {}", 
            player.getName().getString());
    }
}



