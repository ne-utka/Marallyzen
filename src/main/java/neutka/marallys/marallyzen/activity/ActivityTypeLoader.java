package neutka.marallys.marallyzen.activity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ActivityTypeLoader {
    private final Map<String, String> types = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public synchronized void reload() {
        types.clear();
        Path file = getConfigFile();
        if (!Files.exists(file)) {
            Marallyzen.LOGGER.error("ActivityTypeLoader: missing {}", file);
            loaded = true;
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject obj = root.has("types") && root.get("types").isJsonObject()
                    ? root.getAsJsonObject("types")
                    : root;
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = normalize(entry.getKey());
                if (key.isBlank()) {
                    continue;
                }
                JsonElement value = entry.getValue();
                if (value == null || !value.isJsonPrimitive()) {
                    Marallyzen.LOGGER.warn("ActivityTypeLoader: invalid executor for type '{}'", key);
                    continue;
                }
                String className = value.getAsString();
                if (className == null || className.isBlank()) {
                    Marallyzen.LOGGER.warn("ActivityTypeLoader: empty executor for type '{}'", key);
                    continue;
                }
                types.put(key, className.trim());
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.error("ActivityTypeLoader: failed to read {}", file, e);
        }
        loaded = true;
    }

    public synchronized void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    public Map<String, String> types() {
        ensureLoaded();
        return Map.copyOf(types);
    }

    public Path getConfigFile() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(Marallyzen.MODID)
                .resolve("activity_types.json");
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
