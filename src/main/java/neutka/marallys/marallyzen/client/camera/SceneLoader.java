package neutka.marallys.marallyzen.client.camera;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;

import neutka.marallys.marallyzen.Marallyzen;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads scene configurations from JSON files.
 */
public class SceneLoader {
    private static final Map<String, SceneData> scenes = new HashMap<>();
    private static final Gson gson = new Gson();

    /**
     * Loads all scenes from config/marallyzen/scenes/ directory.
     */
    public static void loadScenes() {
        scenes.clear();

        Path scenesDir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("scenes");

        if (!Files.exists(scenesDir)) {
            try {
                Files.createDirectories(scenesDir);
                Marallyzen.LOGGER.info("Created scenes directory: {}", scenesDir);
            } catch (IOException e) {
                Marallyzen.LOGGER.error("Failed to create scenes directory", e);
                return;
            }
        }

        File[] jsonFiles = scenesDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null) {
            return;
        }

        for (File file : jsonFiles) {
            try {
                SceneData sceneData = loadSceneFromFile(file);
                if (sceneData != null) {
                    scenes.put(sceneData.getId(), sceneData);
                    Marallyzen.LOGGER.info("Loaded scene: {} from {}", sceneData.getId(), file.getName());
                }
            } catch (Exception e) {
                Marallyzen.LOGGER.error("Failed to load scene from file: {}", file.getName(), e);
            }
        }
    }

    /**
     * Gets a scene by ID.
     */
    public static SceneData getScene(String sceneId) {
        return scenes.get(sceneId);
    }

    /**
     * Gets all loaded scenes.
     */
    public static Map<String, SceneData> getAllScenes() {
        return new HashMap<>(scenes);
    }

    private static SceneData loadSceneFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            String id = json.get("id").getAsString();
            if (id == null || id.isEmpty()) {
                // Use filename without extension as ID
                String fileName = file.getName();
                id = fileName.substring(0, fileName.lastIndexOf('.'));
            }

            SceneData sceneData = new SceneData(id);

            // Load scene settings
            if (json.has("loop")) {
                sceneData.setLoop(json.get("loop").getAsBoolean());
            }

            if (json.has("interpolationSpeed")) {
                sceneData.setInterpolationSpeed(json.get("interpolationSpeed").getAsFloat());
            }

            // Load keyframes
            if (json.has("keyframes")) {
                JsonArray keyframesArray = json.getAsJsonArray("keyframes");
                for (JsonElement element : keyframesArray) {
                    JsonObject kfJson = element.getAsJsonObject();
                    
                    // Check keyframe type
                    String type = kfJson.has("type") ? kfJson.get("type").getAsString() : "CAMERA";
                    
                    if ("AUDIO".equals(type)) {
                        // Parse audio keyframe
                        String filePath = kfJson.get("file").getAsString();
                        boolean positional = kfJson.has("positional") ? kfJson.get("positional").getAsBoolean() : false;
                        Vec3 position = null;
                        float radius = 10.0f;
                        
                        if (positional && kfJson.has("position")) {
                            JsonObject posJson = kfJson.getAsJsonObject("position");
                            double x = posJson.get("x").getAsDouble();
                            double y = posJson.get("y").getAsDouble();
                            double z = posJson.get("z").getAsDouble();
                            position = new Vec3(x, y, z);
                        }
                        
                        if (kfJson.has("radius")) {
                            radius = kfJson.get("radius").getAsFloat();
                        }
                        
                        boolean block = kfJson.has("block") ? kfJson.get("block").getAsBoolean() : false;
                        
                        sceneData.addAudioKeyframe(new SceneData.AudioKeyframe(filePath, positional, position, radius, block));
                    } else if ("ACTOR".equals(type)) {
                        Vec3 position = null;
                        if (kfJson.has("position")) {
                            JsonElement posElement = kfJson.get("position");
                            if (posElement.isJsonObject()) {
                                JsonObject posJson = posElement.getAsJsonObject();
                                double x = posJson.get("x").getAsDouble();
                                double y = posJson.get("y").getAsDouble();
                                double z = posJson.get("z").getAsDouble();
                                position = new Vec3(x, y, z);
                            } else if (posElement.isJsonArray()) {
                                JsonArray posArray = posElement.getAsJsonArray();
                                if (posArray.size() >= 3) {
                                    double x = posArray.get(0).getAsDouble();
                                    double y = posArray.get(1).getAsDouble();
                                    double z = posArray.get(2).getAsDouble();
                                    position = new Vec3(x, y, z);
                                }
                            }
                        }
                        float yaw = kfJson.has("yaw") ? kfJson.get("yaw").getAsFloat() : 0.0f;
                        float pitch = kfJson.has("pitch") ? kfJson.get("pitch").getAsFloat() : 0.0f;
                        long duration = kfJson.has("duration") ? kfJson.get("duration").getAsLong() : 0L;
                        sceneData.addActorKeyframe(position, yaw, pitch, duration);
                    } else {
                        // Parse camera keyframe (default)
                        // Position
                        Vec3 position = null;
                        if (kfJson.has("position")) {
                            JsonElement posElement = kfJson.get("position");
                            if (posElement.isJsonObject()) {
                                JsonObject posJson = posElement.getAsJsonObject();
                                double x = posJson.get("x").getAsDouble();
                                double y = posJson.get("y").getAsDouble();
                                double z = posJson.get("z").getAsDouble();
                                position = new Vec3(x, y, z);
                            } else if (posElement.isJsonArray()) {
                                JsonArray posArray = posElement.getAsJsonArray();
                                if (posArray.size() >= 3) {
                                    double x = posArray.get(0).getAsDouble();
                                    double y = posArray.get(1).getAsDouble();
                                    double z = posArray.get(2).getAsDouble();
                                    position = new Vec3(x, y, z);
                                }
                            }
                        }

                        // Rotation
                        float yaw = kfJson.has("yaw") ? kfJson.get("yaw").getAsFloat() : 0.0f;
                        float pitch = kfJson.has("pitch") ? kfJson.get("pitch").getAsFloat() : 0.0f;
                        if (kfJson.has("rotation")) {
                            JsonArray rotArray = kfJson.getAsJsonArray("rotation");
                            if (rotArray.size() >= 2) {
                                pitch = rotArray.get(0).getAsFloat();
                                yaw = rotArray.get(1).getAsFloat();
                            }
                        }

                        // FOV
                        float fov = kfJson.has("fov") ? kfJson.get("fov").getAsFloat() : 70.0f;

                        // Duration (optional)
                        long duration = kfJson.has("duration") ? kfJson.get("duration").getAsLong() : 0L;

                        sceneData.addKeyframe(position, yaw, pitch, fov, duration);
                    }
                }
            }

            return sceneData;
        }
    }
}


