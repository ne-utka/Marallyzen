package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager;
import neutka.marallys.marallyzen.network.ClearNarrationPacket;
import neutka.marallys.marallyzen.network.NarratePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unified proximity narration for interactive blocks.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public class InteractiveBlockProximityHandler {
    private static final int CHECK_INTERVAL = 2;
    private static final double TARGET_RANGE = 5.0;

    private static final int FADE_IN_TICKS = 5;
    private static final int FADE_OUT_TICKS = 3;
    private static final int STAY_TICKS = 999999;

    private static int tickCounter = 0;
    private static final Map<UUID, Target> playerCurrentTarget = new HashMap<>();
    private static final Map<UUID, Long> playerLastSeenTick = new HashMap<>();
    private static final Map<UUID, Long> playerLastClearTick = new HashMap<>();
    private static final Map<UUID, String> playerLastMessage = new HashMap<>();
    private static final long CLEAR_COOLDOWN_TICKS = 10L;
    private static final long TARGET_TIMEOUT_TICKS = 6L;

    private record Target(BlockPos pos, InteractiveBlockTargeting.Type type) {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
                Target currentTarget = playerCurrentTarget.get(playerId);
                long gameTime = serverLevel.getGameTime();
                if (DictaphoneScriptManager.isNarrationLocked(playerId, gameTime)) {
                    continue;
                }
                if (DictaphoneScriptManager.hasNarrationLock(playerId)) {
                    DictaphoneScriptManager.clearNarrationLock(playerId);
                    onPlayerExitProximity(player);
                    playerLastClearTick.put(playerId, gameTime);
                }

                Vec3 eyePos = player.getEyePosition();
                Vec3 endPos = eyePos.add(player.getLookAngle().scale(TARGET_RANGE));
                HitResult hitResult = player.level().clip(new ClipContext(
                    eyePos,
                    endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
                ));

                Target newTarget = null;
                Component message = null;

                if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hitResult;
                    BlockPos hitPos = blockHit.getBlockPos();
                    BlockState state = serverLevel.getBlockState(hitPos);
                    InteractiveBlockTargeting.Type type = InteractiveBlockTargeting.getType(state);
                    if (type != InteractiveBlockTargeting.Type.NONE) {
                        if (type != InteractiveBlockTargeting.Type.CHAIN
                            || !player.getPersistentData().getBoolean("marallyzen_chain_hang")) {
                            newTarget = new Target(hitPos, type);
                            message = InteractiveBlockTargeting.getNarrationMessage(state);
                        }
                    }
                }

                if (newTarget != null && message != null) {
                    boolean sameTarget = currentTarget != null
                        && currentTarget.pos().equals(newTarget.pos())
                        && currentTarget.type() == newTarget.type();
                    String messageText = message.getString();
                    String previousMessage = playerLastMessage.get(playerId);
                    if (!sameTarget || previousMessage == null || !previousMessage.equals(messageText)) {
                        onPlayerEnterProximity(player, newTarget.pos(), message);
                        playerCurrentTarget.put(playerId, newTarget);
                        playerLastMessage.put(playerId, messageText);
                    }
                    playerLastSeenTick.put(playerId, gameTime);
                } else if (currentTarget != null) {
                    long lastSeen = playerLastSeenTick.getOrDefault(playerId, gameTime);
                    long lastClear = playerLastClearTick.getOrDefault(playerId, 0L);
                    if (gameTime - lastSeen >= TARGET_TIMEOUT_TICKS && gameTime - lastClear >= CLEAR_COOLDOWN_TICKS) {
                        onPlayerExitProximity(player);
                        playerLastClearTick.put(playerId, gameTime);
                    }
                    if (gameTime - lastSeen >= TARGET_TIMEOUT_TICKS) {
                        playerCurrentTarget.remove(playerId);
                        playerLastMessage.remove(playerId);
                    }
                }
            }
        }
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
        Marallyzen.LOGGER.debug("[InteractiveBlockProximityHandler] Sent narration to player {} for block at {}",
            player.getName().getString(), pos);
    }

    private static void onPlayerExitProximity(ServerPlayer player) {
        NetworkHelper.sendToPlayer(player, new ClearNarrationPacket());
        Marallyzen.LOGGER.debug("[InteractiveBlockProximityHandler] Cleared narration for player {}",
            player.getName().getString());
    }
}
