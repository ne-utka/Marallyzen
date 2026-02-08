package neutka.marallys.marallyzen.client.poster.text;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Builds a texture from PosterTextData.
     * Returns Identifier for the texture, or null if data is empty or style is OLD.
     */
    public static Identifier buildTexture(PosterTextData data) {
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
            net.minecraft.client.renderer.texture.DynamicTexture dynamicTexture =
                new net.minecraft.client.renderer.texture.DynamicTexture(() -> "marallyzen_poster_text_" + System.currentTimeMillis(), image);

            String textureNamespace = "marallyzen";
            String texturePath = "poster_text_" + System.currentTimeMillis() + "_" + data.hashCode();
            Identifier location = Identifier.parse(textureNamespace + ":" + texturePath);

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
     * Placeholder image. Offscreen font rendering pipeline changed in 1.21.11.
     * The actual text is rendered directly in PosterEntityRenderer to match legacy layout.
     */
    private static NativeImage renderTextToImage(PosterTextData data, net.minecraft.client.gui.Font font) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, TEXTURE_WIDTH, TEXTURE_HEIGHT, false);
        for (int y = 0; y < TEXTURE_HEIGHT; y++) {
            for (int x = 0; x < TEXTURE_WIDTH; x++) {
                image.setPixel(x, y, 0x00000000);
            }
        }
        return image;
    }
}
