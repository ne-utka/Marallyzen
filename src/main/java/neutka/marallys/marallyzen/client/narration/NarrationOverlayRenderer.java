package neutka.marallys.marallyzen.client.narration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.MarallyzenClientConfig;
import neutka.marallys.marallyzen.client.lever.LeverQteClient;
import neutka.marallys.marallyzen.client.quest.QuestJournalScreen;
import neutka.marallys.marallyzen.client.valve.ValveQteClient;

/**
 * Renders the narration overlay as a Bedrock-style semi-transparent panel at the bottom of the screen.
 * Uses RenderGuiEvent.Post to render on top of the game HUD.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class NarrationOverlayRenderer {
    private static final String QUEST_ZONE_COUNTDOWN_PREFIX = "__QZ_COUNTDOWN__:";
    private static final String QUEST_ZONE_LEFT_MESSAGE = "\u0412\u044b \u0432\u044b\u0448\u043b\u0438 \u0438\u0437 \u0437\u043e\u043d\u044b \u043a\u0432\u0435\u0441\u0442\u0430!";
    private static final String DIALOG_NAV_ICON_TOKEN = "__MOUSE_ZOOM_ICON__";
    private static final Identifier DIALOG_NAV_ICON =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/icons/mouse_zoom.png");
    private static final int DIALOG_NAV_ICON_WIDTH = 11;
    private static final int DIALOG_NAV_ICON_HEIGHT = 11;
    private static final int QUEST_ZONE_SEGMENTS = 8;
    private static final int QUEST_ZONE_SEGMENT_WIDTH = 4;
    private static final int QUEST_ZONE_SEGMENT_HEIGHT = 8;
    private static final int QUEST_ZONE_SEGMENT_GAP = 2;
    private static final int QUEST_ZONE_SEGMENT_COLOR = 0xAAB2B9;
    private static final int QUEST_ZONE_SEGMENT_FILLED_COLOR = 0x5EC22A;
    private static final int QUEST_ZONE_SHADOW_COLOR = 0x222832;
    private static final int QUEST_ZONE_SHADOW_OFFSET = 1;
    private static final Identifier QUEST_ZONE_CANCEL_ICON =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/icons/cancel.png");
    private static final Identifier QUEST_ZONE_CONFIRM_ICON =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/icons/confirm_icon.png");
    private static final int QUEST_ZONE_ICON_WIDTH = 7;
    private static final int QUEST_ZONE_ICON_HEIGHT = 8;

    private static final int FADE_TICKS = 8;
    private static float overlayAlpha = 1.0f;
    private static float previousAlpha = 1.0f;
    private static final Identifier ROUNDED_BG_TEXTURE =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/gui/rounded_prompt_bg.png");
    private static final int ROUNDED_BG_TEX_SIZE = 64;
    private static final int ROUNDED_BG_CORNER_PX = 10;
    private static final int ROUNDED_BG_RADIUS_PX = 2;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) {
            return;
        }
        if (LeverQteClient.isHudVisible() || ValveQteClient.isHudVisible()) {
            return;
        }
        boolean blockOverlay = mc.screen instanceof QuestJournalScreen;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        var window = mc.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        // Get partialTick for smooth 60fps interpolation
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        previousAlpha = overlayAlpha;
        float target = blockOverlay ? 0.0f : 1.0f;
        float step = 1.0f / FADE_TICKS;
        if (overlayAlpha < target) {
            overlayAlpha = Math.min(target, overlayAlpha + step);
        } else if (overlayAlpha > target) {
            overlayAlpha = Math.max(target, overlayAlpha - step);
        }
        float overlayMultiplier = Mth.lerp(partialTick, previousAlpha, overlayAlpha);

        NarrationManager manager = NarrationManager.getInstance();

        // Render narration overlay (higher priority - shows above proximity)
        NarrationOverlay narrationOverlay = manager.getActive();
        if (narrationOverlay != null) {
            // Always check isVisible() to prevent rendering when alpha is too low
            if (narrationOverlay.isVisible()) {
                // Interpolate alpha for smooth 60fps animation
                float interpolatedAlpha = Mth.lerp(partialTick, narrationOverlay.getPreviousAlpha(), narrationOverlay.getAlpha());

                // Clamp interpolated alpha to prevent negative values or values > 1.0
                interpolatedAlpha = Mth.clamp(interpolatedAlpha, 0.0f, 1.0f);
                float finalAlpha = interpolatedAlpha * overlayMultiplier;
                if (finalAlpha > 0.01f) {
                    renderOverlay(guiGraphics, mc, width, height, narrationOverlay.getText(), finalAlpha,
                            narrationOverlay.isBackgroundVisible());
                }
            }
        } else {
            // Render proximity overlay (only if narration is not active)
            ProximityOverlay proximityOverlay = manager.getProximity();
            if (proximityOverlay != null && proximityOverlay.isVisible()) {
                // Interpolate alpha for smooth 60fps animation
                float interpolatedAlpha = Mth.lerp(partialTick, proximityOverlay.getPreviousAlpha(), proximityOverlay.getAlpha());

                float finalAlpha = interpolatedAlpha * overlayMultiplier;
                if (finalAlpha > 0.01f) {
                    renderOverlay(guiGraphics, mc, width, height, proximityOverlay.getText(), finalAlpha, true);
                }
            }
        }
    }

    private static void renderOverlay(GuiGraphics guiGraphics, Minecraft mc, int width, int height,
                                      Component text, float alpha, boolean showBackground) {
        if (text == null || alpha <= 0.0f) {
            return;
        }
        if (tryRenderQuestZoneCountdown(guiGraphics, mc, width, height, text, alpha)) {
            return;
        }
        if (tryRenderDialogNavigation(guiGraphics, mc, width, height, text, alpha, showBackground)) {
            return;
        }

        var font = mc.font;

        // Calculate max text width (screen width - margins - padding)
        int maxTextWidth = width - 80 - 4; // 40px margin on each side, 2px padding on each side

        // Split text into lines
        var lines = font.split(text, maxTextWidth);
        if (lines.isEmpty()) {
            return;
        }

        // Calculate text dimensions
        int maxLineWidth = 0;
        for (var line : lines) {
            int lineWidth = font.width(line);
            if (lineWidth > maxLineWidth) {
                maxLineWidth = lineWidth;
            }
        }

        int lineHeight = font.lineHeight;
        int textHeight = lines.size() * lineHeight;

        int paddingX = 5;
        int paddingY = 3;
        int boxWidth = maxLineWidth + paddingX * 2;
        int boxHeight = textHeight + paddingY * 2;

        // Bedrock-style positioning: bottom of screen, centered horizontally
        int x = (width - boxWidth) / 2;
        int y = height - 60; // 60px from bottom (lower on screen)

        // Calculate alpha for background (more transparent - ~47% opacity when fully visible)
        int bgAlpha = (int) (alpha * 120); // ~47% of 255 (was 180 = ~70%)
        int bgColor = (bgAlpha << 24); // ARGB: alpha in top 8 bits, RGB = 0 (black)

        // Draw rounded semi-transparent background (fits text with padding)
        if (showBackground && MarallyzenClientConfig.NARRATION_HUD_BACKGROUND.get()) {
            renderRoundedBackground(guiGraphics, x, y, boxWidth, boxHeight, bgColor);
        }

        // Draw text with alpha
        int textAlpha = (int) (alpha * 255);
        int textColor = 0xFFFFFF | (textAlpha << 24); // White text with alpha
        boolean drawShadow = true;

        // Draw text lines (padding offset from box edges)
        int textX = x + paddingX;
        int textY = y + paddingY;

        for (int i = 0; i < lines.size() && i < 2; i++) { // Max 2 lines
            int lineY = textY + (i * lineHeight);
            guiGraphics.drawString(font, lines.get(i), textX, lineY, textColor, drawShadow);
        }
    }

    private static boolean tryRenderDialogNavigation(GuiGraphics guiGraphics, Minecraft mc, int width, int height,
                                                     Component text, float alpha, boolean showBackground) {
        String value = text.getString();
        if (value == null || !value.contains(DIALOG_NAV_ICON_TOKEN)) {
            return false;
        }

        String label = value.replace(DIALOG_NAV_ICON_TOKEN, "").trim();
        if (label.isEmpty()) {
            return false;
        }

        var font = mc.font;
        int spacing = 4;
        int paddingX = 5;
        int paddingY = 3;
        int contentWidth = font.width(label) + spacing + DIALOG_NAV_ICON_WIDTH;
        int boxWidth = contentWidth + paddingX * 2;
        int boxHeight = Math.max(font.lineHeight, DIALOG_NAV_ICON_HEIGHT) + paddingY * 2;
        int x = (width - boxWidth) / 2;
        int y = height - 60;

        int bgAlpha = (int) (alpha * 120);
        int bgColor = (bgAlpha << 24);
        if (showBackground && MarallyzenClientConfig.NARRATION_HUD_BACKGROUND.get()) {
            renderRoundedBackground(guiGraphics, x, y, boxWidth, boxHeight, bgColor);
        }

        int textAlpha = (int) (Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
        int textColor = 0xFFFFFF | (textAlpha << 24);
        int textX = x + paddingX;
        int textY = y + paddingY + (Math.max(font.lineHeight, DIALOG_NAV_ICON_HEIGHT) - font.lineHeight) / 2;
        guiGraphics.drawString(font, label, textX, textY, textColor, true);

        int iconX = textX + font.width(label) + spacing;
        int iconY = y + paddingY + (Math.max(font.lineHeight, DIALOG_NAV_ICON_HEIGHT) - DIALOG_NAV_ICON_HEIGHT) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DIALOG_NAV_ICON, iconX, iconY,
                0.0f, 0.0f, DIALOG_NAV_ICON_WIDTH, DIALOG_NAV_ICON_HEIGHT,
                DIALOG_NAV_ICON_WIDTH, DIALOG_NAV_ICON_HEIGHT, withAlpha(0xFFFFFF, textAlpha));
        return true;
    }

    private static boolean tryRenderQuestZoneCountdown(GuiGraphics guiGraphics, Minecraft mc, int width, int height,
                                                       Component text, float alpha) {
        String value = text.getString();
        if (value == null || !value.startsWith(QUEST_ZONE_COUNTDOWN_PREFIX)) {
            return false;
        }
        int filled = parseCountdownFilled(value.substring(QUEST_ZONE_COUNTDOWN_PREFIX.length()));
        if (filled < 0) {
            return false;
        }
        int textAlpha = (int) (Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
        int shadowColor = withAlpha(QUEST_ZONE_SHADOW_COLOR, textAlpha);

        int segmentsWidth = QUEST_ZONE_SEGMENTS * QUEST_ZONE_SEGMENT_WIDTH
                + (QUEST_ZONE_SEGMENTS - 1) * QUEST_ZONE_SEGMENT_GAP;
        int spacing = 6;
        int contentWidth = QUEST_ZONE_ICON_WIDTH + spacing + segmentsWidth + spacing + QUEST_ZONE_ICON_WIDTH;
        int x = (width - contentWidth) / 2;
        int y = height - 60;

        int iconY = y + (QUEST_ZONE_SEGMENT_HEIGHT - QUEST_ZONE_ICON_HEIGHT) / 2;
        int cursor = x;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, QUEST_ZONE_CANCEL_ICON, cursor, iconY,
                0.0f, 0.0f, QUEST_ZONE_ICON_WIDTH, QUEST_ZONE_ICON_HEIGHT,
                QUEST_ZONE_ICON_WIDTH, QUEST_ZONE_ICON_HEIGHT, withAlpha(0xFFFFFF, textAlpha));
        cursor += QUEST_ZONE_ICON_WIDTH + spacing;

        for (int i = 0; i < QUEST_ZONE_SEGMENTS; i++) {
            int boxX = cursor + i * (QUEST_ZONE_SEGMENT_WIDTH + QUEST_ZONE_SEGMENT_GAP);
            int fillColor = withAlpha(i < filled ? QUEST_ZONE_SEGMENT_FILLED_COLOR : QUEST_ZONE_SEGMENT_COLOR, textAlpha);
            guiGraphics.fill(boxX + QUEST_ZONE_SHADOW_OFFSET, y + QUEST_ZONE_SHADOW_OFFSET,
                    boxX + QUEST_ZONE_SEGMENT_WIDTH + QUEST_ZONE_SHADOW_OFFSET,
                    y + QUEST_ZONE_SEGMENT_HEIGHT + QUEST_ZONE_SHADOW_OFFSET, shadowColor);
            guiGraphics.fill(boxX, y, boxX + QUEST_ZONE_SEGMENT_WIDTH, y + QUEST_ZONE_SEGMENT_HEIGHT, fillColor);
        }
        cursor += segmentsWidth + spacing;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, QUEST_ZONE_CONFIRM_ICON, cursor, iconY,
                0.0f, 0.0f, QUEST_ZONE_ICON_WIDTH, QUEST_ZONE_ICON_HEIGHT,
                QUEST_ZONE_ICON_WIDTH, QUEST_ZONE_ICON_HEIGHT, withAlpha(0xFFFFFF, textAlpha));
        return true;
    }

    private static int parseCountdownFilled(String rawValue) {
        try {
            return Mth.clamp(Integer.parseInt(rawValue.trim()), 0, QUEST_ZONE_SEGMENTS);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return ARGB.color(alpha, ARGB.red(rgb), ARGB.green(rgb), ARGB.blue(rgb));
    }

    private static void renderRoundedBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float corner = Math.min(Math.max(ROUNDED_BG_RADIUS_PX, 0.0f), Math.min(width, height) / 2.0f);
        if (corner <= 0.0f) {
            guiGraphics.fill(x, y, x + width, y + height, color);
            return;
        }
        fillTexturedRect(guiGraphics, x, y, width, height, Math.round(corner), color);
    }

    private static void fillTexturedRect(GuiGraphics guiGraphics,
                                         int x, int y, int width, int height, int corner, int color) {
        float uCorner = (float) ROUNDED_BG_CORNER_PX / (float) ROUNDED_BG_TEX_SIZE;
        int x0 = x;
        int x1 = x + corner;
        int x2 = x + width - corner;
        int x3 = x + width;
        int y0 = y;
        int y1 = y + corner;
        int y2 = y + height - corner;
        int y3 = y + height;
        // Avoid overlap to prevent dark seams from double-blending.
        int overlap = 0;
        blitSlice(guiGraphics, x0, y0, corner + overlap, corner + overlap, 0.0f, 0.0f, uCorner, uCorner, color);
        blitSlice(guiGraphics, x1, y0, (x2 - x1) + overlap, corner + overlap, uCorner, 0.0f, 1.0f - uCorner, uCorner, color);
        blitSlice(guiGraphics, x2 - overlap, y0, (x3 - x2) + overlap, corner + overlap, 1.0f - uCorner, 0.0f, 1.0f, uCorner, color);
        blitSlice(guiGraphics, x0, y1, corner + overlap, (y2 - y1) + overlap, 0.0f, uCorner, uCorner, 1.0f - uCorner, color);
        blitSlice(guiGraphics, x1, y1, (x2 - x1) + overlap, (y2 - y1) + overlap, uCorner, uCorner, 1.0f - uCorner, 1.0f - uCorner, color);
        blitSlice(guiGraphics, x2 - overlap, y1, (x3 - x2) + overlap, (y2 - y1) + overlap, 1.0f - uCorner, uCorner, 1.0f, 1.0f - uCorner, color);
        blitSlice(guiGraphics, x0, y2 - overlap, corner + overlap, (y3 - y2) + overlap, 0.0f, 1.0f - uCorner, uCorner, 1.0f, color);
        blitSlice(guiGraphics, x1, y2 - overlap, (x2 - x1) + overlap, (y3 - y2) + overlap, uCorner, 1.0f - uCorner, 1.0f - uCorner, 1.0f, color);
        blitSlice(guiGraphics, x2 - overlap, y2 - overlap, (x3 - x2) + overlap, (y3 - y2) + overlap, 1.0f - uCorner, 1.0f - uCorner, 1.0f, 1.0f, color);
    }

    private static void blitSlice(GuiGraphics guiGraphics, int x, int y, int w, int h,
                                  float u0, float v0, float u1, float v1, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int tex = ROUNDED_BG_TEX_SIZE;
        int u = Math.round(u0 * tex);
        int v = Math.round(v0 * tex);
        int uSize = Math.round((u1 - u0) * tex);
        int vSize = Math.round((v1 - v0) * tex);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ROUNDED_BG_TEXTURE, x, y, u, v, w, h,
                uSize, vSize, tex, tex, color);
    }
}
