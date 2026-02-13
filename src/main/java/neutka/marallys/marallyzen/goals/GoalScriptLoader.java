package neutka.marallys.marallyzen.goals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads goal scripts from config/marallyzen/goals/*.json.
 */
public final class GoalScriptLoader {
    private static final GoalScriptLoader INSTANCE = new GoalScriptLoader();

    private final Map<String, GoalScript> scripts = new ConcurrentHashMap<>();
    private final Map<String, List<String>> validationErrors = new ConcurrentHashMap<>();

    private volatile boolean loaded;

    private GoalScriptLoader() {
    }

    public static GoalScriptLoader getInstance() {
        return INSTANCE;
    }

    public synchronized void reloadScripts() {
        scripts.clear();
        validationErrors.clear();
        Set<Path> directories = getGoalsDirectories();
        Path primaryDir = getGoalsDirectory();
        try {
            Files.createDirectories(primaryDir);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("GoalScriptLoader: failed to create directory {}", primaryDir, e);
            loaded = true;
            return;
        }

        Map<String, GoalScript> loadedScripts = new LinkedHashMap<>();
        Map<String, Path> loadedFrom = new HashMap<>();
        for (Path dir : directories) {
            if (!Files.exists(dir)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : stream) {
                    String id = scriptIdFromFile(file);
                    if (loadedScripts.containsKey(id)) {
                        Path existing = loadedFrom.get(id);
                        Marallyzen.LOGGER.warn(
                                "GoalScriptLoader: duplicate script id '{}' ignored from {} (already loaded from {})",
                                id,
                                file,
                                existing
                        );
                        continue;
                    }
                    loadSingleFile(file, loadedScripts);
                    if (loadedScripts.containsKey(id)) {
                        loadedFrom.put(id, file);
                    }
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.error("GoalScriptLoader: failed to read scripts from {}", dir, e);
            }
        }

        scripts.putAll(loadedScripts);
        loaded = true;
        Marallyzen.LOGGER.info(
                "GoalScriptLoader: loaded {} goal script(s), {} file(s) with errors, searched directories: {}",
                scripts.size(),
                validationErrors.size(),
                directories
        );
    }

    public synchronized void ensureLoaded() {
        if (!loaded) {
            reloadScripts();
        }
    }

    public GoalScript getScript(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ensureLoaded();
        return scripts.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> getLoadedScriptIds() {
        ensureLoaded();
        return Collections.unmodifiableSet(new TreeSet<>(scripts.keySet()));
    }

    public Set<String> discoverScriptIds() {
        Set<String> ids = new TreeSet<>();
        for (Path dir : getGoalsDirectories()) {
            if (!Files.exists(dir)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : stream) {
                    ids.add(scriptIdFromFile(file));
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.warn("GoalScriptLoader: failed to discover script IDs in {}", dir, e);
            }
        }
        return ids;
    }

    public Map<String, List<String>> getValidationErrors() {
        ensureLoaded();
        Map<String, List<String>> snapshot = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : validationErrors.entrySet()) {
            snapshot.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return snapshot;
    }

    public Path getGoalsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("goals");
    }

    public Set<Path> getGoalsDirectories() {
        LinkedHashSet<Path> dirs = new LinkedHashSet<>();
        Path primary = getGoalsDirectory().toAbsolutePath().normalize();
        dirs.add(primary);

        Path projectConfig = Paths.get("config").resolve(Marallyzen.MODID).resolve("goals").toAbsolutePath().normalize();
        if (!projectConfig.equals(primary)) {
            dirs.add(projectConfig);
        }
        return dirs;
    }

    private void loadSingleFile(Path file, Map<String, GoalScript> target) {
        String scriptId = scriptIdFromFile(file);
        List<String> errors = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            GoalScript script = GoalScript.fromJson(scriptId, root, errors);
            if (script == null || !errors.isEmpty()) {
                validationErrors.put(scriptId, List.copyOf(errors));
                Marallyzen.LOGGER.warn("GoalScriptLoader: {} has {} validation issue(s)", file.getFileName(), errors.size());
                for (String error : errors) {
                    Marallyzen.LOGGER.warn("GoalScriptLoader: [{}] {}", scriptId, error);
                }
                return;
            }
            target.put(script.id().toLowerCase(Locale.ROOT), script);
        } catch (Exception e) {
            errors.add(e.getMessage() != null ? e.getMessage() : "Unknown parse error");
            validationErrors.put(scriptId, List.copyOf(errors));
            Marallyzen.LOGGER.error("GoalScriptLoader: failed to parse {}", file.getFileName(), e);
        }
    }

    private String scriptIdFromFile(Path file) {
        String name = file.getFileName().toString();
        int idx = name.lastIndexOf('.');
        String id = idx > 0 ? name.substring(0, idx) : name;
        return id.toLowerCase(Locale.ROOT);
    }
}
