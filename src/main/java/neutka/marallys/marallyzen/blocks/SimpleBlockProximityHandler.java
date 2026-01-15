package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.util.NarrationIcons;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.ClearNarrationPacket;
import neutka.marallys.marallyzen.network.NarratePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Proximity narration for simple blocks (mirror, old laptop, old TV).
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public class SimpleBlockProximityHandler {
    private static final int CHECK_INTERVAL = 2;
    private static final double PROXIMITY_RANGE = 5.0;
    private static final double LOOK_AT_THRESHOLD = 0.5;

    private static final int FADE_IN_TICKS = 5;
    private static final int FADE_OUT_TICKS = 3;
    private static final int STAY_TICKS = 999999;

    private static int tickCounter = 0;
    private static final Map<UUID, BlockPos> playerCurrentBlock = new HashMap<>();

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

        for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
            if (serverLevel == null) {
                continue;
            }

            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (player.level() != serverLevel) {
                    continue;
                }

                UUID playerId = player.getUUID();
                BlockPos currentBlock = playerCurrentBlock.get(playerId);

                BlockPos closestBlock = null;
                double closestDistance = Double.MAX_VALUE;
                Component message = null;

                BlockPos playerPos = player.blockPosition();
                int searchRadius = (int) Math.ceil(PROXIMITY_RANGE + 1);

                for (int x = -searchRadius; x <= searchRadius; x++) {
                    for (int y = -searchRadius; y <= searchRadius; y++) {
                        for (int z = -searchRadius; z <= searchRadius; z++) {
                            BlockPos checkPos = playerPos.offset(x, y, z);
                            BlockState state = serverLevel.getBlockState(checkPos);
                            Component candidateMessage = getNarrationMessage(state);
                            if (candidateMessage == null) {
                                continue;
                            }

                            Vec3 blockCenter = Vec3.atCenterOf(checkPos);
                            double distance = player.position().distanceTo(blockCenter);
                            if (distance <= PROXIMITY_RANGE && distance < closestDistance) {
                                if (isPlayerLookingAtBlock(player, blockCenter)) {
                                    closestBlock = checkPos;
                                    closestDistance = distance;
                                    message = candidateMessage;
                                }
                            }
                        }
                    }
                }

                if (closestBlock != null && message != null) {
                    boolean wasInRange = closestBlock.equals(currentBlock);
                    if (!wasInRange) {
                        onPlayerEnterProximity(player, closestBlock, message);
                        playerCurrentBlock.put(playerId, closestBlock);
                    }
                } else if (currentBlock != null) {
                    onPlayerExitProximity(player);
                    playerCurrentBlock.remove(playerId);
                }
            }
        }
    }

    private static Component getNarrationMessage(BlockState state) {
        Block block = state.getBlock();
        if (block == MarallyzenBlocks.MIRROR.get()) {
            return createNarrationMessage("narration.marallyzen.mirror_instruction");
        }
        if (block == MarallyzenBlocks.OLD_LAPTOP.get()) {
            return createNarrationMessage("narration.marallyzen.old_laptop_instruction");
        }
        if (block == MarallyzenBlocks.OLD_TV.get()) {
            if (state.hasProperty(OldTvBlock.ON) && state.getValue(OldTvBlock.ON)) {
                return createNarrationMessage("narration.marallyzen.old_tv_turn_off");
            }
            return createNarrationMessage("narration.marallyzen.old_tv_turn_on");
        }
        return null;
    }

    private static Component createNarrationMessage(String key) {
        Component pkm = NarrationIcons.rmb();
        return Component.translatable(key, pkm);
    }

    private static boolean isPlayerLookingAtBlock(ServerPlayer player, Vec3 blockCenter) {
        Vec3 playerEyePos = player.getEyePosition();
        Vec3 directionToBlock = blockCenter.subtract(playerEyePos).normalize();

        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = player.getXRot() * Mth.DEG_TO_RAD;

        double lookX = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double lookY = -Mth.sin(pitchRad);
        double lookZ = Mth.cos(yawRad) * Mth.cos(pitchRad);
        Vec3 lookDirection = new Vec3(lookX, lookY, lookZ).normalize();

        double dotProduct = directionToBlock.dot(lookDirection);
        return dotProduct >= LOOK_AT_THRESHOLD;
    }

    private static void onPlayerEnterProximity(ServerPlayer player, BlockPos pos, Component message) {
        NarratePacket packet = new NarratePacket(
            message,
            null,
            FADE_IN_TICKS,
            STAY_TICKS,
            FADE_OUT_TICKS
        );
        NetworkHelper.sendToPlayer(player, packet);
        Marallyzen.LOGGER.debug("[SimpleBlockProximityHandler] Sent narration to player {} for block at {}",
            player.getName().getString(), pos);
    }

    private static void onPlayerExitProximity(ServerPlayer player) {
        NetworkHelper.sendToPlayer(player, new ClearNarrationPacket());
        Marallyzen.LOGGER.debug("[SimpleBlockProximityHandler] Cleared narration for player {}",
            player.getName().getString());
    }
}


