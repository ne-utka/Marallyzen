package neutka.marallys.marallyzen.client.narration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.lever.LeverQteClient;
import neutka.marallys.marallyzen.client.valve.ValveQteClient;
import neutka.marallys.marallyzen.client.quest.QuestJournalScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;

/**
 * Renders the narration overlay as a Bedrock-style semi-transparent panel at the bottom of the screen.
 * Uses RenderGuiEvent.Post to render on top of the game HUD.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class NarrationOverlayRenderer {
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
                    renderOverlay(guiGraphics, mc, width, height, narrationOverlay.getText(), finalAlpha);
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
                    renderOverlay(guiGraphics, mc, width, height, proximityOverlay.getText(), finalAlpha);
                }
            }
        }
    }
    
    private static void renderOverlay(GuiGraphics guiGraphics, Minecraft mc, int width, int height, Component text, float alpha) {
        if (text == null || alpha <= 0.0f) {
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
        renderRoundedBackground(guiGraphics, x, y, boxWidth, boxHeight, bgColor);
        
        // Draw text with alpha
        int textAlpha = (int) (alpha * 255);
        int textColor = 0xFFFFFF | (textAlpha << 24); // White text with alpha
        
        // Draw text lines (padding offset from box edges)
        int textX = x + paddingX;
        int textY = y + paddingY;
        
        for (int i = 0; i < lines.size() && i < 2; i++) { // Max 2 lines
            int lineY = textY + (i * lineHeight);
            guiGraphics.drawString(font, lines.get(i), textX, lineY, textColor, false);
        }
    }

    private static void renderRoundedBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int corner = Math.min(ROUNDED_BG_RADIUS_PX, Math.min(width, height) / 2);
        if (corner <= 0) {
            guiGraphics.fill(x, y, x + width, y + height, color);
            return;
        }

        int cornerPx = corner;
        int tex = ROUNDED_BG_TEX_SIZE;
        float uCorner = (float) ROUNDED_BG_CORNER_PX / (float) ROUNDED_BG_TEX_SIZE;
        int colorArgb = color;

        int x0 = x;
        int x1 = x + cornerPx;
        int x2 = x + width - cornerPx;
        int x3 = x + width;
        int y0 = y;
        int y1 = y + cornerPx;
        int y2 = y + height - cornerPx;
        int y3 = y + height;

        // Top row
        blitSlice(guiGraphics, x0, y0, cornerPx, cornerPx, 0.0f, 0.0f, uCorner, uCorner, tex, colorArgb);
        blitSlice(guiGraphics, x1, y0, x2 - x1, cornerPx, uCorner, 0.0f, 1.0f - uCorner, uCorner, tex, colorArgb);
        blitSlice(guiGraphics, x2, y0, x3 - x2, cornerPx, 1.0f - uCorner, 0.0f, 1.0f, uCorner, tex, colorArgb);

        // Middle row
        blitSlice(guiGraphics, x0, y1, cornerPx, y2 - y1, 0.0f, uCorner, uCorner, 1.0f - uCorner, tex, colorArgb);
        blitSlice(guiGraphics, x1, y1, x2 - x1, y2 - y1, uCorner, uCorner, 1.0f - uCorner, 1.0f - uCorner, tex, colorArgb);
        blitSlice(guiGraphics, x2, y1, x3 - x2, y2 - y1, 1.0f - uCorner, uCorner, 1.0f, 1.0f - uCorner, tex, colorArgb);

        // Bottom row
        blitSlice(guiGraphics, x0, y2, cornerPx, y3 - y2, 0.0f, 1.0f - uCorner, uCorner, 1.0f, tex, colorArgb);
        blitSlice(guiGraphics, x1, y2, x2 - x1, y3 - y2, uCorner, 1.0f - uCorner, 1.0f - uCorner, 1.0f, tex, colorArgb);
        blitSlice(guiGraphics, x2, y2, x3 - x2, y3 - y2, 1.0f - uCorner, 1.0f - uCorner, 1.0f, 1.0f, tex, colorArgb);
    }

    private static void blitSlice(GuiGraphics guiGraphics, int x, int y, int w, int h,
                                  float u0, float v0, float u1, float v1, int texSize, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float u = u0 * texSize;
        float v = v0 * texSize;
        float uSize = (u1 - u0) * texSize;
        float vSize = (v1 - v0) * texSize;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ROUNDED_BG_TEXTURE, x, y, u, v, w, h, texSize, texSize, color);
    }
}


