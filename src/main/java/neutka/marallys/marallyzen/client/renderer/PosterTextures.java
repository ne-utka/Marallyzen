package neutka.marallys.marallyzen.client.renderer;

import net.minecraft.resources.ResourceLocation;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.Map;

/**
 * Resolves poster texture resource locations for small (wall) and full (flying) variants.
 * Supports legacy numbered posters and new named posters.
 */
public final class PosterTextures {

    private static final Map<Integer, String> SMALL_TEXTURE_OVERRIDES = Map.of(
            11, "oldposter",
            12, "paperposter1",
            13, "paperposter2"
    );

    private static final Map<Integer, String> FULL_TEXTURE_OVERRIDES = Map.of(
            11, "oldposterfull_default",
            12, "paperposterfull",
            13, "paperposterfull"
    );

    private PosterTextures() {
    }

    private static String resolveSmallName(int posterNumber) {
        return SMALL_TEXTURE_OVERRIDES.getOrDefault(posterNumber, "poster" + posterNumber);
    }

    private static String resolveFullName(int posterNumber) {
        return FULL_TEXTURE_OVERRIDES.getOrDefault(posterNumber, "posterfull");
    }

    /**
     * Small wall texture (block atlas).
     */
    public static ResourceLocation getSmallTexture(int posterNumber) {
        String name = resolveSmallName(posterNumber);
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/" + name + ".png");
    }

    /**
     * Full-size flying poster texture.
     * Defaults to entity/posterfull.png for legacy posters.
     * Overrides use block/xxxfull.png as requested.
     * For oldposter (ID 11), supports variants: "default", "alive", "band", "dead"
     */
    public static ResourceLocation getFullTexture(int posterNumber) {
        return getFullTexture(posterNumber, "default");
    }
    
    /**
     * Full-size flying poster texture with variant support.
     * For oldposter (ID 11), variant can be "default", "alive", "band", or "dead".
     */
    public static ResourceLocation getFullTexture(int posterNumber, String variant) {
        String name = resolveFullName(posterNumber);
        
        // Special handling for oldposter (ID 11) with variants
        if (posterNumber == 11 && variant != null && !variant.isEmpty() && !variant.equals("default")) {
            // Use variant-specific texture: oldposterfull_alive, oldposterfull_band, oldposterfull_dead
            name = "oldposterfull_" + variant;
        }
        
        // If default posterfull, keep existing entity path
        if ("posterfull".equals(name)) {
            return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/entity/posterfull.png");
        }
        // Override textures are stored in block/ folder
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/" + name + ".png");
    }
}

