package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles proximity detection for poster blocks (narration disabled).
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public class PosterProximityHandler {

    private static final int CHECK_INTERVAL = 2; // Check every 2 ticks for smoother detection
    private static final double PROXIMITY_RANGE = 5.0; // 5 blocks
    private static final double LOOK_AT_THRESHOLD = 0.5; // Dot product threshold (cosine of ~60 degrees)

    private static int tickCounter = 0;

    // Track which players are viewing which poster blocks
    private static final Map<UUID, BlockPos> playerCurrentPoster = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        return;
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
}
