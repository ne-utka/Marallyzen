package neutka.marallys.marallyzen.client.chain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.InteractiveChainJumpPacket;
import neutka.marallys.marallyzen.network.InteractiveChainSwingPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.util.UUID;

/**
 * Client-only high-rate interpolation for the local chain jump.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class InteractiveChainClientAnimator {
    private static final double SWING_GRAVITY = 0.08;
    private static final double SWING_INPUT = 0.04;
    private static final double SWING_DAMPING = 0.985;
    private static final double SWING_MAX_SPEED = 0.35;
    private static final double HANG_BACK_OFFSET = 0.25;
    private static final double CHEST_HEIGHT_OFFSET = 0.4;
    private static final int SWING_INPUT_RESEND_TICKS = 2;
    private static final int DISMOUNT_INPUT = 2;

    private static JumpState activeJump;
    private static SwingState activeSwing;
    private static int lastSentSwingInput;
    private static int swingResendCooldown;
    private static boolean mouseLeftDown;
    private static boolean mouseRightDown;
    private static java.lang.reflect.Method cachedGetAnimator;
    private static java.lang.reflect.Method cachedSetTickDelta;
    private static boolean animatorLookupFailed;

    private InteractiveChainClientAnimator() {}

    private static final class JumpState {
        private final UUID playerId;
        private final Vec3 start;
        private final Vec3 control;
        private final Vec3 end;
        private final float startYaw;
        private final float startPitch;
        private final float endYaw;
        private final float endPitch;
        private final long startTick;
        private final int durationTicks;

        private JumpState(UUID playerId, Vec3 start, Vec3 control, Vec3 end,
                          float startYaw, float startPitch, float endYaw, float endPitch,
                          long startTick, int durationTicks) {
            this.playerId = playerId;
            this.start = start;
            this.control = control;
            this.end = end;
            this.startYaw = startYaw;
            this.startPitch = startPitch;
            this.endYaw = endYaw;
            this.endPitch = endPitch;
            this.startTick = startTick;
            this.durationTicks = durationTicks;
        }
    }

    private static final class SwingState {
        private final Vec3 anchor;
        private final double length;
        private Vec3 offset;
        private Vec3 velocity;
        private Vec3 prevPos;
        private Vec3 currPos;

        private SwingState(Vec3 anchor, double length, Vec3 offset) {
            this.anchor = anchor;
            this.length = length;
            this.offset = offset;
            this.velocity = Vec3.ZERO;
        this.prevPos = anchor.add(offset);
        this.currPos = anchor.add(offset);
        }
    }

    public static void start(InteractiveChainJumpPacket packet) {
        if (packet == null || packet.durationTicks() <= 0 || packet.playerId() == null) {
            return;
        }
        activeJump = new JumpState(
                packet.playerId(),
                packet.start(),
                packet.control(),
                packet.end(),
                packet.startYaw(),
                packet.startPitch(),
                packet.endYaw(),
                packet.endPitch(),
                packet.startTick(),
                packet.durationTicks()
        );
    }

    public static void clear() {
        activeJump = null;
    }

    public static void clearAll() {
        activeJump = null;
        stopSwing();
    }

    public static void startSwing(Vec3 anchor, double length) {
        if (anchor == null) {
            return;
        }
        double resolvedLength = length > 0.0 ? length : 1.3;
        Vec3 offset = new Vec3(0.0, -resolvedLength, 0.0);
        activeSwing = new SwingState(anchor, resolvedLength, offset);
        lastSentSwingInput = 0;
        swingResendCooldown = 0;
    }

    public static void stopSwing() {
        activeSwing = null;
        lastSentSwingInput = 0;
        swingResendCooldown = 0;
        mouseLeftDown = false;
        mouseRightDown = false;
    }

    public static void setMouseLeftDown(boolean leftDown) {
        mouseLeftDown = leftDown;
    }

    public static void setMouseRightDown(boolean rightDown) {
        mouseRightDown = rightDown;
    }

    public static boolean isSwinging() {
        return activeSwing != null;
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        renderAtPartial(event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SwingState state = activeSwing;
        if (state == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stopSwing();
            return;
        }

        if (mc.options.keyJump.isDown()) {
            sendSwingInput(DISMOUNT_INPUT);
            return;
        }

        int input = resolveSwingInput(mc);
        sendSwingInput(input);
        tickSwing(mc.player, state, input);
    }

    private static void renderAtPartial(float partial) {
        JumpState state = activeJump;
        if (state == null) {
            renderSwingAtPartial(partial);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }

        LocalPlayer player = mc.player;
        if (!player.getUUID().equals(state.playerId)) {
            return;
        }

        updateEmoteTickDelta(player, partial);

        long gameTime = mc.level.getGameTime();
        float elapsed = (gameTime - state.startTick) + partial;
        if (elapsed < 0.0f) {
            return;
        }

        float t = elapsed / (float) state.durationTicks;
        if (t >= 1.0f) {
            clear();
            return;
        }

        float eased = easeInOut(Mth.clamp(t, 0.0f, 1.0f));
        Vec3 pos = quadraticBezier(state.start, state.control, state.end, eased);
        float yaw = Mth.rotLerp(eased, state.startYaw, state.endYaw);
        float pitch = Mth.lerp(eased, state.startPitch, state.endPitch);

        applyLocalTransform(player, pos, yaw, pitch);
    }

    private static void renderSwingAtPartial(float partial) {
        SwingState state = activeSwing;
        if (state == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopSwing();
            return;
        }

        Vec3 pos = lerpVec(partial, state.prevPos, state.currPos);
        LocalPlayer player = mc.player;
        updateEmoteTickDelta(player, partial);
        applyLocalTransform(player, pos, player.getYRot(), player.getXRot());
    }

    private static void applyLocalTransform(LocalPlayer player, Vec3 pos, float yaw, float pitch) {
        player.setPos(pos.x, pos.y, pos.z);
        player.xo = pos.x;
        player.yo = pos.y;
        player.zo = pos.z;
        player.xOld = pos.x;
        player.yOld = pos.y;
        player.zOld = pos.z;
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
    }

    private static void tickSwing(LocalPlayer player, SwingState state, int input) {
        state.prevPos = state.currPos;

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

        state.offset = nextOffset;
        state.velocity = velocity;
        double chestOffset = Math.max(0.8, player.getEyeHeight() - CHEST_HEIGHT_OFFSET);
        state.currPos = computeHangPos(state.anchor, nextOffset, chestOffset);
    }

    private static Vec3 computeHangPos(Vec3 anchor, Vec3 offset, double chestOffset) {
        Vec3 backOffset = computeBackOffset(offset);
        return anchor.add(offset).subtract(0.0, chestOffset, 0.0).add(backOffset);
    }

    private static Vec3 computeBackOffset(Vec3 offset) {
        Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }
        return horizontal.normalize().scale(-HANG_BACK_OFFSET);
    }

    private static int resolveSwingInput(Minecraft mc) {
        if (mc.screen != null) {
            return 0;
        }
        boolean forward = mc.options.keyUp.isDown() || mc.options.keyAttack.isDown() || mouseLeftDown;
        boolean backward = mc.options.keyDown.isDown() || mc.options.keyUse.isDown() || mouseRightDown;
        if (forward == backward) {
            return 0;
        }
        return forward ? 1 : -1;
    }

    private static void sendSwingInput(int input) {
        if (input == DISMOUNT_INPUT) {
            NetworkHelper.sendToServer(new InteractiveChainSwingPacket((byte) input));
            lastSentSwingInput = input;
            swingResendCooldown = 0;
            return;
        }
        if (input != lastSentSwingInput || swingResendCooldown <= 0) {
            NetworkHelper.sendToServer(new InteractiveChainSwingPacket((byte) input));
            lastSentSwingInput = input;
            swingResendCooldown = SWING_INPUT_RESEND_TICKS;
        } else {
            swingResendCooldown--;
        }
    }

    private static Vec3 getHorizontalForward(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return horizontal.normalize();
    }

    private static Vec3 lerpVec(float t, Vec3 from, Vec3 to) {
        if (from == null || to == null) {
            return to != null ? to : Vec3.ZERO;
        }
        return new Vec3(
                Mth.lerp(t, from.x, to.x),
                Mth.lerp(t, from.y, to.y),
                Mth.lerp(t, from.z, to.z)
        );
    }

    private static void updateEmoteTickDelta(LocalPlayer player, float partial) {
        Object applier = resolveAnimationApplier(player);
        if (applier == null) {
            return;
        }
        try {
            if (cachedSetTickDelta == null || cachedSetTickDelta.getDeclaringClass() != applier.getClass()) {
                cachedSetTickDelta = applier.getClass().getMethod("setTickDelta", float.class);
                cachedSetTickDelta.setAccessible(true);
            }
            cachedSetTickDelta.invoke(applier, partial);
        } catch (Exception ignored) {
        }
    }

    private static Object resolveAnimationApplier(LocalPlayer player) {
        if (animatorLookupFailed) {
            return null;
        }
        try {
            if (cachedGetAnimator == null) {
                cachedGetAnimator = player.getClass().getMethod("playerAnimator_getAnimation");
                cachedGetAnimator.setAccessible(true);
            }
            return cachedGetAnimator.invoke(player);
        } catch (Exception ignored) {
            animatorLookupFailed = true;
            return null;
        }
    }

    private static float easeInOut(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static Vec3 quadraticBezier(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        float u = 1.0f - t;
        return p0.scale(u * u).add(p1.scale(2.0f * u * t)).add(p2.scale(t * t));
    }
}
