package neutka.marallys.marallyzen.client.cutscene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Renders the screen fade overlay as a fullscreen black overlay with text.
 * Uses RenderGuiEvent.Post to render on top of everything.
 * The black overlay with alpha 1.0 will naturally hide HUD elements underneath.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ScreenFadeOverlayRenderer {
    
    // Text color: #E6E1C5 (RGB: 230, 225, 197)
    private static final int TEXT_COLOR_RGB = 0xE6E1C5;
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) {
            return;
        }
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        var window = mc.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        
        // Get partialTick for smooth 60fps interpolation
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        
        ScreenFadeManager manager = ScreenFadeManager.getInstance();
        ScreenFadeOverlay overlay = manager.getActive();
        
        if (overlay != null && overlay.isVisible()) {
            // Interpolate overlay alpha for smooth 60fps animation
            float interpolatedOverlayAlpha = Mth.lerp(partialTick, overlay.getPreviousOverlayAlpha(), overlay.getOverlayAlpha());
            
            // Render fullscreen black overlay
            int overlayAlpha = (int) (interpolatedOverlayAlpha * 255);
            int overlayColor = (overlayAlpha << 24) | 0x000000; // ARGB: black with alpha
            guiGraphics.fill(0, 0, width, height, overlayColor);
            
            // Render text during black screen phase
            if (overlay.isTextVisible()) {
                // Interpolate text alpha for smooth 60fps animation (separate for title and subtitle)
                float interpolatedTitleAlpha = Mth.lerp(partialTick, overlay.getPreviousTitleAlpha(), overlay.getTitleAlpha());
                float interpolatedSubtitleAlpha = Mth.lerp(partialTick, overlay.getPreviousSubtitleAlpha(), overlay.getSubtitleAlpha());
                
                if (interpolatedTitleAlpha > 0.0f || interpolatedSubtitleAlpha > 0.0f) {
                    renderText(guiGraphics, mc, width, height, overlay.getTitleText(), overlay.getSubtitleText(), interpolatedTitleAlpha, interpolatedSubtitleAlpha);
                }
            }
        }
    }
    
    /**
     * Renders title and subtitle text.
     * Title: centered on screen, large font
     * Subtitle: at HUD level (bottom of screen), smaller font, red color
     * 
     * @param guiGraphics The graphics context
     * @param mc Minecraft instance
     * @param width Screen width
     * @param height Screen height
     * @param titleText Main text (can be null) - "30 минут спустя"
     * @param subtitleText Subtitle text (can be null) - "11 августа 2024 г."
     * @param titleAlpha Title alpha (0.0 to 1.0)
     * @param subtitleAlpha Subtitle alpha (0.0 to 1.0)
     */
    private static void renderText(GuiGraphics guiGraphics, Minecraft mc, int width, int height, 
                                   Component titleText, Component subtitleText, float titleAlpha, float subtitleAlpha) {
        var font = mc.font;
        
        // Calculate text alpha values
        int titleAlphaInt = (int) (titleAlpha * 255);
        int subtitleAlphaInt = (int) (subtitleAlpha * 255);
        
        // Render title text (centered, large) - "30 минут спустя"
        if (titleText != null && !titleText.getString().isEmpty() && titleAlphaInt > 0) {
            // Use larger font size for title (scale 3.0 = 2.0 * 1.5)
            float titleScale = 3.0f;
            int titleWidth = (int) (font.width(titleText) * titleScale);
            int titleX = (width - titleWidth) / 2;
            int titleY = height / 2 - (int) (font.lineHeight * titleScale / 2); // Center vertically
            
            // Draw title with scale
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(titleX, titleY, 0);
            guiGraphics.pose().scale(titleScale, titleScale, 1.0f);
            int titleColor = TEXT_COLOR_RGB | (titleAlphaInt << 24); // ARGB format
            guiGraphics.drawString(font, titleText, 0, 0, titleColor, false);
            guiGraphics.pose().popPose();
        }
        
        // Render subtitle text (at HUD level, bottom of screen, scaled 1.5x, yellow) - "11 августа 2024 г."
        if (subtitleText != null && !subtitleText.getString().isEmpty() && subtitleAlphaInt > 0) {
            // Use scale 1.5 for subtitle
            float subtitleScale = 1.5f;
            int subtitleWidth = (int) (font.width(subtitleText) * subtitleScale);
            int subtitleX = (width - subtitleWidth) / 2; // Centered horizontally
            // Position at HUD level (bottom of screen, similar to health bar position)
            int subtitleY = height - 50; // 50px from bottom (adjusted for larger text)
            
            // Prime color: #D48E03 (RGB: 212, 142, 3)
            int yellowColor = 0x00D48E03 | (subtitleAlphaInt << 24); // ARGB format: yellow with alpha
            
            // Draw subtitle with scale
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(subtitleX, subtitleY, 0);
            guiGraphics.pose().scale(subtitleScale, subtitleScale, 1.0f);
            guiGraphics.drawString(font, subtitleText, 0, 0, yellowColor, false);
            guiGraphics.pose().popPose();
        }
    }
}


