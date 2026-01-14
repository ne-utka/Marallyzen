package neutka.marallys.marallyzen.client.chain;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.emote.ClientEmoteHandler;
import neutka.marallys.marallyzen.client.fpv.FpvEmoteInvoker;
import neutka.marallys.marallyzen.client.fpv.MarallyzenFpvController;
import neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext;
import neutka.marallys.marallyzen.network.InteractiveChainAttachPacket;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class InteractiveChainPoseManager {
    private static final Map<UUID, ChainAttachment> ATTACHMENTS = new ConcurrentHashMap<>();
    private static final long RELEASE_FADE_MS = 250L;
    private static final long EMOTE_REFRESH_MS = 700L;
    private static final ResourceLocation BASE_EMOTE_RL =
            ResourceLocation.fromNamespaceAndPath("marallyzen", "chain_hang_base");
    private static final String BASE_EMOTE_ID = "chain_hang_base";
    private static final double WIND_PERIOD_SEC = 6.0;
    private static final double WIND_ANGLE_X_DEG = 0.9;
    private static final double WIND_ANGLE_Z_DEG = 0.36;
    private static final double WIND_PHASE_OFFSET_RAD = Math.toRadians(60.0);

    private InteractiveChainPoseManager() {}

    public record ChainPose(float chainAngle, float chainSideAngle, float chainVelocity, float fade) {}

    private static final class ChainAttachment {
        private final UUID playerId;
        private BlockPos chainRoot;
        private Vec3 anchor;
        private double length;
        private long releaseStartMs = -1L;
        private Vec3 releaseOffset;
        private Vec3 lastOffset;
        private long lastEmoteRefreshMs = 0L;
        private boolean emoteStarted = false;

        private ChainAttachment(UUID playerId, BlockPos chainRoot, Vec3 anchor, double length) {
            this.playerId = playerId;
            this.chainRoot = chainRoot;
            this.anchor = anchor;
            this.length = length;
        }
    }

    public static void handleAttach(InteractiveChainAttachPacket packet) {
        if (packet == null || packet.playerId() == null) {
            return;
        }
        if (packet.attached()) {
            attach(packet.playerId(), packet.chainRoot(), packet.anchor(), packet.length());
        } else {
            detach(packet.playerId(), Util.getMillis());
        }
    }

    private static void attach(UUID playerId, BlockPos chainRoot, Vec3 anchor, double length) {
        ChainAttachment attachment = ATTACHMENTS.computeIfAbsent(
                playerId,
                id -> new ChainAttachment(id, chainRoot, anchor, length)
        );
        attachment.chainRoot = chainRoot;
        attachment.anchor = anchor;
        attachment.length = length;
        attachment.releaseStartMs = -1L;
        attachment.releaseOffset = null;
        attachment.emoteStarted = false;

        AbstractClientPlayer player = resolvePlayer(playerId);
        if (player == null) {
            return;
        }

        startBaseEmote(player, attachment);
    }

    private static void detach(UUID playerId, long nowMillis) {
        ChainAttachment attachment = ATTACHMENTS.get(playerId);
        if (attachment == null) {
            return;
        }
        if (attachment.releaseStartMs > 0L) {
            return;
        }
        Vec3 lastOffset = attachment.lastOffset;
        if (lastOffset == null) {
            lastOffset = new Vec3(0.0, -Math.max(0.5, attachment.length), 0.0);
        }
        attachment.releaseOffset = lastOffset;
        attachment.releaseStartMs = nowMillis;
    }

    public static ChainPose getPose(AbstractClientPlayer player, long nowMillis) {
        if (player == null) {
            return null;
        }
        ChainAttachment attachment = ATTACHMENTS.get(player.getUUID());
        if (attachment == null) {
            return null;
        }

        boolean releasing = attachment.releaseStartMs > 0L;
        float fade = 1.0f;
        Vec3 offset;

        if (releasing) {
            float t = (float) (nowMillis - attachment.releaseStartMs) / (float) RELEASE_FADE_MS;
            fade = 1.0f - Mth.clamp(t, 0.0f, 1.0f);
            if (fade <= 0.0f) {
                return null;
            }
            offset = attachment.releaseOffset;
        } else {
            offset = InteractiveChainSwingVisuals.getInterpolatedOffset(attachment.chainRoot, nowMillis);
        }

        if (offset == null || offset.lengthSqr() < 1.0E-6
                || (Math.abs(offset.x) < 0.02 && Math.abs(offset.z) < 0.02)) {
            offset = idleWindOffset(attachment.length, nowMillis);
        }

        attachment.lastOffset = offset;

        Vec3 local = toLocal(offset, player.yBodyRot);
        float chainAngle = (float) Math.atan2(local.z, -local.y);
        float chainSideAngle = (float) Math.atan2(local.x, -local.y);
        chainAngle = Mth.clamp(chainAngle, -1.2f, 1.2f);
        chainSideAngle = Mth.clamp(chainSideAngle, -1.2f, 1.2f);

        Vec3 velocity = releasing ? Vec3.ZERO : InteractiveChainSwingVisuals.getVelocity(attachment.chainRoot);
        double length = Math.max(offset.length(), 1.0E-4);
        double speedPerSec = velocity.length() * 1000.0;
        float chainVelocity = (float) (speedPerSec / length);
        chainVelocity = Mth.clamp(chainVelocity, 0.0f, 4.0f);

        return new ChainPose(chainAngle, chainSideAngle, chainVelocity, fade);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        long nowMillis = Util.getMillis();
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer localPlayer = mc.player;

        if (localPlayer != null) {
            ChainAttachment attachment = ATTACHMENTS.get(localPlayer.getUUID());
            if (attachment != null && attachment.releaseStartMs < 0L) {
                if (nowMillis - attachment.lastEmoteRefreshMs >= EMOTE_REFRESH_MS) {
                    ResourceLocation current = MarallyzenFpvController.getResolvedEmoteId(localPlayer);
                    if (current == null || !current.equals(BASE_EMOTE_RL)) {
                        MarallyzenRenderContext.setFpvEmoteEnabled(true);
                        FpvEmoteInvoker.play(localPlayer, BASE_EMOTE_ID);
                    }
                    attachment.lastEmoteRefreshMs = nowMillis;
                }
            }
        }

        ATTACHMENTS.entrySet().removeIf(entry -> {
            ChainAttachment attachment = entry.getValue();
            if (attachment == null || attachment.releaseStartMs < 0L) {
                if (attachment != null && !attachment.emoteStarted) {
                    AbstractClientPlayer player = resolvePlayer(entry.getKey());
                    if (player != null) {
                        startBaseEmote(player, attachment);
                    }
                }
                return false;
            }
            long elapsed = nowMillis - attachment.releaseStartMs;
            if (elapsed < RELEASE_FADE_MS) {
                return false;
            }
            AbstractClientPlayer player = resolvePlayer(entry.getKey());
            if (player != null) {
                stopBaseEmote(player);
            }
            return true;
        });
    }

    private static AbstractClientPlayer resolvePlayer(UUID playerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        net.minecraft.world.entity.player.Player player = mc.level.getPlayerByUUID(playerId);
        if (player instanceof AbstractClientPlayer clientPlayer) {
            return clientPlayer;
        }
        return null;
    }

    private static boolean isLocalPlayer(AbstractClientPlayer player) {
        return Minecraft.getInstance().player == player;
    }

    private static Vec3 toLocal(Vec3 world, float yawDeg) {
        float yawRad = (float) Math.toRadians(yawDeg);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double x = world.x * cos + world.z * sin;
        double z = -world.x * sin + world.z * cos;
        return new Vec3(x, world.y, z);
    }

    private static Vec3 idleWindOffset(double length, long nowMillis) {
        double clampedLength = Math.max(0.5, length);
        double t = (nowMillis / 1000.0) % WIND_PERIOD_SEC;
        double phase = (t / WIND_PERIOD_SEC) * (Math.PI * 2.0);
        double thetaX = Math.sin(phase) * Math.toRadians(WIND_ANGLE_X_DEG);
        double thetaZ = Math.sin(phase + WIND_PHASE_OFFSET_RAD) * Math.toRadians(WIND_ANGLE_Z_DEG);
        double swingX = Math.sin(thetaZ) * clampedLength;
        double swingZ = Math.sin(thetaX) * clampedLength;
        double swingY = -Math.cos(thetaX) * Math.cos(thetaZ) * clampedLength;
        return new Vec3(swingX, swingY, swingZ);
    }

    private static void startBaseEmote(AbstractClientPlayer player, ChainAttachment attachment) {
        if (player == null || attachment == null) {
            return;
        }
        if (isLocalPlayer(player)) {
            MarallyzenRenderContext.setFpvEmoteEnabled(true);
            FpvEmoteInvoker.play(player, BASE_EMOTE_ID);
            attachment.lastEmoteRefreshMs = Util.getMillis();
        } else {
            ClientEmoteHandler.handle(player.getUUID(), BASE_EMOTE_ID);
        }
        attachment.emoteStarted = true;
    }

    private static void stopBaseEmote(AbstractClientPlayer player) {
        if (player == null) {
            return;
        }
        ResourceLocation current = MarallyzenFpvController.getResolvedEmoteId(player);
        if (current != null && !current.equals(BASE_EMOTE_RL)) {
            if (isLocalPlayer(player)) {
                MarallyzenRenderContext.setFpvEmoteEnabled(false);
            }
            return;
        }

        try {
            Method getEmote = player.getClass().getMethod("emotecraft$getEmote");
            getEmote.setAccessible(true);
            Object emotePlayer = getEmote.invoke(player);
            if (emotePlayer != null) {
                invokeStop(emotePlayer);
            }
        } catch (Exception ignored) {
        }

        if (isLocalPlayer(player)) {
            MarallyzenRenderContext.setFpvEmoteEnabled(false);
        }
    }

    private static void invokeStop(Object emotePlayer) {
        Method[] methods = emotePlayer.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (!name.equals("stopEmote") && !name.equals("emotecraft$stopEmote") && !name.equals("stop")) {
                continue;
            }
            try {
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    method.invoke(emotePlayer);
                    return;
                }
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == boolean.class) {
                    method.setAccessible(true);
                    method.invoke(emotePlayer, true);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }
}
