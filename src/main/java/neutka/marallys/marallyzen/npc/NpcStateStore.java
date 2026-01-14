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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class NpcStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "npcs_state.json";

    private NpcStateStore() {
    }

    public static Map<String, NpcState> load() {
        Path path = getStatePath();
        if (!Files.exists(path)) {
            return Collections.emptyMap();
        }
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("npcs")) {
                return Collections.emptyMap();
            }
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
        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to save NPC state file: {}", path, e);
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
