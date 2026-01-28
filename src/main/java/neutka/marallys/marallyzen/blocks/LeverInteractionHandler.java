package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.LeverInteractionStartPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.util.EmoteConfigUtil;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.blocks.InteractiveLeverBlockEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Marallyzen.MODID)
public final class LeverInteractionHandler {
    private static final String EMOTE_GRAB = "lever_grab";
    private static final String EMOTE_SHAKE = "lever_shake";
    private static final String EMOTE_DOWN = "lever_down";
    private static final int MOVE_TICKS = 12;
    private static final double MOVE_STOP_DIST = 0.06;
    private static final double MAX_INTERACT_DIST = 1.5;
    private static final Map<UUID, LeverSequenceState> ACTIVE = new HashMap<>();

    private LeverInteractionHandler() {
    }

    public static boolean start(ServerPlayer player, BlockPos pos, BlockState state, Vec3 hitLocation) {
        if (player == null || pos == null || state == null) {
            return false;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            Marallyzen.LOGGER.info("[LeverInteract] start denied: player active {}", player.getGameProfile().getName());
            return false;
        }
        if (player.isSpectator() || !player.isAlive()) {
            Marallyzen.LOGGER.info("[LeverInteract] start denied: invalid state name={} spectator={} alive={}",
                player.getGameProfile().getName(), player.isSpectator(), player.isAlive());
            return false;
        }
        if (!isWithinInteractRange(player, pos, hitLocation)) {
            Marallyzen.LOGGER.info("[LeverInteract] start denied: too far player={} pos={}",
                player.getGameProfile().getName(), pos);
            return false;
        }
        if (!canInteractWhileGrounded(player)) {
            Marallyzen.LOGGER.info("[LeverInteract] start denied: not grounded player={} onGround={} flying={}",
                player.getGameProfile().getName(), player.onGround(), player.getAbilities().flying);
            return false;
        }
        if (state.getBlock() == MarallyzenBlocks.INTERACTIVE_LEVER.get()
            && state.getValue(LeverBlock.POWERED)) {
            Marallyzen.LOGGER.info("[LeverInteract] start denied: already powered player={} pos={}",
                player.getGameProfile().getName(), pos);
            return false;
        }

        int grabTicks = EmoteConfigUtil.getEmoteDurationTicks(EMOTE_GRAB);
        int shakeTicks = EmoteConfigUtil.getEmoteDurationTicks(EMOTE_SHAKE);
        int downTicks = EmoteConfigUtil.getEmoteDurationTicks(EMOTE_DOWN);

        Direction facing = state.getValue(LeverBlock.FACING);
        Vec3 targetPos = computeStandPosition(state, pos, player, facing);
        float yaw = facing.getOpposite().toYRot();
        float pitch = 0.0f;

        LeverSequenceState sequence = new LeverSequenceState(
            player,
            pos,
            player.level().dimension(),
            targetPos,
            yaw,
            pitch,
            grabTicks,
            shakeTicks,
            downTicks,
            MOVE_TICKS
        );
        ACTIVE.put(player.getUUID(), sequence);
        Marallyzen.LOGGER.info("[LeverInteract] start ok player={} pos={} target={}",
            player.getGameProfile().getName(), pos, targetPos);
        NetworkHelper.sendToPlayer(player, new neutka.marallys.marallyzen.network.LeverInteractionMovePacket(
            targetPos, yaw, pitch, MOVE_TICKS
        ));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, LeverSequenceState>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            LeverSequenceState state = iterator.next().getValue();
            ServerPlayer player = state.player;
            if (player == null || !player.isAlive()) {
                cleanup(iterator, state);
                continue;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                cleanup(iterator, state);
                continue;
            }
            if (!player.level().dimension().equals(state.dimension)) {
                cleanup(iterator, state);
                continue;
            }
            enforceLock(player, state);

            state.tick++;
            state.ageTicks++;
            if (state.ageTicks > state.maxAgeTicks) {
                cleanup(iterator, state);
                continue;
            }
            if (state.phase == Phase.MOVE) {
                if (state.tick >= state.moveTicks || state.isNearTarget()) {
                    state.phase = Phase.GRAB;
                    state.tick = 0;
                    Marallyzen.LOGGER.info(
                        "[LeverInteract] start emote cycle player={} pos={} yaw={} dist={}",
                        player.getGameProfile().getName(),
                        state.pos,
                        state.targetYaw,
                        new Vec3(state.targetPos.x - player.getX(), 0.0, state.targetPos.z - player.getZ()).length()
                    );
                    NetworkHelper.sendToPlayer(player, new LeverInteractionStartPacket(
                        state.targetPos, state.targetYaw, state.targetPitch, state.grabTicks, state.shakeTicks, state.downTicks
                    ));
                }
            } else if (state.phase == Phase.GRAB && state.tick >= state.grabTicks) {
                state.phase = Phase.SHAKE;
                setLeverPowered(serverLevel, state.pos, state.shakeTicks);
            } else if (state.phase == Phase.SHAKE && state.tick >= state.grabTicks + state.shakeTicks) {
                state.phase = Phase.DOWN;
            } else if (state.phase == Phase.DOWN && state.tick >= state.grabTicks + state.shakeTicks + state.downTicks) {
                cleanup(iterator, state);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LeverSequenceState state = ACTIVE.remove(player.getUUID());
            if (state != null) {
                // no-op
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LeverSequenceState state = ACTIVE.remove(player.getUUID());
            if (state != null) {
                // no-op
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    private static void enforceLock(ServerPlayer player, LeverSequenceState state) {
        player.setDeltaMovement(Vec3.ZERO);
        if (state.phase == Phase.MOVE) {
            player.setYRot(state.targetYaw);
            player.setXRot(state.targetPitch);
            player.yHeadRot = state.targetYaw;
            player.yBodyRot = state.targetYaw;
            player.fallDistance = 0.0f;
        } else {
            player.teleportTo((ServerLevel) player.level(), state.targetPos.x, state.targetPos.y, state.targetPos.z, state.targetYaw, state.targetPitch);
        }
    }

    private static void cleanup(Iterator<Map.Entry<UUID, LeverSequenceState>> iterator, LeverSequenceState state) {
        iterator.remove();
    }

    private static void setLeverPowered(ServerLevel level, BlockPos pos, int shakeTicks) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LeverBlock)) {
            return;
        }
        if (state.getValue(LeverBlock.POWERED)) {
            return;
        }
        level.setBlock(pos, state.setValue(LeverBlock.POWERED, true), 3);
        if (state.getBlock() == MarallyzenBlocks.INTERACTIVE_LEVER.get()) {
            if (level.getBlockEntity(pos) instanceof InteractiveLeverBlockEntity leverEntity) {
                leverEntity.setAnimData(software.bernie.geckolib.constant.DataTickets.USE_TICKS, Math.max(1, shakeTicks));
                leverEntity.triggerAnim("controller", "pull");
            }
        }
    }

    private static Vec3 computeStandPosition(BlockState state, BlockPos pos, Player player, Direction facing) {
        AttachFace face = state.getValue(LeverBlock.FACE);
        Vec3 center = Vec3.atCenterOf(pos);
        double distance = face == AttachFace.WALL ? 0.25 : 1.0;
        Direction offsetDir = face == AttachFace.WALL ? facing : facing.getOpposite();
        Vec3 offset = new Vec3(offsetDir.getStepX() * distance, 0.0, offsetDir.getStepZ() * distance);
        double y = player.getY();
        if (face == AttachFace.CEILING) {
            y = Math.min(y, pos.getY() + 0.3);
        } else if (face == AttachFace.FLOOR) {
            y = Math.max(y, pos.getY());
        }
        return new Vec3(center.x + offset.x, y, center.z + offset.z);
    }

    private static boolean isWithinInteractRange(Player player, BlockPos pos, Vec3 hitLocation) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = hitLocation != null ? hitLocation : Vec3.atCenterOf(pos);
        return eye.distanceTo(target) <= MAX_INTERACT_DIST;
    }

    private static boolean canInteractWhileGrounded(ServerPlayer player) {
        if (player.getAbilities().flying) {
            return false;
        }
        if (!player.onGround()) {
            return false;
        }
        return true;
    }

    private enum Phase {
        MOVE,
        GRAB,
        SHAKE,
        DOWN
    }

    private static final class LeverSequenceState {
        private final ServerPlayer player;
        private final BlockPos pos;
        private final ResourceKey<Level> dimension;
        private final Vec3 targetPos;
        private final float targetYaw;
        private final float targetPitch;
        private final int grabTicks;
        private final int shakeTicks;
        private final int downTicks;
        private final int moveTicks;
        private final int maxAgeTicks;
        private Phase phase = Phase.MOVE;
        private int tick;
        private int ageTicks;

        private LeverSequenceState(ServerPlayer player, BlockPos pos, ResourceKey<Level> dimension, Vec3 targetPos,
                                   float targetYaw, float targetPitch, int grabTicks, int shakeTicks, int downTicks, int moveTicks) {
            this.player = player;
            this.pos = pos;
            this.dimension = dimension;
            this.targetPos = targetPos;
            this.targetYaw = targetYaw;
            this.targetPitch = targetPitch;
            this.grabTicks = Math.max(1, grabTicks);
            this.shakeTicks = Math.max(1, shakeTicks);
            this.downTicks = Math.max(1, downTicks);
            this.moveTicks = Math.max(1, moveTicks);
            this.maxAgeTicks = this.moveTicks + this.grabTicks + this.shakeTicks + this.downTicks + 40;
        }

        private boolean isNearTarget() {
            Vec3 flat = new Vec3(targetPos.x - player.getX(), 0.0, targetPos.z - player.getZ());
            return flat.length() <= MOVE_STOP_DIST;
        }
    }
}
