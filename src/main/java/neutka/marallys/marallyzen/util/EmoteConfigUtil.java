package neutka.marallys.marallyzen.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class EmoteConfigUtil {
    private static final Map<String, Integer> DURATION_CACHE = new HashMap<>();

    private EmoteConfigUtil() {
    }

    public static int getEmoteDurationTicks(String emoteId) {
        if (emoteId == null || emoteId.isBlank()) {
            return 20;
        }
        String key = normalizeName(emoteId);
        Integer cached = DURATION_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int ticks = 20;
        Path path = resolveEmotePath(key);
        if (path != null && Files.isRegularFile(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                ticks = parseDurationTicks(json, key);
            } catch (IOException e) {
                Marallyzen.LOGGER.warn("EmoteConfigUtil: failed to read emote file {}: {}", path, e.getMessage());
            }
        } else {
            Marallyzen.LOGGER.warn("EmoteConfigUtil: emote file not found for {}", emoteId);
        }
        DURATION_CACHE.put(key, ticks);
        return ticks;
    }

    public static Path resolveEmotePath(String emoteName) {
        String fileName = emoteName;
        if (!fileName.endsWith(".json")) {
            fileName += ".json";
        }
        return getConfigEmoteDir().resolve(fileName);
    }

    public static Path getConfigEmoteDir() {
        return FMLPaths.CONFIGDIR.get().resolve("marallyzen").resolve("emotes");
    }

    private static int parseDurationTicks(String json, String emoteName) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return 20;
            }
            JsonObject obj = root.getAsJsonObject();
            JsonObject animations = obj.getAsJsonObject("animations");
            if (animations == null || animations.isEmpty()) {
                return 20;
            }
            JsonObject anim = null;
            if (animations.has(emoteName)) {
                anim = animations.getAsJsonObject(emoteName);
            } else {
                String firstKey = animations.keySet().iterator().next();
                anim = animations.getAsJsonObject(firstKey);
            }
            if (anim == null || !anim.has("animation_length")) {
                return 20;
            }
            double seconds = anim.get("animation_length").getAsDouble();
            return Math.max(1, (int) Math.round(seconds * 20.0));
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("EmoteConfigUtil: failed to parse emote duration for {}: {}", emoteName, e.getMessage());
            return 20;
        }
    }

    private static String normalizeName(String emoteId) {
        String name = emoteId.trim().toLowerCase().replace(' ', '_');
        if (name.endsWith(".json")) {
            name = name.substring(0, name.length() - ".json".length());
        }
        return name;
    }
}
