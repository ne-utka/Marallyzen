package neutka.marallys.marallyzen.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuestStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "quests_state.json";

    private QuestStateStore() {
    }

    public static Map<UUID, QuestPlayerData> load() {
        Path path = getStatePath();
        if (!Files.exists(path)) {
            return Collections.emptyMap();
        }
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("players")) {
                return Collections.emptyMap();
            }
            Map<UUID, QuestPlayerData> result = new HashMap<>();
            JsonObject players = root.getAsJsonObject("players");
            for (String uuidStr : players.keySet()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    QuestPlayerData data = QuestPlayerData.fromJson(players.getAsJsonObject(uuidStr));
                    result.put(uuid, data);
                } catch (IllegalArgumentException ignored) {
                    Marallyzen.LOGGER.warn("QuestStateStore: invalid UUID {}", uuidStr);
                }
            }
            return result;
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to load quest state file: {}", path, e);
            return Collections.emptyMap();
        }
    }

    public static void save(Map<UUID, QuestPlayerData> data) {
        Path path = getStatePath();
        try {
            Files.createDirectories(path.getParent());
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to create quest state directory: {}", path.getParent(), e);
        }
        JsonObject root = new JsonObject();
        JsonObject players = new JsonObject();
        for (Map.Entry<UUID, QuestPlayerData> entry : data.entrySet()) {
            players.add(entry.getKey().toString(), entry.getValue().toJson());
        }
        root.add("players", players);
        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to save quest state file: {}", path, e);
        }
    }

    private static Path getStatePath() {
        return FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve(FILE_NAME);
    }
}
