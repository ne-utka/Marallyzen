package neutka.marallys.marallyzen.door;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class DoorRegistry {
    private final Map<String, DoorDefinition> doors = new ConcurrentHashMap<>();
    private final Map<String, List<String>> validationErrors = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public synchronized void reload() {
        doors.clear();
        validationErrors.clear();
        Path dir = getDoorDirectory();
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("DoorRegistry: failed to create directory {}", dir, e);
            loaded = true;
            return;
        }

        Map<String, DoorDefinition> loadedDoors = new LinkedHashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                String id = idFromFile(file);
                if (loadedDoors.containsKey(id)) {
                    Marallyzen.LOGGER.warn("DoorRegistry: duplicate door id '{}' ignored from {}", id, file);
                    continue;
                }
                loadSingleFile(file, loadedDoors);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.error("DoorRegistry: failed to read {}", dir, e);
        }

        doors.putAll(loadedDoors);
        loaded = true;
        Marallyzen.LOGGER.info("DoorRegistry: loaded {} door(s), {} file(s) with errors.", doors.size(), validationErrors.size());
    }

    public synchronized void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    public DoorDefinition getDoor(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ensureLoaded();
        return doors.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> getDoorIds() {
        ensureLoaded();
        return Collections.unmodifiableSet(new TreeSet<>(doors.keySet()));
    }

    public Path getDoorDirectory() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(Marallyzen.MODID)
                .resolve("doors");
    }

    public boolean deleteDoorFile(String doorId) {
        if (doorId == null || doorId.isBlank()) {
            return false;
        }
        Path file = getDoorDirectory().resolve(doorId.trim().toLowerCase(Locale.ROOT) + ".json");
        try {
            return Files.deleteIfExists(file);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("DoorRegistry: failed to delete {}", file, e);
            return false;
        }
    }

    private void loadSingleFile(Path file, Map<String, DoorDefinition> target) {
        String fallbackId = idFromFile(file);
        List<String> errors = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            DoorDefinition door = DoorDefinition.fromJson(fallbackId, root, errors);
            if (door == null || !errors.isEmpty()) {
                validationErrors.put(fallbackId, List.copyOf(errors));
                Marallyzen.LOGGER.warn("DoorRegistry: {} has {} validation issue(s)", file.getFileName(), errors.size());
                for (String error : errors) {
                    Marallyzen.LOGGER.warn("DoorRegistry: [{}] {}", fallbackId, error);
                }
                return;
            }
            target.put(door.id(), door);
        } catch (Exception e) {
            errors.add(e.getMessage() != null ? e.getMessage() : "Unknown parse error");
            validationErrors.put(fallbackId, List.copyOf(errors));
            Marallyzen.LOGGER.error("DoorRegistry: failed to parse {}", file.getFileName(), e);
        }
    }

    private String idFromFile(Path file) {
        String name = file.getFileName().toString();
        int idx = name.lastIndexOf('.');
        String id = idx > 0 ? name.substring(0, idx) : name;
        return id.toLowerCase(Locale.ROOT);
    }
}
