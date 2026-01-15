package neutka.marallys.marallyzen.client.cutscene.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldStorage;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldTrack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serializes and deserializes cutscene editor data to/from JSON format.
 */
public class CutsceneEditorSerializer {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Saves cutscene data to JSON file.
     */
    public static void save(CutsceneEditorData data) throws IOException {
        if (data.getId() == null || data.getId().isEmpty()) {
            throw new IllegalArgumentException("Cutscene ID cannot be null or empty");
        }

        Path scenesDir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("scenes");
        
        if (!Files.exists(scenesDir)) {
            Files.createDirectories(scenesDir);
        }

        File file = scenesDir.resolve(data.getId() + ".json").toFile();
        
        JsonObject json = toJson(data);
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        }

        persistWorldTrack(data);
        
        Marallyzen.LOGGER.info("Saved cutscene: {} to {}", data.getId(), file.getAbsolutePath());
    }

    /**
     * Loads cutscene data from JSON file.
     */
    public static CutsceneEditorData load(String sceneId) throws IOException {
        Path scenesDir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("scenes");
        File file = scenesDir.resolve(sceneId + ".json").toFile();
        
        if (!file.exists()) {
            throw new IOException("Cutscene file not found: " + file.getAbsolutePath());
        }

        JsonObject json = gson.fromJson(Files.readString(file.toPath()), JsonObject.class);
        CutsceneEditorData data = fromJson(json);
        try {
            CutsceneWorldTrack track = CutsceneWorldStorage.load(sceneId);
            data.setWorldTrack(track);
        } catch (IOException e) {
            Marallyzen.LOGGER.warn("Failed to load cutscene world track: {}", sceneId, e);
        }
        return data;
    }

    /**
     * Converts CutsceneEditorData to JSON.
     */
    private static JsonObject toJson(CutsceneEditorData data) {
        JsonObject root = new JsonObject();
        root.addProperty("id", data.getId());
        root.addProperty("duration", data.getTotalDuration());

        JsonArray keyframesArray = new JsonArray();
        for (CutsceneEditorData.EditorKeyframe keyframe : data.getKeyframes()) {
            JsonObject kfJson = new JsonObject();
            kfJson.addProperty("time", keyframe.getTime());
            kfJson.addProperty("type", keyframe.getType().name());
            if (keyframe.getGroupId() >= 0) {
                kfJson.addProperty("group", keyframe.getGroupId());
            }

            switch (keyframe.getType()) {
                case CAMERA -> {
                    CutsceneEditorData.CameraKeyframe cameraKf = (CutsceneEditorData.CameraKeyframe) keyframe;
                    JsonArray posArray = new JsonArray();
                    posArray.add(cameraKf.getPosition().x);
                    posArray.add(cameraKf.getPosition().y);
                    posArray.add(cameraKf.getPosition().z);
                    kfJson.add("position", posArray);
                    JsonArray rotation = new JsonArray();
                    rotation.add(cameraKf.getPitch());
                    rotation.add(cameraKf.getYaw());
                    kfJson.add("rotation", rotation);
                    kfJson.addProperty("fov", cameraKf.getFov());
                    kfJson.addProperty("smooth", cameraKf.isSmooth());
                    kfJson.addProperty("duration", cameraKf.getDuration());
                    // Note: Duration is calculated from time difference to next keyframe in playback
                }
                case PAUSE -> {
                    CutsceneEditorData.PauseKeyframe pauseKf = (CutsceneEditorData.PauseKeyframe) keyframe;
                    kfJson.addProperty("duration", pauseKf.getDuration());
                }
                case ACTOR -> {
                    CutsceneEditorData.ActorKeyframe actorKf = (CutsceneEditorData.ActorKeyframe) keyframe;
                    JsonArray posArray = new JsonArray();
                    posArray.add(actorKf.getPosition().x);
                    posArray.add(actorKf.getPosition().y);
                    posArray.add(actorKf.getPosition().z);
                    kfJson.add("position", posArray);
                    kfJson.addProperty("yaw", actorKf.getYaw());
                    kfJson.addProperty("pitch", actorKf.getPitch());
                    kfJson.addProperty("duration", actorKf.getDuration());
                }
                case EMOTION -> {
                    CutsceneEditorData.EmotionKeyframe emotionKf = (CutsceneEditorData.EmotionKeyframe) keyframe;
                    kfJson.addProperty("npcId", emotionKf.getNpcId());
                    kfJson.addProperty("emote", emotionKf.getEmoteId());
                }
                case CAMERA_MODE -> {
                    CutsceneEditorData.CameraModeKeyframe modeKf = (CutsceneEditorData.CameraModeKeyframe) keyframe;
                    kfJson.addProperty("mode", modeKf.getMode().name());
                }
            }

            keyframesArray.add(kfJson);
        }

        root.add("keyframes", keyframesArray);
        return root;
    }

    /**
     * Converts JSON to CutsceneEditorData.
     */
    private static CutsceneEditorData fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        CutsceneEditorData data = new CutsceneEditorData(id);

        if (json.has("duration")) {
            data.setTotalDuration(json.get("duration").getAsLong());
        }

        if (json.has("keyframes")) {
            JsonArray keyframesArray = json.getAsJsonArray("keyframes");
            for (var element : keyframesArray) {
                JsonObject kfJson = element.getAsJsonObject();
                long time = kfJson.get("time").getAsLong();
                String typeStr = kfJson.get("type").getAsString();
                CutsceneEditorData.KeyframeType type = CutsceneEditorData.KeyframeType.valueOf(typeStr);

                CutsceneEditorData.EditorKeyframe keyframe = switch (type) {
                case CAMERA -> {
                    JsonArray posArray = kfJson.getAsJsonArray("position");
                    Vec3 position = new Vec3(
                        posArray.get(0).getAsDouble(),
                        posArray.get(1).getAsDouble(),
                        posArray.get(2).getAsDouble()
                    );
                    JsonArray rotArray = kfJson.getAsJsonArray("rotation");
                    float pitch = rotArray.get(0).getAsFloat();
                    float yaw = rotArray.get(1).getAsFloat();
                    float fov = kfJson.has("fov") ? kfJson.get("fov").getAsFloat() : 70.0f;
                    boolean smooth = kfJson.has("smooth") && kfJson.get("smooth").getAsBoolean();
                    
                    // Calculate duration from next keyframe or use default
                    // For now, use a default duration - actual duration will be calculated during playback
                    long duration = 20; // Default 1 second (20 ticks)
                    if (kfJson.has("duration")) {
                        duration = kfJson.get("duration").getAsLong();
                    }
                    yield new CutsceneEditorData.CameraKeyframe(time, position, yaw, pitch, fov, duration, smooth);
                }
                    case ACTOR -> {
                        JsonArray posArray = kfJson.getAsJsonArray("position");
                        Vec3 position = new Vec3(
                            posArray.get(0).getAsDouble(),
                            posArray.get(1).getAsDouble(),
                            posArray.get(2).getAsDouble()
                        );
                        float yaw = kfJson.has("yaw") ? kfJson.get("yaw").getAsFloat() : 0.0f;
                        float pitch = kfJson.has("pitch") ? kfJson.get("pitch").getAsFloat() : 0.0f;
                        if (kfJson.has("rotation")) {
                            JsonArray rotArray = kfJson.getAsJsonArray("rotation");
                            if (rotArray.size() >= 2) {
                                pitch = rotArray.get(0).getAsFloat();
                                yaw = rotArray.get(1).getAsFloat();
                            }
                        }
                        long duration = kfJson.has("duration") ? kfJson.get("duration").getAsLong() : 20;
                        yield new CutsceneEditorData.ActorKeyframe(time, position, yaw, pitch, duration);
                    }
                    case PAUSE -> {
                        long duration = kfJson.get("duration").getAsLong();
                        yield new CutsceneEditorData.PauseKeyframe(time, duration);
                    }
                    case EMOTION -> {
                        String npcId = kfJson.get("npcId").getAsString();
                        String emote = kfJson.get("emote").getAsString();
                        yield new CutsceneEditorData.EmotionKeyframe(time, npcId, emote);
                    }
                    case CAMERA_MODE -> {
                        String modeStr = kfJson.get("mode").getAsString();
                        CutsceneEditorData.CameraMode mode = CutsceneEditorData.CameraMode.valueOf(modeStr);
                        yield new CutsceneEditorData.CameraModeKeyframe(time, mode);
                    }
                };

                if (kfJson.has("group")) {
                    keyframe.setGroupId(kfJson.get("group").getAsInt());
                }
                data.addKeyframe(keyframe);
            }
        }

        return data;
    }

    /**
     * Converts Vec3 to JSON array.
     */
    private static JsonArray toJsonArray(Vec3 vec) {
        JsonArray array = new JsonArray();
        array.add(vec.x);
        array.add(vec.y);
        array.add(vec.z);
        return array;
    }

    private static void persistWorldTrack(CutsceneEditorData data) {
        if (data == null || data.getId() == null || data.getId().isBlank()) {
            return;
        }
        CutsceneWorldTrack track = data.getWorldTrack();
        try {
            if (track != null) {
                CutsceneWorldStorage.save(data.getId(), track);
            } else {
                Path path = CutsceneWorldStorage.getWorldTrackPath(data.getId());
                if (Files.exists(path)) {
                    Files.delete(path);
                }
            }
        } catch (IOException e) {
            Marallyzen.LOGGER.warn("Failed to save cutscene world track: {}", data.getId(), e);
        }
    }
}
