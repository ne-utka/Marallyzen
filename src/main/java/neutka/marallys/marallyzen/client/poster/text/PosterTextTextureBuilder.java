package neutka.marallys.marallyzen.client.poster.text;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Builds pre-rendered textures for poster text.
 * Renders text once into a NativeImage, then converts to DynamicTexture.
 * Text is rendered only once, not every frame.
 */
public class PosterTextTextureBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(PosterTextTextureBuilder.class);
    
    // Fixed texture dimensions (2x for better quality)
    // Original: 55x73 px, working area: 43x61 px, margin: 6 px
    // Scaled 2x: 110x146 px, working area: 86x122 px, margin: 12 px
    private static final int TEXTURE_WIDTH = 110;
    private static final int TEXTURE_HEIGHT = 146;
    private static final int WORKING_WIDTH = 86;
    private static final int WORKING_HEIGHT = 122;
    private static final int MARGIN = 12;
    
    /**
     * Builds a texture from PosterTextData.
     * Returns ResourceLocation for the texture, or null if data is empty or style is OLD.
     */
    public static ResourceLocation buildTexture(PosterTextData data) {
        LOGGER.warn("========== PosterTextTextureBuilder.buildTexture() CALLED ==========");
        LOGGER.warn("Data: {}", data);
        
        if (data == null || data.isEmpty() || data.style() == PosterStyle.OLD) {
            LOGGER.warn("PosterTextTextureBuilder: Data is null/empty or style is OLD, returning null");
            return null;
        }
        
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        if (font == null) {
            LOGGER.warn("PosterTextTextureBuilder: Font is null, cannot build texture");
            return null;
        }
        
        try {
            LOGGER.warn("PosterTextTextureBuilder: Rendering text to image...");
            NativeImage image = renderTextToImage(data, font);
            if (image == null) {
                LOGGER.warn("PosterTextTextureBuilder: renderTextToImage returned null");
                return null;
            }
            
            LOGGER.warn("PosterTextTextureBuilder: Creating DynamicTexture...");
            net.minecraft.client.renderer.texture.DynamicTexture dynamicTexture = new net.minecraft.client.renderer.texture.DynamicTexture(image);
            
            String textureNamespace = "marallyzen";
            String texturePath = "poster_text_" + System.currentTimeMillis() + "_" + data.hashCode();
            ResourceLocation location = ResourceLocation.parse(textureNamespace + ":" + texturePath);
            
            LOGGER.warn("PosterTextTextureBuilder: Registering texture with TextureManager: {}", location);
            Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);

            LOGGER.warn("PosterTextTextureBuilder: SUCCESS - Created texture {} for poster text", location);
            LOGGER.warn("========== PosterTextTextureBuilder.buildTexture() RETURNING ==========");
            return location;
        } catch (Exception e) {
            LOGGER.error("PosterTextTextureBuilder: FAILED to build texture", e);
            return null;
        }
    }
    
    /**
     * Renders text into NativeImage using Minecraft Font via character-by-character rendering.
     * This approach uses Minecraft's font directly to ensure proper spacing and appearance.
     */
    private static NativeImage renderTextToImage(PosterTextData data, net.minecraft.client.gui.Font font) {
        // Create NativeImage with transparent background
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, TEXTURE_WIDTH, TEXTURE_HEIGHT, false);
        
        // Fill with transparent background
        for (int y = 0; y < TEXTURE_HEIGHT; y++) {
            for (int x = 0; x < TEXTURE_WIDTH; x++) {
                image.setPixelRGBA(x, y, 0x00000000); // Fully transparent
            }
        }
        
        // Use Framebuffer to render text with Minecraft Font
        com.mojang.blaze3d.pipeline.RenderTarget framebuffer = new com.mojang.blaze3d.pipeline.TextureTarget(TEXTURE_WIDTH, TEXTURE_HEIGHT, true, false);
        framebuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        
        // Save current framebuffer state
        com.mojang.blaze3d.pipeline.RenderTarget mainFramebuffer = Minecraft.getInstance().getMainRenderTarget();
        
        try {
            // Bind our framebuffer for writing
            framebuffer.bindWrite(false);
            framebuffer.clear(false);
            
            // Setup rendering context - ensure we're rendering to our framebuffer
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(
                new org.joml.Matrix4f().ortho(0.0f, TEXTURE_WIDTH, TEXTURE_HEIGHT, 0.0f, -1000.0f, 1000.0f),
                com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z
            );
            
            // Clear viewport and ensure we're rendering to our framebuffer
            com.mojang.blaze3d.systems.RenderSystem.viewport(0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            
            // Disable depth test and enable blending for transparent background
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            
            com.mojang.blaze3d.vertex.PoseStack poseStack = new com.mojang.blaze3d.vertex.PoseStack();
            poseStack.setIdentity();
            
            // Choose style-dependent attributes
            int titleColor = data.style() == PosterStyle.PAPER ? 0xFF1A1A1A : 0xFF2B1B0E;
            int textColor = titleColor;
            int authorColor = data.style() == PosterStyle.PAPER ? 0xFF555555 : titleColor;
            float titleScale = data.style() == PosterStyle.PAPER ? 1.25f : 1.3f;
            float authorScale = data.style() == PosterStyle.PAPER ? 0.8f : 0.7f;
            boolean isAllCaps = data.style() != PosterStyle.PAPER;
            int textLineHeight = data.style() == PosterStyle.PAPER ? 14 : 13;

            int margin = MARGIN;
            int workingWidth = WORKING_WIDTH;
            int workingHeight = WORKING_HEIGHT;
            if (data.style() == PosterStyle.PAPER) {
                margin = MARGIN + 2;
                workingWidth = WORKING_WIDTH - 4;
                workingHeight = WORKING_HEIGHT - 6;
            }

            int currentY = margin;
            int centerX = TEXTURE_WIDTH / 2;
            int maxY = margin + workingHeight;
            
            // Create a buffer source for rendering that uses our framebuffer
            // We need to create a custom buffer source that renders to our framebuffer
            // For now, let's try using the main buffer source but ensure we're bound to our framebuffer
            net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = 
                Minecraft.getInstance().renderBuffers().bufferSource();
            
            // Ensure we're still bound to our framebuffer
            int writeFBO = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            LOGGER.warn("PosterTextTextureBuilder: Write FBO before rendering: {} (expected: {})", writeFBO, framebuffer.frameBufferId);
            
            // Force bind our framebuffer for writing
            if (writeFBO != framebuffer.frameBufferId) {
                org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, framebuffer.frameBufferId);
                LOGGER.warn("PosterTextTextureBuilder: Forced bind to our framebuffer for writing");
            }
            
            // Enable blending for text rendering
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            
            LOGGER.warn("PosterTextTextureBuilder: Starting text rendering, framebuffer ID: {}", framebuffer.frameBufferId);
            
            try {
                // 1. TITLE
                if (data.title() != null && !data.title().isEmpty() && !data.title().equals("Poster")) {
                    String titleText = isAllCaps ? data.title().toUpperCase() : data.title();
                    // Make title bold
                    net.minecraft.network.chat.Component titleComponent = net.minecraft.network.chat.Component.literal(titleText)
                        .withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true));
                    
                    // Calculate title width with scale
                    int titleWidth = (int)(font.width(titleComponent) * titleScale);
                    int maxTitleWidth = workingWidth;
                    
                    poseStack.pushPose();
                    poseStack.scale(titleScale, titleScale, 1.0f);
                    
                    if (titleWidth > maxTitleWidth) {
                        // Title is too long, split into multiple lines
                        List<net.minecraft.util.FormattedCharSequence> titleLines = font.split(titleComponent, (int)(maxTitleWidth / titleScale));
                        int titleY = currentY;
                        
                        for (net.minecraft.util.FormattedCharSequence line : titleLines) {
                            int lineWidth = (int)(font.width(line) * titleScale);
                            int titleX = centerX - lineWidth / 2;
                            
                            poseStack.pushPose();
                            poseStack.translate(titleX / titleScale, titleY / titleScale, 0);
                            // Ensure we're still bound to our framebuffer
                            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, framebuffer.frameBufferId);
                            font.drawInBatch(line, 0, 0, titleColor, false, poseStack.last().pose(), bufferSource, 
                                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                            bufferSource.endBatch(); // Flush immediately
                            poseStack.popPose();
                            
                            titleY += (int)(font.lineHeight * titleScale) + 2;
                        }
                        
                        currentY = titleY;
                    } else {
                        // Title fits on one line
                        int titleX = centerX - titleWidth / 2;
                        poseStack.pushPose();
                        poseStack.translate(titleX / titleScale, currentY / titleScale, 0);
                        // Ensure we're still bound to our framebuffer
                        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, framebuffer.frameBufferId);
                        font.drawInBatch(titleComponent, 0, 0, titleColor, false, poseStack.last().pose(), bufferSource, 
                            net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                        bufferSource.endBatch(); // Flush immediately
                        poseStack.popPose();
                        currentY += (int)(font.lineHeight * titleScale) + 4;
                    }
                    
                    poseStack.popPose();
                    
                    // Draw separator line using simple pixel drawing
                    for (int x = margin; x < TEXTURE_WIDTH - margin; x++) {
                        image.setPixelRGBA(x, currentY - 2, titleColor | 0xFF000000);
                    }
                    currentY += 6;
                }
                
                // 2. BODY TEXT
                // Calculate reserved space for author (with extra margin to prevent compression)
                int authorHeight = 0;
                if (data.author() != null && !data.author().isEmpty()) {
                    // Reserve more space: author line height * scale + extra margin for spacing
                    // Use full line height (not scaled) to ensure enough space, plus margin
                    authorHeight = font.lineHeight + MARGIN + 6; // Extra 6px for spacing to prevent compression
                }
                int maxYForText = Math.min(TEXTURE_HEIGHT - margin - authorHeight, maxY - authorHeight);
                
                if (data.pages() != null && !data.pages().isEmpty()) {
                    int textY = currentY;
                    int wrapWidth = workingWidth;
                    
                    for (String page : data.pages()) {
                        if (page == null || page.isEmpty()) {
                            textY += textLineHeight;
                            continue;
                        }
                        
                        String[] paragraphs = page.split("\n\n");
                        for (String paragraph : paragraphs) {
                            if (paragraph.isEmpty()) {
                                textY += textLineHeight;
                                continue;
                            }
                            
                            String[] lines = paragraph.split("\n");
                            for (String line : lines) {
                                if (line.isEmpty()) {
                                    textY += textLineHeight;
                                    continue;
                                }
                                
                                // Use Minecraft font's word wrap
                                List<net.minecraft.util.FormattedCharSequence> wrappedLines = font.split(
                                    net.minecraft.network.chat.Component.literal(line), wrapWidth);
                                
                                for (net.minecraft.util.FormattedCharSequence wrappedLine : wrappedLines) {
                                    // Check if we have space for this line (including line height)
                                    if (textY + textLineHeight > maxYForText) {
                                        // No more space, stop rendering
                                        break;
                                    }
                                    
                                    int lineWidth = font.width(wrappedLine);
                                    int lineX = centerX - lineWidth / 2;
                                    
                                    poseStack.pushPose();
                                    poseStack.translate(lineX, textY, 0);
                                    // Ensure we're still bound to our framebuffer
                                    org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, framebuffer.frameBufferId);
                                    font.drawInBatch(wrappedLine, 0, 0, textColor, false, poseStack.last().pose(), bufferSource, 
                                        net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                                    bufferSource.endBatch(); // Flush immediately
                                    poseStack.popPose();
                                    
                                    textY += textLineHeight;
                                }
                                
                                // Check again after rendering all wrapped lines
                                if (textY > maxYForText) break;
                            }
                            
                            // Check if we have space for paragraph spacing
                            if (textY + textLineHeight > maxYForText) {
                                break;
                            }
                            if (textY <= maxYForText) {
                                textY += textLineHeight;
                            }
                        }
                    }
                    
                    currentY = textY;
                }
                
                // 3. AUTHOR (always render at bottom, reserved space already calculated)
                if (data.author() != null && !data.author().isEmpty()) {
                    String authorStr = "— " + data.author();
                    net.minecraft.network.chat.Component authorComponent = net.minecraft.network.chat.Component.literal(authorStr);
                    
                    poseStack.pushPose();
                    poseStack.scale(authorScale, authorScale, 1.0f);
                    
                    int authorWidth = (int)(font.width(authorComponent) * authorScale);
                    // Position author with proper spacing from bottom, accounting for scale
                    // Use extra spacing to prevent compression
                    int authorY = TEXTURE_HEIGHT - margin - 6 - (int)(font.lineHeight * authorScale);
                    
                    int authorX;
                    if (data.style() == PosterStyle.PAPER) {
                        authorX = margin + workingWidth - authorWidth;
                    } else {
                        authorX = centerX - authorWidth / 2;
                    }
                    
                    poseStack.pushPose();
                    poseStack.translate(authorX / authorScale, authorY / authorScale, 0);
                    // Ensure we're still bound to our framebuffer
                    org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER, framebuffer.frameBufferId);
                    font.drawInBatch(authorComponent, 0, 0, authorColor, false, poseStack.last().pose(), bufferSource, 
                        net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                    bufferSource.endBatch(); // Flush immediately
                    poseStack.popPose();
                    
                    poseStack.popPose();
                }
                
                bufferSource.endBatch();
                
            } finally {
                bufferSource.endBatch();
            }
            
            // Flush all rendering to ensure text is in framebuffer
            // RenderSystem doesn't have flush() in this version, bufferSource.endBatch() already flushed
            
            // Copy framebuffer to NativeImage using GL
            // Unbind write first, then bind for reading
            framebuffer.unbindWrite();
            
            // Get framebuffer ID before binding
            int ourFBO = framebuffer.frameBufferId;
            LOGGER.debug("PosterTextTextureBuilder: Our framebuffer ID: {}", ourFBO);
            
            // Force bind our framebuffer for reading using GL directly
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, ourFBO);
            
            try {
                // Verify we're reading from the correct framebuffer
                int currentFBO = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING);
                LOGGER.warn("PosterTextTextureBuilder: Reading from FBO: {} (expected: {})", currentFBO, ourFBO);
                
                if (currentFBO != ourFBO) {
                    LOGGER.warn("PosterTextTextureBuilder: FBO still mismatched after bind! Reading from {} but expected {}", currentFBO, ourFBO);
                }
                
                // Read pixels from framebuffer using GL into ByteBuffer
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(TEXTURE_WIDTH * TEXTURE_HEIGHT * 4);
                // Use LWJGL constants for GL calls
                org.lwjgl.opengl.GL11.glReadPixels(0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, 
                    org.lwjgl.opengl.GL11.GL_RGBA, 
                    org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, 
                    buffer);
                buffer.rewind();
                
                // Copy from ByteBuffer to NativeImage and count non-transparent pixels
                int nonTransparentPixels = 0;
                int totalPixels = TEXTURE_WIDTH * TEXTURE_HEIGHT;
                // Note: GL reads in bottom-to-top order, but NativeImage expects top-to-bottom
                for (int y = 0; y < TEXTURE_HEIGHT; y++) {
                    for (int x = 0; x < TEXTURE_WIDTH; x++) {
                        // Read from bottom-up (GL order) and write top-down (NativeImage order)
                        int srcY = TEXTURE_HEIGHT - 1 - y;
                        int index = (srcY * TEXTURE_WIDTH + x) * 4;
                        int r = buffer.get(index) & 0xFF;
                        int g = buffer.get(index + 1) & 0xFF;
                        int b = buffer.get(index + 2) & 0xFF;
                        int a = buffer.get(index + 3) & 0xFF;
                        image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                        if (a > 0) {
                            nonTransparentPixels++;
                        }
                    }
                }
                LOGGER.warn("PosterTextTextureBuilder: Read {} non-transparent pixels out of {} total pixels", nonTransparentPixels, totalPixels);
            } catch (Exception e) {
                LOGGER.error("PosterTextTextureBuilder: Failed to read pixels from framebuffer", e);
            }
            
        } catch (Exception e) {
            LOGGER.error("PosterTextTextureBuilder: Failed to render text with Minecraft Font", e);
            return image; // Return partially rendered image
        } finally {
            // Unbind our framebuffer
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, 0);
            
            // Restore rendering state
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            
            // Restore main framebuffer
            if (mainFramebuffer != null) {
                mainFramebuffer.bindWrite(false);
                // Restore viewport to main framebuffer size
                com.mojang.blaze3d.systems.RenderSystem.viewport(0, 0, mainFramebuffer.viewWidth, mainFramebuffer.viewHeight);
            }
            framebuffer.destroyBuffers();
        }
        
        return image;
    }
}
