package neutka.marallys.marallyzen.mirror;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class MirrorTeleportClient {
    private static boolean active;
    private static int elapsedMs;
    private static int negativeTimeMs;
    private static int whiteFadeInMs;
    private static int whiteFadeOutMs;
    private static boolean hideGuiCaptured;
    private static boolean previousHideGui;

    private MirrorTeleportClient() {
    }

    public static void start(MirrorTeleportPacket packet) {
        if (packet == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }
        if (!hideGuiCaptured) {
            previousHideGui = mc.options.hideGui;
            hideGuiCaptured = true;
        }
        mc.options.hideGui = true;

        active = true;
        elapsedMs = 0;
        negativeTimeMs = Math.max(0, packet.negativeTimeMs());
        whiteFadeInMs = Math.max(0, packet.fadeInTimeMs());
        whiteFadeOutMs = Math.max(1, packet.fadeOutTimeMs());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            return;
        }
        elapsedMs += 50;
        if (elapsedMs >= totalDurationMs()) {
            stop();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float timelineMs = elapsedMs + partial * 50.0f;

        float negAlpha = 0.0f;
        float whiteAlpha = 0.0f;
        int negEnd = negativeTimeMs;
        int whiteInEnd = negEnd + whiteFadeInMs;

        if (timelineMs <= negEnd && negEnd > 0) {
            // Fast ramp into clear negative, then hold until white phase starts.
            float t = Mth.clamp(timelineMs / (float) negEnd, 0.0f, 1.0f);
            negAlpha = t < 0.35f ? (t / 0.35f) : 1.0f;
        } else if (timelineMs <= whiteInEnd && whiteFadeInMs > 0) {
            float t = (timelineMs - negEnd) / (float) whiteFadeInMs;
            negAlpha = 1.0f - t;
            whiteAlpha = t;
        } else {
            float t = (timelineMs - whiteInEnd) / (float) whiteFadeOutMs;
            whiteAlpha = 1.0f - Mth.clamp(t, 0.0f, 1.0f);
        }

        if (negAlpha > 0.0f) {
            renderNegativePass(guiGraphics, width, height, negAlpha);
        }
        if (whiteAlpha > 0.0f) {
            int a = (int) (Mth.clamp(whiteAlpha, 0.0f, 1.0f) * 255.0f);
            int color = (a << 24) | 0xFFFFFF;
            guiGraphics.fill(0, 0, width, height, color);
        }
    }

    private static void renderNegativePass(GuiGraphics guiGraphics, int width, int height, float alpha) {
        float clamped = Mth.clamp(alpha, 0.0f, 1.0f);

        // Neutral, high-contrast pass (no blue tint), visually distinct from white flash.
        int darkA = (int) (clamped * 180.0f);
        int midA = (int) (clamped * 96.0f);
        guiGraphics.fill(0, 0, width, height, (darkA << 24) | 0x0D0D0D);
        guiGraphics.fill(0, 0, width, height, (midA << 24) | 0xBFBFBF);

        // Light vignette to sell the "negative frame" before white transition.
        int vignetteA = (int) (clamped * 72.0f);
        int edge = Math.max(12, Math.min(width, height) / 14);
        int vignetteColor = (vignetteA << 24);
        guiGraphics.fill(0, 0, width, edge, vignetteColor);
        guiGraphics.fill(0, height - edge, width, height, vignetteColor);
        guiGraphics.fill(0, edge, edge, height - edge, vignetteColor);
        guiGraphics.fill(width - edge, edge, width, height - edge, vignetteColor);
    }

    private static void stop() {
        active = false;
        elapsedMs = 0;
        Minecraft mc = Minecraft.getInstance();
        if (hideGuiCaptured && mc != null && mc.options != null) {
            mc.options.hideGui = previousHideGui;
        }
        hideGuiCaptured = false;
    }

    private static int totalDurationMs() {
        return negativeTimeMs + whiteFadeInMs + whiteFadeOutMs;
    }
}
