package neutka.marallys.marallyzen.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NpcStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "npcs_state.json";

    private NpcStateStore() {
    }

    public static Map<String, NpcState> load() {
        Path path = getStatePath();
        JsonObject root = readRoot(path);
        if (root == null || !root.has("npcs")) {
            return Collections.emptyMap();
        }
        try {
            Map<String, NpcState> result = new HashMap<>();
            JsonObject npcs = root.getAsJsonObject("npcs");
            for (String id : npcs.keySet()) {
                JsonObject entry = npcs.getAsJsonObject(id);
                NpcState state = NpcState.fromJson(entry);
                if (state != null) {
                    result.put(id, state);
                }
            }
            return result;
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to load NPC state file: {}", path, e);
            return Collections.emptyMap();
        }
    }

    public static void save(Map<String, NpcState> states) {
        Path path = getStatePath();
        try {
            Files.createDirectories(path.getParent());
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to create NPC state directory: {}", path.getParent(), e);
        }
        JsonObject root = new JsonObject();
        JsonObject npcs = new JsonObject();
        for (Map.Entry<String, NpcState> entry : states.entrySet()) {
            npcs.add(entry.getKey(), entry.getValue().toJson());
        }
        root.add("npcs", npcs);
        List<String> disabled = new ArrayList<>(loadDisabled());
        if (!disabled.isEmpty()) {
            com.google.gson.JsonArray disabledArray = new com.google.gson.JsonArray();
            for (String id : disabled) {
                disabledArray.add(id);
            }
            root.add("disabled", disabledArray);
        }
        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to save NPC state file: {}", path, e);
        }
    }

    public static Set<String> loadDisabled() {
        Path path = getStatePath();
        JsonObject root = readRoot(path);
        if (root == null || !root.has("disabled")) {
            return Collections.emptySet();
        }
        try {
            Set<String> result = new HashSet<>();
            var disabled = root.getAsJsonArray("disabled");
            for (var entry : disabled) {
                if (entry != null && entry.isJsonPrimitive()) {
                    String id = entry.getAsString();
                    if (id != null && !id.isBlank()) {
                        result.add(id);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to load disabled NPC list: {}", path, e);
            return Collections.emptySet();
        }
    }

    public static void addDisabled(String npcId) {
        if (npcId == null || npcId.isEmpty()) {
            return;
        }
        Set<String> disabled = new HashSet<>(loadDisabled());
        if (!disabled.add(npcId)) {
            return;
        }
        saveWithDisabled(disabled);
    }

    public static void removeDisabled(String npcId) {
        if (npcId == null || npcId.isEmpty()) {
            return;
        }
        Set<String> disabled = new HashSet<>(loadDisabled());
        if (!disabled.remove(npcId)) {
            return;
        }
        saveWithDisabled(disabled);
    }

    private static void saveWithDisabled(Set<String> disabled) {
        Map<String, NpcState> states = load();
        Path path = getStatePath();
        try {
            Files.createDirectories(path.getParent());
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to create NPC state directory: {}", path.getParent(), e);
        }
        JsonObject root = new JsonObject();
        JsonObject npcs = new JsonObject();
        for (Map.Entry<String, NpcState> entry : states.entrySet()) {
            npcs.add(entry.getKey(), entry.getValue().toJson());
        }
        root.add("npcs", npcs);
        if (disabled != null && !disabled.isEmpty()) {
            com.google.gson.JsonArray disabledArray = new com.google.gson.JsonArray();
            for (String id : disabled) {
                disabledArray.add(id);
            }
            root.add("disabled", disabledArray);
        }
        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to save NPC state file: {}", path, e);
        }
    }

    private static JsonObject readRoot(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try (FileReader reader = new FileReader(path.toFile())) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to read NPC state file: {}", path, e);
            return null;
        }
    }

    private static Path getStatePath() {
        return FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve(FILE_NAME);
    }

    public record NpcState(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("dimension", dimension.location().toString());
            obj.addProperty("x", x);
            obj.addProperty("y", y);
            obj.addProperty("z", z);
            obj.addProperty("yaw", yaw);
            obj.addProperty("pitch", pitch);
            return obj;
        }

        static NpcState fromJson(JsonObject obj) {
            if (obj == null || !obj.has("dimension")) {
                return null;
            }
            ResourceLocation dimId = ResourceLocation.parse(obj.get("dimension").getAsString());
            ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
            double x = obj.has("x") ? obj.get("x").getAsDouble() : 0.0;
            double y = obj.has("y") ? obj.get("y").getAsDouble() : 0.0;
            double z = obj.has("z") ? obj.get("z").getAsDouble() : 0.0;
            float yaw = obj.has("yaw") ? obj.get("yaw").getAsFloat() : 0.0f;
            float pitch = obj.has("pitch") ? obj.get("pitch").getAsFloat() : 0.0f;
            return new NpcState(dimension, x, y, z, yaw, pitch);
        }
    }
}
