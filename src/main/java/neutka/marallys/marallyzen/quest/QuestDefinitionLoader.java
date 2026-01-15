package neutka.marallys.marallyzen.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class QuestDefinitionLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private QuestDefinitionLoader() {
    }

    public static Map<String, QuestDefinition> loadDefinitions(Path directory) {
        Map<String, QuestDefinition> result = new HashMap<>();
        if (directory == null || !Files.exists(directory)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path path : stream) {
                QuestDefinition definition = loadDefinition(path);
                if (definition == null) {
                    continue;
                }
                result.put(definition.id(), definition);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestDefinitionLoader: failed to read {}", directory, e);
        }
        return result;
    }

    private static QuestDefinition loadDefinition(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            QuestDefinition definition = QuestDefinition.fromJson(obj);
            if (definition == null || !definition.isValid()) {
                Marallyzen.LOGGER.warn("QuestDefinitionLoader: invalid quest file {}", path.getFileName());
                return null;
            }
            return definition;
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestDefinitionLoader: failed to parse {}", path.getFileName(), e);
            return null;
        }
    }
}
