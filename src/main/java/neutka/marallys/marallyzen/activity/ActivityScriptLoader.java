package neutka.marallys.marallyzen.activity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class ActivityScriptLoader {
    private final Map<String, Map<String, ActivityScript>> scripts = new ConcurrentHashMap<>();
    private final Map<String, java.util.List<String>> validationErrors = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public synchronized void reload() {
        scripts.clear();
        validationErrors.clear();
        Path root = getRootDirectory();
        try {
            Files.createDirectories(root);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("ActivityScriptLoader: failed to create directory {}", root, e);
            loaded = true;
            return;
        }

        try (DirectoryStream<Path> types = Files.newDirectoryStream(root)) {
            for (Path typeDir : types) {
                if (!Files.isDirectory(typeDir)) {
                    continue;
                }
                String type = typeDir.getFileName().toString().toLowerCase(Locale.ROOT);
                loadTypeDir(type, typeDir);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("ActivityScriptLoader: failed to scan {}", root, e);
        }
        loaded = true;
        Marallyzen.LOGGER.info("ActivityScriptLoader: loaded {} type(s), {} file(s) with errors",
                scripts.size(), validationErrors.size());
    }

    public synchronized void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    public ActivityScript getScript(String type, String id) {
        if (type == null || type.isBlank() || id == null || id.isBlank()) {
            return null;
        }
        ensureLoaded();
        Map<String, ActivityScript> byType = scripts.get(normalize(type));
        if (byType == null) {
            return null;
        }
        return byType.get(normalize(id));
    }

    public Set<String> getTypes() {
        ensureLoaded();
        return Collections.unmodifiableSet(new TreeSet<>(scripts.keySet()));
    }

    public Set<String> getScriptIds(String type) {
        if (type == null || type.isBlank()) {
            return Set.of();
        }
        ensureLoaded();
        Map<String, ActivityScript> byType = scripts.get(normalize(type));
        if (byType == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new TreeSet<>(byType.keySet()));
    }

    public Path getRootDirectory() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(Marallyzen.MODID)
                .resolve("activities");
    }

    private void loadTypeDir(String type, Path dir) {
        Map<String, ActivityScript> loadedScripts = new TreeMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                String id = idFromFile(file);
                if (loadedScripts.containsKey(id)) {
                    Marallyzen.LOGGER.warn("ActivityScriptLoader: duplicate script '{}' ignored from {}", id, file);
                    continue;
                }
                ActivityScript script = loadSingleFile(type, id, file);
                if (script != null) {
                    loadedScripts.put(id, script);
                }
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("ActivityScriptLoader: failed to read {}", dir, e);
        }
        if (!loadedScripts.isEmpty()) {
            scripts.put(normalize(type), Map.copyOf(loadedScripts));
        }
    }

    private ActivityScript loadSingleFile(String type, String fallbackId, Path file) {
        java.util.List<String> errors = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            ActivityScript script = ActivityScript.fromJson(fallbackId, type, root, errors);
            if (script == null || !errors.isEmpty()) {
                validationErrors.put(type + "/" + fallbackId, java.util.List.copyOf(errors));
                Marallyzen.LOGGER.warn("ActivityScriptLoader: {} has {} validation issue(s)", file.getFileName(), errors.size());
                for (String error : errors) {
                    Marallyzen.LOGGER.warn("ActivityScriptLoader: [{}] {}", fallbackId, error);
                }
                return null;
            }
            return script;
        } catch (Exception e) {
            errors.add(e.getMessage() != null ? e.getMessage() : "Unknown parse error");
            validationErrors.put(type + "/" + fallbackId, java.util.List.copyOf(errors));
            Marallyzen.LOGGER.warn("ActivityScriptLoader: failed to parse {}", file.getFileName(), e);
            return null;
        }
    }

    private String idFromFile(Path file) {
        String name = file.getFileName().toString();
        int idx = name.lastIndexOf('.');
        String id = idx > 0 ? name.substring(0, idx) : name;
        return normalize(id);
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
