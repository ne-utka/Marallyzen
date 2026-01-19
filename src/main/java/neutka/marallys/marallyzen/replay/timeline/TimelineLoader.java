package neutka.marallys.marallyzen.replay.timeline;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

public final class TimelineLoader {
    private static final Gson GSON = new Gson();
    private static final Map<String, TimelineTrack> TRACKS = new HashMap<>();

    private TimelineLoader() {
    }

    public static void loadTracks() {
        TRACKS.clear();

        Path timelinesDir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("replay_timeline");
        if (!Files.exists(timelinesDir)) {
            try {
                Files.createDirectories(timelinesDir);
                Marallyzen.LOGGER.info("Created replay timeline directory: {}", timelinesDir);
            } catch (IOException e) {
                Marallyzen.LOGGER.error("Failed to create replay timeline directory", e);
                return;
            }
        }

        File[] jsonFiles = timelinesDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null) {
            return;
        }

        for (File file : jsonFiles) {
            try {
                TimelineTrack track = loadTrackFromFile(file);
                if (track != null) {
                    TRACKS.put(track.getId(), track);
                    Marallyzen.LOGGER.info("Loaded replay timeline: {} from {}", track.getId(), file.getName());
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to load replay timeline: {}", file.getName(), e);
            }
        }
    }

    public static TimelineTrack getTrack(String id) {
        return TRACKS.get(id);
    }

    public static Map<String, TimelineTrack> getAllTracks() {
        return new HashMap<>(TRACKS);
    }

    private static TimelineTrack loadTrackFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return null;
            }

            String id = json.has("id") ? json.get("id").getAsString() : "";
            if (id == null || id.isEmpty()) {
                String fileName = file.getName();
                id = fileName.substring(0, fileName.lastIndexOf('.'));
            }

            boolean loop = json.has("loop") && json.get("loop").getAsBoolean();
            double duration = json.has("duration") ? json.get("duration").getAsDouble() : 0.0;

            JsonArray eventsArray = json.getAsJsonArray("events");
            if (eventsArray == null || eventsArray.isEmpty()) {
                return null;
            }

            java.util.List<TimelineEvent> events = new java.util.ArrayList<>();
            for (JsonElement element : eventsArray) {
                JsonObject evJson = element.getAsJsonObject();
                if (!evJson.has("time") || !evJson.has("type")) {
                    continue;
                }
                double time = evJson.get("time").getAsDouble();
                String type = evJson.get("type").getAsString();
                String target = evJson.has("target") ? evJson.get("target").getAsString() : null;
                String value = evJson.has("value") ? evJson.get("value").getAsString() : null;

                JsonObject dataCopy = evJson.deepCopy();
                dataCopy.remove("time");
                dataCopy.remove("type");
                dataCopy.remove("target");
                dataCopy.remove("value");

                events.add(new TimelineEvent(time, type, target, value, dataCopy));
            }

            if (events.isEmpty()) {
                return null;
            }

            return new TimelineTrack(id, loop, duration, events);
        }
    }
}
