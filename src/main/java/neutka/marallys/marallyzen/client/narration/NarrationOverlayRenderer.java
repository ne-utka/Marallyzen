package neutka.marallys.marallyzen.client.narration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.quest.QuestJournalScreen;

/**
 * Renders the narration overlay as a Bedrock-style semi-transparent panel at the bottom of the screen.
 * Uses RenderGuiEvent.Post to render on top of the game HUD.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class NarrationOverlayRenderer {
    private static final int FADE_TICKS = 8;
    private static float overlayAlpha = 1.0f;
    private static float previousAlpha = 1.0f;
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) {
            return;
        }
        boolean blockOverlay = mc.screen instanceof QuestJournalScreen;
        
        // Don't render narration/proximity during screen fade cutscene
        if (neutka.marallys.marallyzen.client.cutscene.ScreenFadeManager.getInstance().isActive()) {
            return;
        }
        
        // Don't render narration/proximity during eyes close cutscene
        if (neutka.marallys.marallyzen.client.cutscene.EyesCloseManager.getInstance().isActive()) {
            return;
        }
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        var window = mc.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        
        // Get partialTick for smooth 60fps interpolation
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
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
        
        // Calculate box dimensions: text size + 2px padding on each side
        int boxWidth = maxLineWidth + 4; // 2px padding on each side
        int boxHeight = textHeight + 4; // 2px padding on each side
        
        // Bedrock-style positioning: bottom of screen, centered horizontally
        int x = (width - boxWidth) / 2;
        int y = height - 60; // 60px from bottom (lower on screen)
        
        // Calculate alpha for background (more transparent - ~47% opacity when fully visible)
        int bgAlpha = (int) (alpha * 120); // ~47% of 255 (was 180 = ~70%)
        int bgColor = (bgAlpha << 24); // ARGB: alpha in top 8 bits, RGB = 0 (black)
        
        // Draw semi-transparent background rectangle (fits text with 2px padding)
        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);
        
        // Draw text with alpha
        int textAlpha = (int) (alpha * 255);
        int textColor = 0xFFFFFF | (textAlpha << 24); // White text with alpha
        
        // Draw text lines (2px offset from box edges)
        int textX = x + 2;
        int textY = y + 2;
        
        for (int i = 0; i < lines.size() && i < 2; i++) { // Max 2 lines
            int lineY = textY + (i * lineHeight);
            guiGraphics.drawString(font, lines.get(i), textX, lineY, textColor, false);
        }
    }
}
