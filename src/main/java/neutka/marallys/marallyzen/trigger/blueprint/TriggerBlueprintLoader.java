package neutka.marallys.marallyzen.trigger.blueprint;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct blueprint loader for config/marallyzen/triggers/blueprints/*.json.
 */
public final class TriggerBlueprintLoader {
    private final Map<String, TriggerBlueprint> blueprints = new ConcurrentHashMap<>();
    private final Map<String, List<String>> validationErrors = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public synchronized void reloadBlueprints() {
        blueprints.clear();
        validationErrors.clear();
        Set<Path> directories = getBlueprintDirectories();
        Path primaryDir = getBlueprintDirectory();
        try {
            Files.createDirectories(primaryDir);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("TriggerBlueprintLoader: failed to create directory {}", primaryDir, e);
            loaded = true;
            return;
        }

        Map<String, TriggerBlueprint> loadedBlueprints = new LinkedHashMap<>();
        Map<String, Path> loadedFrom = new HashMap<>();
        for (Path dir : directories) {
            if (!Files.exists(dir)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : stream) {
                    String id = idFromFile(file);
                    if (loadedBlueprints.containsKey(id)) {
                        Marallyzen.LOGGER.warn(
                                "TriggerBlueprintLoader: duplicate blueprint id '{}' ignored from {} (already loaded from {})",
                                id,
                                file,
                                loadedFrom.get(id)
                        );
                        continue;
                    }
                    loadSingleFile(file, loadedBlueprints);
                    if (loadedBlueprints.containsKey(id)) {
                        loadedFrom.put(id, file);
                    }
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.error("TriggerBlueprintLoader: failed to read {}", dir, e);
            }
        }

        blueprints.putAll(loadedBlueprints);
        loaded = true;
        Marallyzen.LOGGER.info(
                "TriggerBlueprintLoader: loaded {} blueprint(s), {} file(s) with errors, searched directories: {}",
                blueprints.size(),
                validationErrors.size(),
                directories
        );
    }

    public synchronized void ensureLoaded() {
        if (!loaded) {
            reloadBlueprints();
        }
    }

    public TriggerBlueprint getBlueprint(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ensureLoaded();
        return blueprints.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> getLoadedBlueprintIds() {
        ensureLoaded();
        return Collections.unmodifiableSet(new TreeSet<>(blueprints.keySet()));
    }

    public Set<String> discoverBlueprintIds() {
        Set<String> ids = new TreeSet<>();
        for (Path dir : getBlueprintDirectories()) {
            if (!Files.exists(dir)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : stream) {
                    ids.add(idFromFile(file));
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.warn("TriggerBlueprintLoader: failed to discover ids in {}", dir, e);
            }
        }
        return ids;
    }

    public Path getBlueprintDirectory() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(Marallyzen.MODID)
                .resolve("triggers")
                .resolve("blueprints");
    }

    public Path getActiveDirectory() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(Marallyzen.MODID)
                .resolve("triggers")
                .resolve("active");
    }

    public Set<Path> getBlueprintDirectories() {
        LinkedHashSet<Path> dirs = new LinkedHashSet<>();
        Path primary = getBlueprintDirectory().toAbsolutePath().normalize();
        dirs.add(primary);

        Path projectConfig = Paths.get("config")
                .resolve(Marallyzen.MODID)
                .resolve("triggers")
                .resolve("blueprints")
                .toAbsolutePath()
                .normalize();
        if (!projectConfig.equals(primary)) {
            dirs.add(projectConfig);
        }
        return dirs;
    }

    private void loadSingleFile(Path file, Map<String, TriggerBlueprint> target) {
        String fallbackId = idFromFile(file);
        List<String> errors = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            TriggerBlueprint blueprint = TriggerBlueprint.fromJson(fallbackId, root, errors);
            if (blueprint == null || !errors.isEmpty()) {
                validationErrors.put(fallbackId, List.copyOf(errors));
                Marallyzen.LOGGER.warn("TriggerBlueprintLoader: {} has {} validation issue(s)", file.getFileName(), errors.size());
                for (String error : errors) {
                    Marallyzen.LOGGER.warn("TriggerBlueprintLoader: [{}] {}", fallbackId, error);
                }
                return;
            }
            target.put(blueprint.id().toLowerCase(Locale.ROOT), blueprint);
        } catch (Exception e) {
            errors.add(e.getMessage() != null ? e.getMessage() : "Unknown parse error");
            validationErrors.put(fallbackId, List.copyOf(errors));
            Marallyzen.LOGGER.error("TriggerBlueprintLoader: failed to parse {}", file.getFileName(), e);
        }
    }

    private String idFromFile(Path file) {
        String name = file.getFileName().toString();
        int idx = name.lastIndexOf('.');
        String id = idx > 0 ? name.substring(0, idx) : name;
        return id.toLowerCase(Locale.ROOT);
    }
}
