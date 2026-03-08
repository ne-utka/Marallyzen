package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;

import java.lang.reflect.Method;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class MirrorTeleportClient {
    private static final int DURATION_TICKS = 30; // 1.5s at 20 TPS
    private static final int COOLDOWN_TICKS = 4;
    private static final float FOV_BOOST = 14.0f;
    private static final boolean DISABLE_MIRROR_FOV_SHAKE = true;
    private static final float FLASH_ALPHA = 0.22f;
    private static final boolean LOG_TIMINGS = true;
    private static final ResourceLocation EFFECT_ID =
        ResourceLocation.fromNamespaceAndPath("minecraft", "invert");
    private static final ResourceLocation SOUND_START =
        ResourceLocation.fromNamespaceAndPath("minecraft", "block.respawn_anchor.charge");
    private static final ResourceLocation SOUND_PEAK =
        ResourceLocation.fromNamespaceAndPath("minecraft", "entity.enderman.teleport");

    private static boolean running = false;
    private static boolean peakSoundPlayed = false;
    private static boolean prewarmed = false;
    private static boolean reverseLogged = false;
    private static boolean finishedPhaseLogged = false;
    private static int tick = 0;
    private static int cooldown = 0;
    private static float baseFov = 70.0f;
    private static long effectStartNanos = 0L;

    private static boolean postEffectMethodsResolved = false;
    private static Method setPostEffectMethod = null;
    private static Method clearPostEffectMethod = null;

    private MirrorTeleportClient() {
    }

    public static boolean isRunning() {
        return running;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickMirror(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) {
            return;
        }
        if (event.getButton() != 1 || event.getAction() != 1) {
            return;
        }
        if (running || cooldown > 0) {
            return;
        }

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
        if (state.getBlock() != MarallyzenBlocks.MIRROR.get()) {
            return;
        }

        start(mc);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (cooldown > 0) {
            cooldown--;
        }

        if (!prewarmed) {
            tryPrewarm(mc);
        }

        if (!running) {
            return;
        }
        if (mc.level == null || mc.player == null || mc.options == null) {
            stop(mc);
            return;
        }

        tick++;
        float progress = Mth.clamp(tick / (float) DURATION_TICKS, 0.0f, 1.0f);
        logPhaseProgress(progress, mc);

        if (!DISABLE_MIRROR_FOV_SHAKE) {
            float pulse = Mth.sin(progress * (float) Math.PI);
            float targetFov = baseFov + pulse * FOV_BOOST;
            setFov(mc, targetFov);
        }

        if (!peakSoundPlayed && progress >= 0.72f) {
            peakSoundPlayed = true;
            playSound(mc, SOUND_PEAK, 0.8f, 1.06f);
        }

        if (tick >= DURATION_TICKS) {
            stop(mc);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!running) {
            return;
        }

        float progress = Mth.clamp(tick / (float) DURATION_TICKS, 0.0f, 1.0f);
        float flash = flashPulse(progress, 0.08f, 0.08f);
        int alpha = Mth.clamp((int) (flash * FLASH_ALPHA * 255.0f), 0, 255);
        if (alpha <= 0) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), (alpha << 24) | 0x00FFFFFF);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        stop(Minecraft.getInstance());
        prewarmed = false;
    }

    private static void start(Minecraft mc) {
        running = true;
        peakSoundPlayed = false;
        reverseLogged = false;
        finishedPhaseLogged = false;
        tick = 0;
        baseFov = getFov(mc);
        effectStartNanos = System.nanoTime();
        log("START durationTicks={} durationMs={} baseFov={}", DURATION_TICKS, ticksToMs(DURATION_TICKS), String.format("%.2f", baseFov));
        applyPostEffect(mc);
        playSound(mc, SOUND_START, 0.75f, 0.92f);
    }

    private static void stop(Minecraft mc) {
        if (!running && cooldown <= 0) {
            return;
        }

        running = false;
        peakSoundPlayed = false;
        tick = 0;
        cooldown = COOLDOWN_TICKS;

        if (mc != null && mc.options != null) {
            setFov(mc, baseFov);
        }
        clearPostEffect(mc);
        long elapsedMs = nanosToMs(System.nanoTime() - effectStartNanos);
        log("STOP elapsedMs={} expectedMs={} cooldownTicks={}", elapsedMs, ticksToMs(DURATION_TICKS), COOLDOWN_TICKS);
    }

    private static void applyPostEffect(Minecraft mc) {
        if (mc == null || mc.gameRenderer == null) {
            return;
        }
        resolvePostEffectMethods();
        if (setPostEffectMethod == null) {
            return;
        }

        try {
            long t0 = System.nanoTime();
            setPostEffectMethod.invoke(mc.gameRenderer, EFFECT_ID);
            log("POST_EFFECT_APPLY id={} callMs={}", EFFECT_ID, nanosToMs(System.nanoTime() - t0));
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("MirrorTeleport: failed to apply post effect", e);
        }
    }

    private static void clearPostEffect(Minecraft mc) {
        if (mc == null || mc.gameRenderer == null) {
            return;
        }
        resolvePostEffectMethods();
        try {
            long t0 = System.nanoTime();
            if (clearPostEffectMethod != null) {
                clearPostEffectMethod.invoke(mc.gameRenderer);
                log("POST_EFFECT_CLEAR method={} callMs={}", clearPostEffectMethod.getName(), nanosToMs(System.nanoTime() - t0));
                return;
            }
            if (setPostEffectMethod != null) {
                setPostEffectMethod.invoke(mc.gameRenderer, new Object[]{null});
                log("POST_EFFECT_CLEAR method={} callMs={}", setPostEffectMethod.getName(), nanosToMs(System.nanoTime() - t0));
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("MirrorTeleport: failed to clear post effect", e);
        }
    }

    private static void resolvePostEffectMethods() {
        if (postEffectMethodsResolved) {
            return;
        }
        postEffectMethodsResolved = true;

        for (Method method : GameRenderer.class.getMethods()) {
            String name = method.getName();
            if (method.getParameterCount() == 1
                && method.getParameterTypes()[0] == ResourceLocation.class
                && ("setPostEffect".equals(name) || "loadEffect".equals(name))) {
                setPostEffectMethod = method;
            } else if (method.getParameterCount() == 0
                && ("shutdownEffect".equals(name) || "clearPostEffect".equals(name))) {
                clearPostEffectMethod = method;
            }
        }
    }

    private static float getFov(Minecraft mc) {
        if (mc == null || mc.options == null) {
            return 70.0f;
        }
        Object raw = mc.options.fov().get();
        if (raw instanceof Number n) {
            return n.floatValue();
        }
        return 70.0f;
    }

    private static void setFov(Minecraft mc, float value) {
        if (mc == null || mc.options == null) {
            return;
        }
        int clamped = Mth.clamp(Math.round(value), 30, 110);
        mc.options.fov().set(clamped);
    }

    private static void playSound(Minecraft mc, ResourceLocation id, float volume, float pitch) {
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null);
        if (sound == null) {
            return;
        }
        mc.level.playLocalSound(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            sound,
            SoundSource.PLAYERS,
            volume,
            pitch,
            false
        );
    }

    private static void tryPrewarm(Minecraft mc) {
        if (mc == null || mc.level == null || mc.gameRenderer == null || mc.player == null || running) {
            return;
        }
        if (mc.screen != null) {
            return;
        }
        prewarmed = true;
        long t0 = System.nanoTime();
        applyPostEffect(mc);
        clearPostEffect(mc);
        log("PREWARM totalMs={}", nanosToMs(System.nanoTime() - t0));
    }

    private static float flashPulse(float progress, float center, float halfWidth) {
        float dist = Math.abs(progress - center);
        if (dist >= halfWidth) {
            return 0.0f;
        }
        float x = 1.0f - (dist / halfWidth);
        return x * x * (3.0f - 2.0f * x); // smoothstep
    }

    private static void logPhaseProgress(float progress, Minecraft mc) {
        if (!LOG_TIMINGS) {
            return;
        }
        if (!reverseLogged && progress >= 0.50f) {
            reverseLogged = true;
            log("PHASE_REVERSE_START tick={} progress={} fovNow={}", tick, fmt(progress), fmt(getFov(mc)));
        }
        if (!finishedPhaseLogged && progress >= 0.97f) {
            finishedPhaseLogged = true;
            log("PHASE_FINISHING tick={} progress={}", tick, fmt(progress));
        }
    }

    private static long nanosToMs(long nanos) {
        return nanos / 1_000_000L;
    }

    private static long ticksToMs(int ticks) {
        return ticks * 50L;
    }

    private static String fmt(float v) {
        return String.format("%.3f", v);
    }

    private static void log(String template, Object... args) {
        if (!LOG_TIMINGS) {
            return;
        }
        Marallyzen.LOGGER.info("[MirrorTeleportTiming] " + template, args);
    }
}
