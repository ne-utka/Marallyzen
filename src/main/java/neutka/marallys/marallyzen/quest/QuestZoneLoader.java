package neutka.marallys.marallyzen.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.FileReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class QuestZoneLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private QuestZoneLoader() {
    }

    public static Map<String, QuestZoneDefinition> loadZones(Path directory) {
        Map<String, QuestZoneDefinition> result = new HashMap<>();
        if (directory == null || !Files.exists(directory)) {
            Marallyzen.LOGGER.info("QuestZoneLoader: directory missing {}", directory);
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path path : stream) {
                Marallyzen.LOGGER.info("QuestZoneLoader: loading {}", path.getFileName());
                QuestZoneDefinition zone = loadZone(path);
                if (zone == null) {
                    continue;
                }
                result.put(zone.id(), zone);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestZoneLoader: failed to read {}", directory, e);
        }
        return result;
    }

    private static QuestZoneDefinition loadZone(Path path) {
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            QuestZoneDefinition zone = QuestZoneDefinition.fromJson(obj);
            if (zone == null) {
                Marallyzen.LOGGER.warn("QuestZoneLoader: invalid zone file {}", path.getFileName());
                return null;
            }
            return zone;
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestZoneLoader: failed to parse {}", path.getFileName(), e);
            return null;
        }
    }
}
