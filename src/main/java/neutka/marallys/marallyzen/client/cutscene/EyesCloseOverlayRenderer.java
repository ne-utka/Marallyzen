package neutka.marallys.marallyzen.client.cutscene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;
import org.joml.Matrix4f;

/**
 * Renders a cinematic "blink" overlay using an elliptical vignette.
 * 
 * NOT rectangles, NOT "shutters" — a dynamic radial mask where:
 * - Black = distance_from_center > threshold
 * - Center closes LAST (like real eyelids)
 * - Edges are soft via smoothstep
 * - Feels like actual blinking, not UI
 * 
 * Based on Bedrock Edition / AAA cutscene style.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class EyesCloseOverlayRenderer {
    
    // ═══════════════════════════════════════════════════════════════════
    // VIGNETTE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════
    
    // Open radius when eyelidPosition = 0 (fully open)
    // > 1.0 means vignette starts outside screen edges
    private static final float OPEN_RADIUS = 1.3f;
    
    // Closed radius when eyelidPosition = 1 (fully closed)
    private static final float CLOSED_RADIUS = 0.0f;
    
    // Softness of the edge transition (smoothstep range)
    // Higher = softer/blurrier edge, Lower = sharper
    private static final float EDGE_SOFTNESS = 0.18f;
    
    // Render grid size (2 = 2x2 pixels per sample, 4 = 4x4)
    // Larger = faster but blockier (eye won't notice at 4)
    private static final int GRID_SIZE = 3;
    
    // Ellipse aspect ratio (vertical is smaller = eyelids close vertically first)
    // 1.0 = perfect circle, < 1.0 = taller ellipse, > 1.0 = wider ellipse
    private static final float ASPECT_RATIO = 0.75f;
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        
        EyesCloseManager manager = EyesCloseManager.getInstance();
        EyesCloseOverlay overlay = manager.getActive();
        
        if (overlay == null || !overlay.isVisible()) {
            return;
        }
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        var window = mc.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        
        // Get partialTick for smooth 60fps interpolation
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        
        // Interpolate eyelid position for smooth animation
        float eyelidPosition = Mth.lerp(partialTick, 
                overlay.getPreviousEyelidPosition(), 
                overlay.getEyelidPosition());
        eyelidPosition = Mth.clamp(eyelidPosition, 0.0f, 1.0f);
        
        // Render the elliptical vignette
        renderVignette(guiGraphics, width, height, eyelidPosition);
    }
    
    /**
     * Renders the elliptical vignette mask.
     * 
     * For each pixel (sampled on grid), calculates normalized ellipse distance
     * from center and applies smoothstep to determine alpha.
     * Center closes LAST — exactly like real eyelids.
     */
    private static void renderVignette(GuiGraphics guiGraphics, int width, int height, float eyelidPosition) {
        // Calculate current open radius (shrinks as eyes close)
        // eyelidPosition: 0 = open, 1 = closed
        float openRadius = Mth.lerp(eyelidPosition, OPEN_RADIUS, CLOSED_RADIUS);
        
        // If fully closed, just fill black (optimization)
        if (openRadius <= 0.01f) {
            guiGraphics.fill(0, 0, width, height, 0xFF000000);
            return;
        }
        
        // If fully open and radius > screen diagonal, skip rendering
        if (openRadius > 1.5f) {
            return;
        }
        
        // Screen center
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        
        // Half dimensions for normalization
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        
        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        
        // Render grid — each cell is GRID_SIZE x GRID_SIZE pixels
        for (int gy = 0; gy < height; gy += GRID_SIZE) {
            for (int gx = 0; gx < width; gx += GRID_SIZE) {
                // Sample at cell center
                float sampleX = gx + GRID_SIZE / 2.0f;
                float sampleY = gy + GRID_SIZE / 2.0f;
                
                // Normalized distance from center (-1 to 1 range)
                float dx = (sampleX - cx) / halfWidth;
                float dy = (sampleY - cy) / halfHeight;
                
                // Apply aspect ratio (vertical closes faster)
                dy /= ASPECT_RATIO;
                
                // Ellipse distance (0 = center, 1 = edge of unit circle)
                float dist = Mth.sqrt(dx * dx + dy * dy);
                
                // Calculate alpha via smoothstep
                // black if dist > openRadius, transparent if dist < openRadius - softness
                float alpha = smoothstep(openRadius - EDGE_SOFTNESS, openRadius, dist);
                
                // Only render if visible (alpha > threshold)
                if (alpha > 0.005f) {
                    int alphaInt = (int)(alpha * 255);
                    int cellWidth = Math.min(GRID_SIZE, width - gx);
                    int cellHeight = Math.min(GRID_SIZE, height - gy);
                    
                    addQuad(buffer, matrix, gx, gy, gx + cellWidth, gy + cellHeight, 0, 0, 0, alphaInt);
                }
            }
        }
        
        // Draw all quads
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Smoothstep interpolation — the magic behind soft edges.
     * 
     * Returns 0 when x <= edge0
     * Returns 1 when x >= edge1
     * Smooth S-curve transition in between
     */
    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
    
    /**
     * Adds a solid colored quad to the buffer.
     */
    private static void addQuad(BufferBuilder buffer, Matrix4f matrix,
                                float x1, float y1, float x2, float y2,
                                int r, int g, int b, int a) {
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y1, 0).setColor(r, g, b, a);
    }
}
