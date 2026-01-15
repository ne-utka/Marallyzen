package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.network.InteractiveChainHangPacket;
import neutka.marallys.marallyzen.network.InteractiveChainAttachPacket;
import neutka.marallys.marallyzen.network.InteractiveChainJumpPacket;
import neutka.marallys.marallyzen.network.InteractiveChainSwingStatePacket;
import neutka.marallys.marallyzen.network.ClearNarrationPacket;
import neutka.marallys.marallyzen.network.NarratePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;
import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.util.NarrationIcons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles smooth jump-to-chain animation by moving the player along a short arc.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public class InteractiveChainJumpHandler {
    public static final String CHAIN_EMOTE_ID = "SPE_Interactive Chain Jump";
    private static final int JUMP_TICKS = 12;
    private static final double ARC_HEIGHT = 0.6;
    private static final double MIN_SWING_LENGTH = 0.6;
    private static final double CHAIN_ANCHOR_Y_OFFSET = 0.5;
    private static final double CHEST_HEIGHT_OFFSET = 0.4;
    private static final double SWING_GRAVITY = 0.08;
    private static final double SWING_INPUT = 0.04;
    private static final double SWING_DAMPING = 0.985;
    private static final double SWING_MAX_SPEED = 0.35;
    private static final double HANG_BACK_OFFSET = 0.25;
    private static final int SWING_INPUT_TIMEOUT_TICKS = 3;
    private static final String HANG_TAG = "marallyzen_chain_hang";

    private static final Map<UUID, JumpState> activeJumps = new HashMap<>();
    private static final Map<UUID, HangState> activeHangs = new HashMap<>();

    private static final class JumpState {
        private final ServerPlayer player;
        private final BlockPos chainPos;
        private final ResourceKey<Level> dimension;
        private final Vec3 start;
        private final Vec3 control;
        private final Vec3 end;
        private final float startYaw;
        private final float startPitch;
        private final float endYaw;
        private final float endPitch;
        private int tick;

        private JumpState(ServerPlayer player, BlockPos chainPos, ResourceKey<Level> dimension, Vec3 start, Vec3 control, Vec3 end,
                          float startYaw, float startPitch, float endYaw, float endPitch) {
            this.player = player;
            this.chainPos = chainPos;
            this.dimension = dimension;
            this.start = start;
            this.control = control;
            this.end = end;
            this.startYaw = startYaw;
            this.startPitch = startPitch;
            this.endYaw = endYaw;
            this.endPitch = endPitch;
        }
    }

    private static final class HangState {
        private final ServerPlayer player;
        private final BlockPos chainPos;
        private final BlockPos chainRoot;
        private final Vec3 anchor;
        private final ResourceKey<Level> dimension;
        private final double length;
        private final double chestOffset;
        private Vec3 offset;
        private Vec3 velocity;
        private int inputDirection;
        private long lastInputTick;
        private long lastLogTick;

        private HangState(ServerPlayer player, BlockPos chainPos, BlockPos chainRoot, Vec3 anchor, ResourceKey<Level> dimension, double length, double chestOffset, Vec3 offset, Vec3 velocity) {
            this.player = player;
            this.chainPos = chainPos;
            this.chainRoot = chainRoot;
            this.anchor = anchor;
            this.dimension = dimension;
            this.length = length;
            this.chestOffset = chestOffset;
            this.offset = offset;
            this.velocity = velocity;
            this.lastLogTick = -20L;
        }
    }

    private static final class HangGeometry {
        private final BlockPos chainRoot;
        private final Vec3 anchor;
        private final Vec3 feetPos;
        private final Vec3 chainEnd;
        private final double chestOffset;
        private final Vec3 offset;
        private final double length;

        private HangGeometry(BlockPos chainRoot, Vec3 anchor, Vec3 feetPos, Vec3 chainEnd, double chestOffset, Vec3 offset, double length) {
            this.chainRoot = chainRoot;
            this.anchor = anchor;
            this.feetPos = feetPos;
            this.chainEnd = chainEnd;
            this.chestOffset = chestOffset;
            this.offset = offset;
            this.length = length;
        }
    }

    public static void startJump(ServerPlayer player, BlockPos chainPos) {
        if (player == null || chainPos == null) {
            return;
        }
        if (activeJumps.containsKey(player.getUUID())) {
            return;
        }
        if (activeHangs.containsKey(player.getUUID())) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }

        HangGeometry geometry = computeHangGeometry(player.level(), chainPos, player);
        if (geometry == null) {
            return;
        }
        for (HangState hang : activeHangs.values()) {
            if (geometry.chainRoot.equals(hang.chainRoot)) {
                return;
            }
        }

        Vec3 start = player.position();
        Vec3 end = geometry.feetPos;
        Vec3 control = start.add(end).scale(0.5).add(0.0, ARC_HEIGHT, 0.0);

        float startYaw = player.getYRot();
        float startPitch = player.getXRot();

        float endYaw = (float) (Math.toDegrees(Math.atan2(end.z - start.z, end.x - start.x)) - 90.0f);
        float endPitch = 0.0f;

        activeJumps.put(player.getUUID(), new JumpState(player, chainPos, player.level().dimension(), start, control, end, startYaw, startPitch, endYaw, endPitch));
        NetworkHelper.sendToPlayer(player, new InteractiveChainJumpPacket(
                player.getUUID(),
                start,
                control,
                end,
                startYaw,
                startPitch,
                endYaw,
                endPitch,
                player.level().getGameTime(),
                JUMP_TICKS
        ));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!activeJumps.isEmpty()) {
            Iterator<Map.Entry<UUID, JumpState>> iterator = activeJumps.entrySet().iterator();
            while (iterator.hasNext()) {
                JumpState state = iterator.next().getValue();
                ServerPlayer player = state.player;
                if (player == null || player.isRemoved()) {
                    iterator.remove();
                    continue;
                }
                if (!player.level().dimension().equals(state.dimension)) {
                    iterator.remove();
                    continue;
                }

                if (player.level() instanceof ServerLevel) {
                    state.tick++;
                    float t = state.tick / (float) JUMP_TICKS;
                    if (t >= 1.0f) {
                        syncPlayerPosition(player, state.end, state.endYaw, state.endPitch, true);
                        if (player.level().getBlockState(state.chainPos).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
                            beginHang(player, state.chainPos);
                        } else {
                            stopHang(player);
                        }
                        iterator.remove();
                        continue;
                    }

                    float eased = easeInOut(t);
                    Vec3 pos = quadraticBezier(state.start, state.control, state.end, eased);

                    float yaw = Mth.rotLerp(eased, state.startYaw, state.endYaw);
                    float pitch = Mth.lerp(eased, state.startPitch, state.endPitch);

                    syncPlayerPosition(player, pos, yaw, pitch, true);
                    player.setPose(Pose.STANDING);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.fallDistance = 0.0f;
                } else {
                    iterator.remove();
                }
            }
        }

        if (!activeHangs.isEmpty()) {
            Iterator<Map.Entry<UUID, HangState>> hangIterator = activeHangs.entrySet().iterator();
            while (hangIterator.hasNext()) {
                HangState state = hangIterator.next().getValue();
                ServerPlayer player = state.player;
                if (player == null || player.isRemoved()) {
                    if (player != null) {
                        stopHang(player);
                    }
                    hangIterator.remove();
                    continue;
                }
                if (!player.level().dimension().equals(state.dimension)) {
                    stopHang(player);
                    hangIterator.remove();
                    continue;
                }

                if (!(player.level() instanceof ServerLevel)) {
                    stopHang(player);
                    hangIterator.remove();
                    continue;
                }

                if (player.isShiftKeyDown()) {
                    stopHang(player);
                    hangIterator.remove();
                    continue;
                }

                if (player.level().getBlockState(state.chainPos).getBlock() != MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
                    stopHang(player);
                    hangIterator.remove();
                    continue;
                }

                applySwing(player, state);
            }
        }
    }

    private static float easeInOut(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static Vec3 quadraticBezier(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        float u = 1.0f - t;
        return p0.scale(u * u).add(p1.scale(2.0f * u * t)).add(p2.scale(t * t));
    }

    private static void applySwing(ServerPlayer player, HangState state) {
        long gameTime = player.level().getGameTime();
        int input = resolveInputDirection(state, gameTime);

        Vec3 offset = state.offset;
        Vec3 velocity = state.velocity;
        Vec3 radial = offset.lengthSqr() > 1.0E-6 ? offset.normalize() : new Vec3(0.0, -1.0, 0.0);

        velocity = velocity.add(0.0, -SWING_GRAVITY, 0.0);
        if (input != 0) {
            Vec3 forward = getHorizontalForward(player);
            Vec3 tangent = forward.subtract(radial.scale(forward.dot(radial)));
            if (tangent.lengthSqr() > 1.0E-6) {
                velocity = velocity.add(tangent.normalize().scale(SWING_INPUT * input));
            }
        }

        velocity = velocity.scale(SWING_DAMPING);
        if (velocity.lengthSqr() > SWING_MAX_SPEED * SWING_MAX_SPEED) {
            velocity = velocity.normalize().scale(SWING_MAX_SPEED);
        }

        Vec3 nextOffset = offset.add(velocity);
        if (nextOffset.lengthSqr() < 1.0E-6) {
            nextOffset = new Vec3(0.0, -1.0, 0.0);
        }
        radial = nextOffset.normalize();
        nextOffset = radial.scale(state.length);
        velocity = velocity.subtract(radial.scale(velocity.dot(radial)));

        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
        player.setPose(Pose.STANDING);

        Vec3 chainEnd = state.anchor.add(nextOffset);
        Vec3 pos = chainEnd.subtract(0.0, state.chestOffset, 0.0).add(computeBackOffset(nextOffset));
        if (collidesWithNonChain(player.level(), player, pos)) {
            nextOffset = offset;
            velocity = Vec3.ZERO;
            chainEnd = state.anchor.add(nextOffset);
            pos = chainEnd.subtract(0.0, state.chestOffset, 0.0).add(computeBackOffset(nextOffset));
        }

        state.offset = nextOffset;
        state.velocity = velocity;

        syncPlayerPosition(player, pos, player.getYRot(), player.getXRot(), false);
        NetworkHelper.sendToAll(new InteractiveChainSwingStatePacket(state.chainRoot, state.anchor, nextOffset, true, gameTime));

        if (gameTime - state.lastLogTick >= 20L) {
            state.lastLogTick = gameTime;
            Marallyzen.LOGGER.debug(
                "[InteractiveChain] swing tick player={} root={} anchor=({}, {}, {}) offset=({}, {}, {}) input={}",
                player.getGameProfile().getName(),
                state.chainRoot,
                state.anchor.x, state.anchor.y, state.anchor.z,
                nextOffset.x, nextOffset.y, nextOffset.z,
                input
            );
        }
    }

    private static Vec3 getHorizontalForward(ServerPlayer player) {
        float yawRad = (float) Math.toRadians(player.getYRot());
        double x = -Mth.sin(yawRad);
        double z = Mth.cos(yawRad);
        Vec3 horizontal = new Vec3(x, 0.0, z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return horizontal.normalize();
    }

    private static boolean collidesWithNonChain(Level level, ServerPlayer player, Vec3 targetPos) {
        AABB moved = player.getBoundingBox().move(targetPos.subtract(player.position()));
        int minX = Mth.floor(moved.minX);
        int minY = Mth.floor(moved.minY);
        int minZ = Mth.floor(moved.minZ);
        int maxX = Mth.floor(moved.maxX);
        int maxY = Mth.floor(moved.maxY);
        int maxZ = Mth.floor(moved.maxZ);
        CollisionContext context = CollisionContext.of(player);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (level.getBlockState(pos).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
                continue;
            }
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos, context);
            if (shape.isEmpty()) {
                continue;
            }
            VoxelShape movedShape = shape.move(pos.getX(), pos.getY(), pos.getZ());
            if (Shapes.joinIsNotEmpty(movedShape, Shapes.create(moved), BooleanOp.AND)) {
                return true;
            }
        }
        return false;
    }

    private static int resolveInputDirection(HangState state, long gameTime) {
        if (gameTime - state.lastInputTick > SWING_INPUT_TIMEOUT_TICKS) {
            return 0;
        }
        return state.inputDirection;
    }

    private static void beginHang(ServerPlayer player, BlockPos chainPos) {
        HangGeometry geometry = computeHangGeometry(player.level(), chainPos, player);
        if (geometry == null) {
            return;
        }

        Vec3 anchor = geometry.anchor;
        Vec3 offset = geometry.offset;
        double length = geometry.length;
        double chestOffset = geometry.chestOffset;
        Vec3 velocity = Vec3.ZERO;
        Vec3 hangPos = geometry.feetPos;

        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
        player.getPersistentData().putBoolean(HANG_TAG, true);
        syncPlayerPosition(player, hangPos, player.getYRot(), player.getXRot(), false);

        activeHangs.put(player.getUUID(), new HangState(player, chainPos, geometry.chainRoot, anchor, player.level().dimension(), length, chestOffset, offset, velocity));
        NetworkHelper.sendToPlayer(player, new InteractiveChainHangPacket(true, anchor, length));
        NetworkHelper.sendToAll(new InteractiveChainAttachPacket(player.getUUID(), geometry.chainRoot, anchor, length, true));
        NetworkHelper.sendToAll(new InteractiveChainSwingStatePacket(geometry.chainRoot, anchor, offset, true, player.level().getGameTime()));
        NetworkHelper.sendToPlayer(
            player,
            new NarratePacket(
                Component.translatable("narration.marallyzen.interactive_chain_swing", NarrationIcons.rmb()),
                null,
                5,
                999999,
                3
            )
        );
    }

    private static HangGeometry computeHangGeometry(Level level, BlockPos chainPos, ServerPlayer player) {
        if (level == null || chainPos == null || player == null) {
            return null;
        }
        BlockPos top = findChainTop(level, chainPos);
        BlockPos bottom = findChainBottom(level, chainPos);
        if (top == null || bottom == null) {
            return null;
        }

        Vec3 anchor = new Vec3(top.getX() + 0.5, top.getY() + CHAIN_ANCHOR_Y_OFFSET, top.getZ() + 0.5);
        Vec3 chainEnd = new Vec3(bottom.getX() + 0.5, bottom.getY() + 0.5, bottom.getZ() + 0.5);
        Vec3 offset = chainEnd.subtract(anchor);
        double length = offset.length();
        if (length < MIN_SWING_LENGTH) {
            length = MIN_SWING_LENGTH;
            if (offset.lengthSqr() < 1.0E-6) {
                offset = new Vec3(0.0, -length, 0.0);
            } else {
                offset = offset.normalize().scale(length);
            }
        }
        double chestOffset = Math.max(0.8, player.getEyeHeight() - CHEST_HEIGHT_OFFSET);
        Vec3 feetPos = anchor.add(offset).subtract(0.0, chestOffset, 0.0).add(computeBackOffset(offset));
        return new HangGeometry(top, anchor, feetPos, chainEnd, chestOffset, offset, length);
    }

    private static Vec3 computeBackOffset(Vec3 offset) {
        Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }
        return horizontal.normalize().scale(-HANG_BACK_OFFSET);
    }

    private static BlockPos findChainTop(Level level, BlockPos pos) {
        BlockPos current = pos;
        while (true) {
            BlockPos above = current.above();
            if (level.getBlockState(above).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
                current = above;
                continue;
            }
            return current;
        }
    }

    private static BlockPos findChainBottom(Level level, BlockPos pos) {
        BlockPos current = pos;
        while (true) {
            BlockPos below = current.below();
            if (level.getBlockState(below).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
                current = below;
                continue;
            }
            return current;
        }
    }

    private static void stopHang(ServerPlayer player) {
        HangState state = activeHangs.get(player.getUUID());
        NetworkHelper.sendToPlayer(player, new InteractiveChainHangPacket(false, Vec3.ZERO, 0.0));
        NetworkHelper.sendToPlayer(player, new ClearNarrationPacket());
        BlockPos root = state != null ? state.chainRoot : BlockPos.ZERO;
        NetworkHelper.sendToAll(new InteractiveChainSwingStatePacket(root, Vec3.ZERO, Vec3.ZERO, false, player.level().getGameTime()));
        if (state != null) {
            NetworkHelper.sendToAll(new InteractiveChainAttachPacket(player.getUUID(), state.chainRoot, state.anchor, state.length, false));
        }
        player.setNoGravity(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
        player.getPersistentData().remove(HANG_TAG);
    }

    public static void setSwingInput(ServerPlayer player, int direction) {
        HangState state = activeHangs.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (direction == 2) {
            stopHang(player);
            activeHangs.remove(player.getUUID());
            return;
        }
        if (direction > 1) {
            direction = 1;
        } else if (direction < -1) {
            direction = -1;
        }
        state.inputDirection = direction;
        state.lastInputTick = player.level().getGameTime();
    }

    private static void syncPlayerPosition(ServerPlayer player, Vec3 pos, float yaw, float pitch, boolean teleport) {
        player.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
        if (teleport) {
            tryTeleport(player, pos, yaw, pitch);
        }
    }

    private static void tryTeleport(ServerPlayer player, Vec3 pos, float yaw, float pitch) {
        try {
            Object connection = player.connection;
            if (connection == null) {
                return;
            }
            var method = resolveTeleportMethod(connection);
            if (method == null) {
                return;
            }
            int paramCount = method.getParameterCount();
            if (paramCount == 5) {
                method.invoke(connection, pos.x, pos.y, pos.z, yaw, pitch);
            } else if (paramCount == 6) {
                method.invoke(connection, pos.x, pos.y, pos.z, yaw, pitch, java.util.Collections.emptySet());
            } else if (paramCount == 7) {
                method.invoke(connection, pos.x, pos.y, pos.z, yaw, pitch, java.util.Collections.emptySet(), false);
            }
        } catch (Exception ignored) {
        }
    }

    private static java.lang.reflect.Method cachedTeleportMethod = null;
    private static boolean teleportResolved = false;

    private static java.lang.reflect.Method resolveTeleportMethod(Object connection) {
        if (teleportResolved) {
            return cachedTeleportMethod;
        }
        teleportResolved = true;
        for (java.lang.reflect.Method m : connection.getClass().getMethods()) {
            if (!m.getName().equals("teleport")) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 5 &&
                params[0] == double.class &&
                params[1] == double.class &&
                params[2] == double.class &&
                params[3] == float.class &&
                params[4] == float.class) {
                m.setAccessible(true);
                cachedTeleportMethod = m;
                return m;
            }
            if (params.length == 6 &&
                params[0] == double.class &&
                params[1] == double.class &&
                params[2] == double.class &&
                params[3] == float.class &&
                params[4] == float.class &&
                java.util.Set.class.isAssignableFrom(params[5])) {
                m.setAccessible(true);
                cachedTeleportMethod = m;
                return m;
            }
            if (params.length == 7 &&
                params[0] == double.class &&
                params[1] == double.class &&
                params[2] == double.class &&
                params[3] == float.class &&
                params[4] == float.class &&
                java.util.Set.class.isAssignableFrom(params[5]) &&
                params[6] == boolean.class) {
                m.setAccessible(true);
                cachedTeleportMethod = m;
                return m;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        resetChainState(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        resetChainState(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        resetChainState(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        resetChainState(player);
    }

    private static void resetChainState(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (player.isSpectator()) {
            player.getPersistentData().remove(HANG_TAG);
            return;
        }
        if (player.isNoGravity() || player.getPersistentData().getBoolean(HANG_TAG)) {
            stopHang(player);
        }
        activeHangs.remove(playerId);
        activeJumps.remove(playerId);
    }
}


