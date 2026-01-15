package neutka.marallys.marallyzen.client.head;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Manages client-side caching of player head textures.
 * Downloads heads from mc-heads.net and caches them locally.
 */
@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public class HeadCacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadCacheManager.class);
    
    private static final String HEAD_URL_TEMPLATE = "https://mc-heads.net/avatar/%s/26";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    
    // Pattern for valid Minecraft usernames: 3-16 characters, letters, numbers, underscores
    private static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    
    private static final Map<String, ResourceLocation> textureCache = new HashMap<>();
    private static final Map<String, CompletableFuture<ResourceLocation>> pendingDownloads = new HashMap<>();
    private static final ExecutorService downloadExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Marallyzen-HeadDownloader");
        t.setDaemon(true);
        return t;
    });
    
    private static Path cacheDir = null;
    
    /**
     * Gets the cache directory for player heads.
     */
    private static Path getCacheDir() {
        if (cacheDir == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gameDirectory != null) {
                cacheDir = mc.gameDirectory.toPath().resolve("marallyzen").resolve("cache").resolve("heads");
            }
        }
        return cacheDir;
    }
    
    /**
     * Validates a player nickname.
     */
    private static boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }
        String normalized = nickname.toLowerCase().trim();
        return VALID_USERNAME.matcher(normalized).matches();
    }
    
    /**
     * Normalizes a nickname for use in file names and URLs.
     */
    private static String normalizeNickname(String nickname) {
        if (nickname == null) {
            return "";
        }
        return nickname.toLowerCase().trim();
    }
    
    /**
     * Gets the file path for a cached head.
     */
    private static Path getHeadFilePath(String nickname) {
        Path cacheDir = getCacheDir();
        if (cacheDir == null) {
            return null;
        }
        String normalized = normalizeNickname(nickname);
        return cacheDir.resolve(normalized + ".png");
    }
    
    /**
     * Checks if a head is already cached (file exists).
     */
    public static boolean isCached(String nickname) {
        if (!isValidNickname(nickname)) {
            return false;
        }
        
        Path filePath = getHeadFilePath(nickname);
        if (filePath == null) {
            return false;
        }
        
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
    
    /**
     * Gets the ResourceLocation for a cached head texture.
     * Returns null if the texture is not yet loaded or registered.
     */
    public static ResourceLocation getHeadTexture(String nickname) {
        if (!isValidNickname(nickname)) {
            return null;
        }
        
        String normalized = normalizeNickname(nickname);
        return textureCache.get(normalized);
    }
    
    /**
     * Downloads a head from the internet and caches it.
     * This method is async and returns a CompletableFuture.
     */
    public static CompletableFuture<ResourceLocation> requestHead(String nickname) {
        if (!isValidNickname(nickname)) {
            LOGGER.warn("HeadCacheManager: Invalid nickname: {}", nickname);
            return CompletableFuture.completedFuture(null);
        }
        
        String normalized = normalizeNickname(nickname);
        
        // Check if already cached
        ResourceLocation existing = textureCache.get(normalized);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        
        // Check if download is already in progress
        CompletableFuture<ResourceLocation> pending = pendingDownloads.get(normalized);
        if (pending != null) {
            return pending;
        }
        
        // Check if file exists on disk
        Path filePath = getHeadFilePath(nickname);
        if (filePath != null && Files.exists(filePath)) {
            // Load from disk
            CompletableFuture<ResourceLocation> loadFuture = CompletableFuture.supplyAsync(() -> {
                return loadHeadFromFile(nickname, filePath);
            }, downloadExecutor);
            pendingDownloads.put(normalized, loadFuture);
            loadFuture.whenComplete((result, error) -> {
                pendingDownloads.remove(normalized);
                if (error != null) {
                    LOGGER.error("HeadCacheManager: Failed to load head from file: {}", nickname, error);
                }
            });
            return loadFuture;
        }
        
        // Start new download
        CompletableFuture<ResourceLocation> downloadFuture = CompletableFuture.supplyAsync(() -> {
            return downloadAndCacheHead(nickname);
        }, downloadExecutor);
        
        pendingDownloads.put(normalized, downloadFuture);
        downloadFuture.whenComplete((result, error) -> {
            pendingDownloads.remove(normalized);
            if (error != null) {
                LOGGER.error("HeadCacheManager: Failed to download head: {}", nickname, error);
            }
        });
        
        return downloadFuture;
    }
    
    /**
     * Gets or requests a head texture.
     * Returns immediately if cached, otherwise starts async download.
     */
    public static ResourceLocation getOrRequestHead(String nickname) {
        if (!isValidNickname(nickname)) {
            return null;
        }
        
        String normalized = normalizeNickname(nickname);
        
        // Check if already loaded
        ResourceLocation cached = textureCache.get(normalized);
        if (cached != null) {
            return cached;
        }
        
        // Check if file exists - load it synchronously if possible
        Path filePath = getHeadFilePath(nickname);
        if (filePath != null && Files.exists(filePath)) {
            ResourceLocation loaded = loadHeadFromFile(nickname, filePath);
            if (loaded != null) {
                return loaded;
            }
        }
        
        // Start async download (returns null immediately, texture will be available later)
        requestHead(nickname);
        return null;
    }
    
    /**
     * Downloads a head from the internet and saves it to cache.
     */
    private static ResourceLocation downloadAndCacheHead(String nickname) {
        String normalized = normalizeNickname(nickname);
        String urlString = String.format(HEAD_URL_TEMPLATE, normalized);
        
        LOGGER.info("HeadCacheManager: Downloading head for {} from {}", nickname, urlString);
        
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Marallyzen/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOGGER.warn("HeadCacheManager: HTTP error {} for {}", responseCode, nickname);
                return null;
            }
            
            // Read image data
            try (InputStream inputStream = connection.getInputStream()) {
                NativeImage image = NativeImage.read(inputStream);
                
                // Save to cache
                Path cacheDir = getCacheDir();
                if (cacheDir != null) {
                    Files.createDirectories(cacheDir);
                    Path filePath = getHeadFilePath(nickname);
                    if (filePath != null) {
                        image.writeToFile(filePath);
                        LOGGER.info("HeadCacheManager: Saved head to cache: {}", filePath);
                    }
                }
                
                // Register texture on render thread
                ResourceLocation location = registerHeadTexture(nickname, image);
                return location;
            }
        } catch (IOException e) {
            LOGGER.error("HeadCacheManager: Failed to download head for {}: {}", nickname, e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("HeadCacheManager: Unexpected error downloading head for {}: {}", nickname, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Loads a head from a cached file.
     */
    private static ResourceLocation loadHeadFromFile(String nickname, Path filePath) {
        String normalized = normalizeNickname(nickname);
        
        LOGGER.info("HeadCacheManager: Loading head from cache: {}", filePath);
        
        try {
            NativeImage image = NativeImage.read(Files.newInputStream(filePath));
            
            // Register texture on render thread
            ResourceLocation location = registerHeadTexture(nickname, image);
            return location;
        } catch (IOException e) {
            LOGGER.error("HeadCacheManager: Failed to load head from file {}: {}", filePath, e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("HeadCacheManager: Unexpected error loading head from file {}: {}", filePath, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Registers a head texture with the TextureManager.
     * Must be called on the render thread.
     */
    private static ResourceLocation registerHeadTexture(String nickname, NativeImage image) {
        String normalized = normalizeNickname(nickname);
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("marallyzen", "head/" + normalized);
        
        // Check if already registered
        if (textureCache.containsKey(normalized)) {
            return textureCache.get(normalized);
        }
        
        // Register on render thread
        // We need to schedule this on the render thread, but we can't wait for it
        // So we schedule it and return the location immediately
        // The texture will be available on the next frame
        Minecraft.getInstance().execute(() -> {
            try {
                // Create a copy of the image for the texture (NativeImage may be closed)
                // Actually, we should keep the image alive until registered
                DynamicTexture dynamicTexture = new DynamicTexture(image);
                Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
                textureCache.put(normalized, location);
                LOGGER.info("HeadCacheManager: Registered head texture: {}", location);
            } catch (Exception e) {
                LOGGER.error("HeadCacheManager: Failed to register head texture for {}: {}", nickname, e.getMessage(), e);
            }
        });
        
        return location;
    }
    
    /**
     * Clears the texture cache (but keeps files on disk).
     */
    public static void clearCache() {
        textureCache.clear();
        pendingDownloads.clear();
    }
}

