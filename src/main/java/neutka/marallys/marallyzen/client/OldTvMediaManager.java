package neutka.marallys.marallyzen.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OldTvMediaManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path MEDIA_DIR = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("tv");
    private static final Path STATE_PATH = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("tv_bindings.json");
    private static final int DEFAULT_FPS = 12;

    private static final Map<String, String> bindings = new HashMap<>();
    private static final Map<String, Media> cache = new HashMap<>();
    private static boolean loaded;

    private static boolean bindMode;
    private static String selectedMedia;

    private OldTvMediaManager() {
    }

    public static void enableBindMode(String mediaName) {
        ensureLoaded();
        selectedMedia = normalizeMediaName(mediaName);
        bindMode = selectedMedia != null;
    }

    public static void disableBindMode() {
        bindMode = false;
        selectedMedia = null;
    }

    public static boolean isBindMode() {
        return bindMode;
    }

    public static String getSelectedMedia() {
        return selectedMedia;
    }

    public static void bind(BlockPos pos, net.minecraft.resources.ResourceKey<Level> dimension) {
        if (!bindMode || selectedMedia == null) {
            return;
        }
        bindMedia(pos, dimension, selectedMedia);
    }

    public static boolean bindMedia(BlockPos pos, net.minecraft.resources.ResourceKey<Level> dimension, String mediaName) {
        if (mediaName == null || mediaName.isBlank()) {
            return false;
        }
        String key = buildKey(dimension, pos);
        bindings.put(key, normalizeMediaName(mediaName));
        saveState();
        return true;
    }

    public static ResourceLocation getTexture(BlockPos pos, net.minecraft.resources.ResourceKey<Level> dimension, long gameTime) {
        ensureLoaded();
        String key = buildKey(dimension, pos);
        String mediaName = bindings.get(key);
        if (mediaName == null) {
            return null;
        }
        Media media = cache.computeIfAbsent(mediaName, OldTvMediaManager::loadMedia);
        if (media == null || media.frames.isEmpty()) {
            return null;
        }
        if (!media.animated) {
            return media.frames.get(0);
        }
        int frameIndex = (int) ((gameTime / media.ticksPerFrame) % media.frames.size());
        return media.frames.get(frameIndex);
    }

    public static MediaState getMediaState(BlockPos pos, net.minecraft.resources.ResourceKey<Level> dimension) {
        ensureLoaded();
        String key = buildKey(dimension, pos);
        String mediaName = bindings.get(key);
        if (mediaName == null) {
            return null;
        }
        Media media = cache.computeIfAbsent(mediaName, OldTvMediaManager::loadMedia);
        if (media == null) {
            return null;
        }
        return new MediaState(media.animated, media.soundId);
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            Files.createDirectories(MEDIA_DIR);
        } catch (IOException e) {
            Marallyzen.LOGGER.warn("OldTvMediaManager: failed to create media dir {}", MEDIA_DIR, e);
        }
        loadState();
    }

    private static void loadState() {
        if (!Files.exists(STATE_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(STATE_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            JsonArray array = root.getAsJsonArray("bindings");
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();
                    String key = obj.get("key").getAsString();
                    String media = obj.get("media").getAsString();
                    bindings.put(key, media);
                }
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("OldTvMediaManager: failed to read {}", STATE_PATH, e);
        }
    }

    private static void saveState() {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", entry.getKey());
            obj.addProperty("media", entry.getValue());
            array.add(obj);
        }
        root.add("bindings", array);
        try {
            Files.createDirectories(STATE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(STATE_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("OldTvMediaManager: failed to write {}", STATE_PATH, e);
        }
    }

    private static String buildKey(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static String normalizeMediaName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private static Media loadMedia(String mediaName) {
        Path file = MEDIA_DIR.resolve(mediaName);
        if (!Files.exists(file)) {
            String fallback = mediaName + ".png";
            Path fallbackFile = MEDIA_DIR.resolve(fallback);
            if (Files.exists(fallbackFile)) {
                file = fallbackFile;
            }
        }

        try {
            if (Files.isDirectory(file)) {
                return loadSequence(mediaName, file);
            }
            return loadSingle(mediaName, file);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("OldTvMediaManager: failed to load media {}", mediaName, e);
            return null;
        }
    }

    private static Media loadSingle(String mediaName, Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        var image = com.mojang.blaze3d.platform.NativeImage.read(new BufferedInputStream(Files.newInputStream(file)));
        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation id = textures.register("tv/" + sanitize(mediaName), texture);
        return new Media(false, List.of(id), 1, null);
    }

    private static Media loadSequence(String mediaName, Path dir) throws IOException {
        List<Path> frames = Files.list(dir)
                .filter(path -> {
                    String lower = path.getFileName().toString().toLowerCase();
                    return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
                })
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
        if (frames.isEmpty()) {
            return null;
        }
        MediaMeta meta = readMediaMeta(mediaName);
        if (meta.soundId == null) {
            meta = new MediaMeta(meta.fps, defaultSoundId(mediaName));
        }
        int ticksPerFrame = Math.max(1, 20 / meta.fps);
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        List<ResourceLocation> ids = new ArrayList<>();
        int index = 0;
        for (Path frame : frames) {
            var image = com.mojang.blaze3d.platform.NativeImage.read(new BufferedInputStream(Files.newInputStream(frame)));
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation id = textures.register("tv/" + sanitize(mediaName) + "/" + index, texture);
            ids.add(id);
            index++;
        }
        return new Media(true, ids, ticksPerFrame, meta.soundId);
    }

    private static MediaMeta readMediaMeta(String mediaName) {
        Path meta = MEDIA_DIR.resolve(mediaName + ".json");
        if (!Files.exists(meta)) {
            return new MediaMeta(DEFAULT_FPS, null);
        }
        try (Reader reader = Files.newBufferedReader(meta)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj == null) {
                return new MediaMeta(DEFAULT_FPS, null);
            }
            int fps = DEFAULT_FPS;
            if (obj.has("fps")) {
                fps = Math.max(1, obj.get("fps").getAsInt());
            }
            String soundId = null;
            if (obj.has("sound")) {
                String raw = obj.get("sound").getAsString();
                if (!StringUtil.isNullOrEmpty(raw)) {
                    soundId = raw;
                }
            }
            return new MediaMeta(fps, soundId);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("OldTvMediaManager: failed to read fps meta {}", meta, e);
            return new MediaMeta(DEFAULT_FPS, null);
        }
    }

    private static String sanitize(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9/_-]", "_");
    }

    private static String defaultSoundId(String mediaName) {
        String sanitized = sanitize(mediaName).toLowerCase();
        if (StringUtil.isNullOrEmpty(sanitized)) {
            return Marallyzen.MODID + ":tv/default";
        }
        return Marallyzen.MODID + ":tv/" + sanitized;
    }

    public static Component buildBindNarration(String mediaName) {
        Component coloredName = Component.literal(mediaName)
                .withStyle(style -> style.withColor(TextColor.fromRgb(0xD48E03)));
        return Component.translatable("narration.marallyzen.tv_bind", coloredName);
    }

    public record MediaState(boolean animated, String soundId) {}

    private record MediaMeta(int fps, String soundId) {}

    private record Media(boolean animated, List<ResourceLocation> frames, int ticksPerFrame, String soundId) {}
}

