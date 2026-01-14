package neutka.marallys.marallyzen.client.poster.text;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton cache for poster text textures.
 * Maps PosterTextKey to ResourceLocation.
 * Textures are not recreated if they already exist.
 */
public class PosterTextTextureCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PosterTextTextureCache.class);
    private static final Map<PosterTextKey, ResourceLocation> cache = new HashMap<>();
    
    /**
     * Gets or creates a texture for the given PosterTextData.
     * Returns existing texture if available, otherwise creates a new one.
     */
    public static ResourceLocation getOrCreate(PosterTextData data) {
        LOGGER.warn("========== PosterTextTextureCache.getOrCreate() CALLED ==========");
        LOGGER.warn("Data: {}", data);
        if (data != null) {
            LOGGER.warn("  Title: {}", data.title());
            LOGGER.warn("  Author: {}", data.author());
            LOGGER.warn("  Style: {}", data.style());
            LOGGER.warn("  Pages: {}", data.pages());
            LOGGER.warn("  isEmpty: {}", data.isEmpty());
        }
        
        if (data == null || data.isEmpty() || data.style() == PosterStyle.OLD) {
            LOGGER.warn("PosterTextTextureCache: Data is null/empty or style is OLD, returning null");
            return null;
        }
        
        PosterTextKey key = PosterTextKey.from(data);
        LOGGER.warn("PosterTextTextureCache: Key={}", key);
        
        // Check cache first
        ResourceLocation existing = cache.get(key);
        if (existing != null) {
            LOGGER.warn("PosterTextTextureCache: Found cached texture {} for key {}", existing, key);
            return existing;
        }
        
        LOGGER.warn("PosterTextTextureCache: Cache miss, creating new texture...");
        // Create new texture
        ResourceLocation texture = PosterTextTextureBuilder.buildTexture(data);
        if (texture != null) {
            cache.put(key, texture);
            LOGGER.warn("PosterTextTextureCache: Created and cached texture {} for key {}", texture, key);
        } else {
            LOGGER.warn("PosterTextTextureCache: buildTexture returned null");
        }
        LOGGER.warn("========== PosterTextTextureCache.getOrCreate() RETURNING ==========");
        
        return texture;
    }
    
    /**
     * Clears the cache.
     * Should be called when exiting world or reloading resources.
     */
    public static void clear() {
        LOGGER.debug("PosterTextTextureCache: Clearing cache ({} entries)", cache.size());
        cache.clear();
    }
    
    /**
     * Gets the current cache size (for debugging).
     */
    public static int size() {
        return cache.size();
    }
}




