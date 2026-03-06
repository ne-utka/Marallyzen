package neutka.marallys.marallyzen.npc.replay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader for replay scripts stored in config/marallyzen/npc_replay.
 */
public final class NpcReplayLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final NpcReplayLoader INSTANCE = new NpcReplayLoader();
    private static final int MAX_SCRIPT_BYTES = 4 * 1024 * 1024;

    private final Map<String, NpcReplayScript> scripts = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    private NpcReplayLoader() {
    }

    public static NpcReplayLoader getInstance() {
        return INSTANCE;
    }

    public synchronized void reload() {
        scripts.clear();
        Path dir = getDirectory();
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("NpcReplayLoader: failed to create directory {}", dir, e);
            loaded = true;
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                loadSingle(file);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.error("NpcReplayLoader: failed to scan {}", dir, e);
        }
        loaded = true;
        Marallyzen.LOGGER.info("NpcReplayLoader: loaded {} script(s)", scripts.size());
    }

    public synchronized void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    public NpcReplayScript get(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ensureLoaded();
        return scripts.get(normalize(id));
    }

    public Set<String> ids() {
        ensureLoaded();
        return Collections.unmodifiableSet(new TreeSet<>(scripts.keySet()));
    }

    public Map<String, NpcReplayScript> snapshot() {
        ensureLoaded();
        return Collections.unmodifiableMap(new HashMap<>(scripts));
    }

    public synchronized boolean save(NpcReplayScript script) {
        if (script == null || script.id() == null || script.id().isBlank()) {
            return false;
        }
        ensureLoaded();
        String id = normalize(script.id());
        NpcReplayScript normalizedScript = new NpcReplayScript(
                id,
                script.loop(),
                script.mode(),
                script.interpolate(),
                script.maxTicks(),
                script.sourceNpcId(),
                script.nextTriggerId(),
                script.frames()
        );
        Path file = getDirectory().resolve(id + ".json");
        try {
            Files.createDirectories(file.getParent());
            String jsonString = GSON.toJson(normalizedScript.toJson());
            byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_SCRIPT_BYTES) {
                Marallyzen.LOGGER.warn(
                        "NpcReplayLoader: refusing to save script {} because size {} exceeds {} bytes",
                        id,
                        bytes.length,
                        MAX_SCRIPT_BYTES
                );
                return false;
            }
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(jsonString);
            }
            scripts.put(id, normalizedScript);
            return true;
        } catch (Exception e) {
            Marallyzen.LOGGER.error("NpcReplayLoader: failed to save script {}", id, e);
            return false;
        }
    }

    public synchronized boolean delete(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        ensureLoaded();
        String normalized = normalize(id);
        Path file = getDirectory().resolve(normalized + ".json");
        boolean removed = false;
        try {
            removed = Files.deleteIfExists(file);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("NpcReplayLoader: failed to delete script {}", normalized, e);
            return false;
        }
        scripts.remove(normalized);
        return removed;
    }

    public Path getDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("npc_replay");
    }

    private void loadSingle(Path file) {
        String fallbackId = normalize(file.getFileName().toString().replace(".json", ""));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            NpcReplayScript script = NpcReplayScript.fromJson(root);
            if (script == null) {
                Marallyzen.LOGGER.warn("NpcReplayLoader: invalid script file {}", file.getFileName());
                return;
            }
            scripts.put(normalize(script.id()), script);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("NpcReplayLoader: failed to parse {}", file.getFileName(), e);
            scripts.remove(fallbackId);
        }
    }

    private String normalize(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
