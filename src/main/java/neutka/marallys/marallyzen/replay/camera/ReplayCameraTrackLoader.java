package neutka.marallys.marallyzen.replay.camera;

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
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

public final class ReplayCameraTrackLoader {
    private static final Gson GSON = new Gson();
    private static final Map<String, ReplayCameraTrack> TRACKS = new HashMap<>();

    private ReplayCameraTrackLoader() {
    }

    public static void loadTracks() {
        TRACKS.clear();

        Path tracksDir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("replay_camera");
        if (!Files.exists(tracksDir)) {
            try {
                Files.createDirectories(tracksDir);
                Marallyzen.LOGGER.info("Created replay camera tracks directory: {}", tracksDir);
            } catch (IOException e) {
                Marallyzen.LOGGER.error("Failed to create replay camera tracks directory", e);
                return;
            }
        }

        File[] jsonFiles = tracksDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null) {
            return;
        }

        for (File file : jsonFiles) {
            try {
                ReplayCameraTrack track = loadTrackFromFile(file);
                if (track != null) {
                    TRACKS.put(track.getId(), track);
                    Marallyzen.LOGGER.info("Loaded replay camera track: {} from {}", track.getId(), file.getName());
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to load replay camera track: {}", file.getName(), e);
            }
        }
    }

    public static ReplayCameraTrack getTrack(String id) {
        return TRACKS.get(id);
    }

    public static Map<String, ReplayCameraTrack> getAllTracks() {
        return new HashMap<>(TRACKS);
    }

    private static ReplayCameraTrack loadTrackFromFile(File file) throws IOException {
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
            CameraEase defaultEase = json.has("easing")
                ? CameraEase.fromString(json.get("easing").getAsString())
                : CameraEase.LINEAR;

            JsonArray keyframesArray = json.getAsJsonArray("keyframes");
            JsonArray cameraArray = json.getAsJsonArray("camera");
            if ((keyframesArray == null || keyframesArray.isEmpty()) && (cameraArray == null || cameraArray.isEmpty())) {
                return null;
            }

            java.util.List<ReplayCameraKeyframe> keyframes = new java.util.ArrayList<>();
            JsonArray sourceArray = keyframesArray != null && !keyframesArray.isEmpty() ? keyframesArray : cameraArray;
            for (JsonElement element : sourceArray) {
                JsonObject kfJson = element.getAsJsonObject();
                if (!kfJson.has("time")) {
                    continue;
                }
                double time = kfJson.get("time").getAsDouble();
                Vec3 position = readVec3(kfJson, "pos", "position");
                float yaw = kfJson.has("yaw") ? kfJson.get("yaw").getAsFloat() : 0.0f;
                float pitch = kfJson.has("pitch") ? kfJson.get("pitch").getAsFloat() : 0.0f;
                if (kfJson.has("rot")) {
                    float[] rot = readRotation(kfJson.get("rot"));
                    pitch = rot[0];
                    yaw = rot[1];
                } else if (kfJson.has("rotation")) {
                    float[] rot = readRotation(kfJson.get("rotation"));
                    pitch = rot[0];
                    yaw = rot[1];
                }
                float fov = kfJson.has("fov") ? kfJson.get("fov").getAsFloat() : 70.0f;
                CameraEase ease = kfJson.has("ease")
                    ? CameraEase.fromString(kfJson.get("ease").getAsString())
                    : null;

                keyframes.add(new ReplayCameraKeyframe(time, position, yaw, pitch, fov, ease));
            }

            if (keyframes.isEmpty()) {
                return null;
            }

            return new ReplayCameraTrack(id, loop, duration, defaultEase, keyframes);
        }
    }

    private static Vec3 readVec3(JsonObject json, String primaryKey, String fallbackKey) {
        JsonElement element = json.has(primaryKey) ? json.get(primaryKey) : json.get(fallbackKey);
        if (element == null) {
            return Vec3.ZERO;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() >= 3) {
                return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            return new Vec3(obj.get("x").getAsDouble(), obj.get("y").getAsDouble(), obj.get("z").getAsDouble());
        }
        return Vec3.ZERO;
    }

    private static float[] readRotation(JsonElement element) {
        if (element != null && element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() >= 2) {
                return new float[] { array.get(0).getAsFloat(), array.get(1).getAsFloat() };
            }
        }
        return new float[] { 0.0f, 0.0f };
    }
}
