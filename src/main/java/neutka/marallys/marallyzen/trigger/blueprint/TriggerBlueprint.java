package neutka.marallys.marallyzen.trigger.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Immutable trigger structure blueprint.
 */
public final class TriggerBlueprint {
    private final String id;
    private final BlockPos size;
    private final BlockPos origin;
    private final List<BlockEntry> blocks;
    private final Settings settings;

    public TriggerBlueprint(String id, BlockPos size, BlockPos origin, List<BlockEntry> blocks, Settings settings) {
        this.id = id;
        this.size = size == null ? new BlockPos(1, 1, 1) : size.immutable();
        this.origin = origin == null ? null : origin.immutable();
        this.blocks = blocks == null ? List.of() : List.copyOf(blocks);
        this.settings = settings == null ? Settings.defaults() : settings;
    }

    public TriggerBlueprint(String id, BlockPos size, List<BlockEntry> blocks, Settings settings) {
        this(id, size, null, blocks, settings);
    }

    public String id() {
        return id;
    }

    public BlockPos size() {
        return size;
    }

    public BlockPos origin() {
        return origin;
    }

    public List<BlockEntry> blocks() {
        return blocks;
    }

    public Settings settings() {
        return settings;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id);

        JsonArray sizeArray = new JsonArray();
        sizeArray.add(size.getX());
        sizeArray.add(size.getY());
        sizeArray.add(size.getZ());
        root.add("size", sizeArray);
        if (origin != null) {
            JsonArray originArray = new JsonArray();
            originArray.add(origin.getX());
            originArray.add(origin.getY());
            originArray.add(origin.getZ());
            root.add("origin", originArray);
        }

        JsonArray blocksArray = new JsonArray();
        for (BlockEntry block : blocks) {
            blocksArray.add(block.toJson());
        }
        root.add("blocks", blocksArray);
        root.add("settings", settings.toJson());
        return root;
    }

    public static TriggerBlueprint fromJson(String fallbackId, JsonObject root, List<String> errors) {
        if (root == null) {
            errors.add("Root JSON is null");
            return null;
        }

        String id = readString(root, "id", fallbackId).trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            errors.add("id is required");
            return null;
        }

        BlockPos size = parseSize(root.getAsJsonArray("size"), errors);
        BlockPos origin = parseOptionalPos(root.getAsJsonArray("origin"));
        List<BlockEntry> blocks = parseBlocks(root.getAsJsonArray("blocks"), errors);
        Settings settings = Settings.fromJson(root.getAsJsonObject("settings"));
        return new TriggerBlueprint(id, size, origin, blocks, settings);
    }

    private static BlockPos parseSize(JsonArray array, List<String> errors) {
        if (array == null || array.size() != 3) {
            errors.add("size must be [x,y,z]");
            return new BlockPos(1, 1, 1);
        }
        try {
            int sx = Math.max(1, array.get(0).getAsInt());
            int sy = Math.max(1, array.get(1).getAsInt());
            int sz = Math.max(1, array.get(2).getAsInt());
            if (sx > 20 || sy > 20 || sz > 20) {
                errors.add("size values must be <= 20");
            }
            return new BlockPos(Math.min(20, sx), Math.min(20, sy), Math.min(20, sz));
        } catch (Exception e) {
            errors.add("size contains invalid values");
            return new BlockPos(1, 1, 1);
        }
    }

    private static BlockPos parseOptionalPos(JsonArray array) {
        if (array == null || array.size() != 3) {
            return null;
        }
        try {
            return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
        } catch (Exception e) {
            return null;
        }
    }

    private static List<BlockEntry> parseBlocks(JsonArray array, List<String> errors) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<BlockEntry> entries = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                errors.add("blocks entry must be object");
                continue;
            }
            BlockEntry entry = BlockEntry.fromJson(element.getAsJsonObject(), errors);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    private static String readString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsString();
    }

    public record BlockEntry(BlockPos localPos, String state, String blockEntitySnbt) {
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            JsonArray pos = new JsonArray();
            pos.add(localPos.getX());
            pos.add(localPos.getY());
            pos.add(localPos.getZ());
            obj.add("pos", pos);
            obj.addProperty("state", state);
            if (blockEntitySnbt != null && !blockEntitySnbt.isBlank()) {
                obj.addProperty("block_entity_snbt", blockEntitySnbt);
            }
            return obj;
        }

        public static BlockEntry fromJson(JsonObject obj, List<String> errors) {
            if (obj == null) {
                return null;
            }
            JsonArray pos = obj.getAsJsonArray("pos");
            if (pos == null || pos.size() != 3) {
                errors.add("block.pos must be [x,y,z]");
                return null;
            }
            String state = readString(obj, "state", "").trim();
            if (state.isBlank()) {
                errors.add("block.state is required");
                return null;
            }
            try {
                int x = pos.get(0).getAsInt();
                int y = pos.get(1).getAsInt();
                int z = pos.get(2).getAsInt();
                String be = readString(obj, "block_entity_snbt", "");
                return new BlockEntry(new BlockPos(x, y, z), state, be);
            } catch (Exception e) {
                errors.add("block.pos contains invalid values");
                return null;
            }
        }
    }

    public record Settings(
            AnimationSettings animation,
            int cooldownTicks,
            int timerIntervalTicks,
            int zoneRadius,
            String sound,
            String particle,
            String condition,
            ActionSettings action
    ) {
        public static Settings defaults() {
            return new Settings(
                    AnimationSettings.defaults(),
                    20,
                    200,
                    6,
                    "",
                    "block_dust",
                    "",
                    ActionSettings.none()
            );
        }

        public static Settings fromJson(JsonObject obj) {
            if (obj == null) {
                return defaults();
            }
            AnimationSettings animation = AnimationSettings.fromJson(obj);
            int cooldownTicks = Math.max(0, readInt(obj, "cooldown", 20));
            int timerIntervalTicks = Math.max(1, readInt(obj, "timer_interval", 200));
            int zoneRadius = Math.max(1, readInt(obj, "zone_radius", 6));
            String sound = readString(obj, "sound", "");
            String particle = readString(obj, "particle", readString(obj, "particles", "block_dust"));
            String condition = readString(obj, "condition", "");
            ActionSettings action = ActionSettings.fromJson(obj);
            return new Settings(animation, cooldownTicks, timerIntervalTicks, zoneRadius, sound, particle, condition, action);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.add("animation", animation.toJson());
            obj.addProperty("cooldown", cooldownTicks);
            obj.addProperty("timer_interval", timerIntervalTicks);
            obj.addProperty("zone_radius", zoneRadius);
            if (sound != null && !sound.isBlank()) {
                obj.addProperty("sound", sound);
            }
            if (particle != null && !particle.isBlank()) {
                obj.addProperty("particle", particle);
            }
            if (condition != null && !condition.isBlank()) {
                obj.addProperty("condition", condition);
            }
            if (action != null && action.hasAction()) {
                obj.add("action", action.toJson());
            }
            return obj;
        }

        public record ActionSettings(
                String type,
                String npc,
                String replay,
                String scene,
                String sceneArgs,
                String nextTrigger,
                String doorId,
                String activityType,
                String activityScript,
                String activityArgs,
                java.util.Map<String, String> params
        ) {
            public static ActionSettings none() {
                return new ActionSettings("", "", "", "", "", "", "", "", "", "", java.util.Map.of());
            }

            public boolean hasAction() {
                if (activityType != null && !activityType.isBlank()) {
                    return true;
                }
                if (activityScript != null && !activityScript.isBlank()) {
                    return true;
                }
                return type != null && !type.isBlank();
            }

            public static ActionSettings fromJson(JsonObject settingsObject) {
                if (settingsObject == null) {
                    return none();
                }
                JsonObject actionObj = settingsObject.getAsJsonObject("action");
                if (actionObj != null) {
                    java.util.Map<String, String> params = extractParams(actionObj);
                    String type = readString(actionObj, "type", "");
                    String npc = readString(actionObj, "npc", readString(actionObj, "npc_id", ""));
                    String replay = readString(actionObj, "replay", readString(actionObj, "replay_id", ""));
                    String scene = readString(actionObj, "scene", readString(actionObj, "script", ""));
                    String sceneArgs = readString(actionObj, "scene_args", readString(actionObj, "args", ""));
                    String nextTrigger = readString(actionObj, "next_trigger", readString(actionObj, "nextTrigger", ""));
                    String doorId = readString(actionObj, "door_id", readString(actionObj, "doorId", ""));

                    String activityType = readString(actionObj, "activity_type", readString(actionObj, "activityType", ""));
                    String activityScript = readString(actionObj, "activity_script", readString(actionObj, "activityScript", ""));
                    String activityArgs = readString(actionObj, "activity_args", readString(actionObj, "activityArgs", ""));
                    String activityRef = readString(actionObj, "activity", "");
                    if (actionObj.has("activity") && actionObj.get("activity").isJsonObject()) {
                        JsonObject activityObj = actionObj.getAsJsonObject("activity");
                        activityType = readString(activityObj, "type", activityType);
                        activityScript = readString(activityObj, "script", readString(activityObj, "id", activityScript));
                        activityArgs = readString(activityObj, "args", activityArgs);
                    }
                    if (activityScript.isBlank() && !activityRef.isBlank()) {
                        ActivityRef parsed = ActivityRef.parse(activityRef, activityType);
                        activityType = parsed.type;
                        activityScript = parsed.script;
                    }
                    return new ActionSettings(type, npc, replay, scene, sceneArgs, nextTrigger, doorId,
                            activityType, activityScript, activityArgs, params);
                }
                // Legacy flattened form inside settings.
                String legacyType = readString(settingsObject, "type", "");
                if (legacyType.isBlank()) {
                    return none();
                }
                java.util.Map<String, String> params = extractParams(settingsObject);
                String npc = readString(settingsObject, "npc", readString(settingsObject, "npc_id", ""));
                String replay = readString(settingsObject, "replay", readString(settingsObject, "replay_id", ""));
                String scene = readString(settingsObject, "scene", readString(settingsObject, "script", ""));
                String sceneArgs = readString(settingsObject, "scene_args", readString(settingsObject, "args", ""));
                String nextTrigger = readString(settingsObject, "next_trigger", readString(settingsObject, "nextTrigger", ""));
                String doorId = readString(settingsObject, "door_id", readString(settingsObject, "doorId", ""));
                String activityType = readString(settingsObject, "activity_type", readString(settingsObject, "activityType", ""));
                String activityScript = readString(settingsObject, "activity_script", readString(settingsObject, "activityScript", ""));
                String activityArgs = readString(settingsObject, "activity_args", readString(settingsObject, "activityArgs", ""));
                String activityRef = readString(settingsObject, "activity", "");
                if (settingsObject.has("activity") && settingsObject.get("activity").isJsonObject()) {
                    JsonObject activityObj = settingsObject.getAsJsonObject("activity");
                    activityType = readString(activityObj, "type", activityType);
                    activityScript = readString(activityObj, "script", readString(activityObj, "id", activityScript));
                    activityArgs = readString(activityObj, "args", activityArgs);
                }
                if (activityScript.isBlank() && !activityRef.isBlank()) {
                    ActivityRef parsed = ActivityRef.parse(activityRef, activityType);
                    activityType = parsed.type;
                    activityScript = parsed.script;
                }
                return new ActionSettings(legacyType, npc, replay, scene, sceneArgs, nextTrigger, doorId,
                        activityType, activityScript, activityArgs, params);
            }

            public JsonObject toJson() {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", type == null ? "" : type);
                if (npc != null && !npc.isBlank()) {
                    obj.addProperty("npc", npc);
                }
                if (replay != null && !replay.isBlank()) {
                    obj.addProperty("replay", replay);
                }
                if (scene != null && !scene.isBlank()) {
                    obj.addProperty("scene", scene);
                }
                if (sceneArgs != null && !sceneArgs.isBlank()) {
                    obj.addProperty("scene_args", sceneArgs);
                }
                if (nextTrigger != null && !nextTrigger.isBlank()) {
                    obj.addProperty("next_trigger", nextTrigger);
                }
                if (doorId != null && !doorId.isBlank()) {
                    obj.addProperty("door_id", doorId);
                }
                if (activityType != null && !activityType.isBlank()) {
                    obj.addProperty("activity_type", activityType);
                }
                if (activityScript != null && !activityScript.isBlank()) {
                    obj.addProperty("activity_script", activityScript);
                }
                if (activityArgs != null && !activityArgs.isBlank()) {
                    obj.addProperty("activity_args", activityArgs);
                }
                if (params != null && !params.isEmpty()) {
                    JsonObject paramsObj = new JsonObject();
                    for (var entry : params.entrySet()) {
                        String key = entry.getKey();
                        if (key == null || key.isBlank()) {
                            continue;
                        }
                        if (isReservedParam(key)) {
                            continue;
                        }
                        paramsObj.addProperty(key, entry.getValue());
                    }
                    if (!paramsObj.isEmpty()) {
                        obj.add("params", paramsObj);
                    }
                }
                return obj;
            }

            public String resolveActivityType() {
                if (activityType != null && !activityType.isBlank()) {
                    return activityType.trim().toLowerCase(Locale.ROOT);
                }
                if (doorId != null && !doorId.isBlank()) {
                    return "door";
                }
                if ((replay != null && !replay.isBlank()) || (scene != null && !scene.isBlank())) {
                    return "npc";
                }
                if (type != null && !type.isBlank() && !"activity".equalsIgnoreCase(type)) {
                    return type.trim().toLowerCase(Locale.ROOT);
                }
                return "";
            }

            public String resolveActivityScript() {
                if (activityScript != null && !activityScript.isBlank()) {
                    return activityScript.trim().toLowerCase(Locale.ROOT);
                }
                if (doorId != null && !doorId.isBlank()) {
                    return doorId.trim().toLowerCase(Locale.ROOT);
                }
                if (scene != null && !scene.isBlank()) {
                    return scene.trim().toLowerCase(Locale.ROOT);
                }
                if (replay != null && !replay.isBlank()) {
                    return replay.trim().toLowerCase(Locale.ROOT);
                }
                String ref = param("activity");
                if (ref != null && !ref.isBlank()) {
                    ActivityRef parsed = ActivityRef.parse(ref, "");
                    return parsed.script;
                }
                return "";
            }

            public String param(String key) {
                if (key == null || key.isBlank()) {
                    return "";
                }
                String normalized = key.trim().toLowerCase(Locale.ROOT);
                if (params != null && params.containsKey(normalized)) {
                    return params.get(normalized);
                }
                return "";
            }

            private static java.util.Map<String, String> extractParams(JsonObject obj) {
                java.util.Map<String, String> out = new java.util.HashMap<>();
                if (obj == null) {
                    return out;
                }
                for (var entry : obj.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
                        out.put(normalizeKey(entry.getKey()), entry.getValue().getAsString());
                    }
                }
                JsonObject paramsObj = obj.getAsJsonObject("params");
                if (paramsObj != null) {
                    for (var entry : paramsObj.entrySet()) {
                        if (entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
                            out.put(normalizeKey(entry.getKey()), entry.getValue().getAsString());
                        }
                    }
                }
                return out;
            }

            private static boolean isReservedParam(String key) {
                String normalized = key.toLowerCase(Locale.ROOT);
                return "type".equals(normalized)
                        || "activity_type".equals(normalized)
                        || "activity_script".equals(normalized)
                        || "activity_args".equals(normalized)
                        || "scene_args".equals(normalized)
                        || "next_trigger".equals(normalized)
                        || "door_id".equals(normalized)
                        || "npc".equals(normalized)
                        || "npc_id".equals(normalized)
                        || "replay".equals(normalized)
                        || "replay_id".equals(normalized)
                        || "scene".equals(normalized)
                        || "script".equals(normalized)
                        || "params".equals(normalized);
            }

            private static String normalizeKey(String key) {
                if (key == null) {
                    return "";
                }
                return key.trim().toLowerCase(Locale.ROOT);
            }

            private record ActivityRef(String type, String script) {
                private static ActivityRef parse(String raw, String fallbackType) {
                    String clean = raw == null ? "" : raw.trim();
                    if (clean.isBlank()) {
                        return new ActivityRef(fallbackType, "");
                    }
                    String type = fallbackType == null ? "" : fallbackType;
                    String script = clean;
                    if (clean.contains("/")) {
                        String[] parts = clean.split("/", 2);
                        type = parts[0];
                        script = parts[1];
                    } else if (clean.contains(":")) {
                        String[] parts = clean.split(":", 2);
                        type = parts[0];
                        script = parts[1];
                    }
                    return new ActivityRef(type == null ? "" : type.trim().toLowerCase(Locale.ROOT),
                            script == null ? "" : script.trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        public record AnimationSettings(
                String type,
                int duration,
                BlockPos spawnOffset,
                String easing,
                ShakeSettings screenShake
        ) {
            public static AnimationSettings defaults() {
                return new AnimationSettings(
                        "rise_from_ground",
                        60,
                        new BlockPos(0, -6, 0),
                        "ease_out",
                        ShakeSettings.defaults()
                );
            }

            public static AnimationSettings fromJson(JsonObject settingsObject) {
                if (settingsObject == null) {
                    return defaults();
                }
                JsonElement animationElement = settingsObject.get("animation");
                if (animationElement != null && animationElement.isJsonObject()) {
                    JsonObject animationObject = animationElement.getAsJsonObject();
                    String type = readString(animationObject, "type", "rise_from_ground");
                    int duration = Math.max(1, readInt(animationObject, "duration", 60));
                    BlockPos spawnOffset = readPos(animationObject.getAsJsonArray("spawn_offset"), new BlockPos(0, -6, 0));
                    String easing = readString(animationObject, "easing", "ease_out");
                    ShakeSettings screenShake = ShakeSettings.fromJson(animationObject.getAsJsonObject("screen_shake"), duration);
                    return new AnimationSettings(type, duration, spawnOffset, easing, screenShake);
                }

                String type = readString(settingsObject, "animation", "rise_from_ground");
                int duration = Math.max(1, readInt(settingsObject, "duration", 60));
                BlockPos spawnOffset = readPos(settingsObject.getAsJsonArray("spawn_offset"), new BlockPos(0, -6, 0));
                String easing = readString(settingsObject, "easing", "ease_out");
                int shakeRadius = Math.max(1, readInt(settingsObject, "shake_radius", 30));
                float shakeIntensity = (float) readDouble(settingsObject, "camera_shake", 0.6D);
                String shakeFalloff = readString(settingsObject, "shake_falloff", "linear");
                ShakeSettings screenShake = new ShakeSettings(shakeRadius, shakeIntensity, shakeFalloff, duration);
                return new AnimationSettings(type, duration, spawnOffset, easing, screenShake);
            }

            public JsonObject toJson() {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", type);
                obj.addProperty("duration", duration);
                JsonArray offset = new JsonArray();
                offset.add(spawnOffset.getX());
                offset.add(spawnOffset.getY());
                offset.add(spawnOffset.getZ());
                obj.add("spawn_offset", offset);
                obj.addProperty("easing", easing);
                obj.add("screen_shake", screenShake.toJson());
                return obj;
            }
        }

        public record ShakeSettings(
                int radius,
                float maxIntensity,
                String falloff,
                int durationTicks
        ) {
            public static ShakeSettings defaults() {
                return new ShakeSettings(30, 0.6F, "linear", 60);
            }

            public static ShakeSettings fromJson(JsonObject obj, int animationDuration) {
                if (obj == null) {
                    return new ShakeSettings(30, 0.6F, "linear", Math.max(1, animationDuration));
                }
                int radius = Math.max(1, readInt(obj, "radius", 30));
                float maxIntensity = (float) readDouble(obj, "max_intensity", 0.6D);
                String falloff = readString(obj, "falloff", "linear");
                int duration = Math.max(1, readInt(obj, "duration", animationDuration));
                return new ShakeSettings(radius, maxIntensity, falloff, duration);
            }

            public JsonObject toJson() {
                JsonObject obj = new JsonObject();
                obj.addProperty("radius", radius);
                obj.addProperty("max_intensity", maxIntensity);
                obj.addProperty("falloff", falloff);
                obj.addProperty("duration", durationTicks);
                return obj;
            }
        }

        private static int readInt(JsonObject obj, String key, int fallback) {
            if (obj == null || !obj.has(key)) {
                return fallback;
            }
            JsonElement element = obj.get(key);
            if (element == null || !element.isJsonPrimitive()) {
                return fallback;
            }
            try {
                return element.getAsInt();
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private static double readDouble(JsonObject obj, String key, double fallback) {
            if (obj == null || !obj.has(key)) {
                return fallback;
            }
            JsonElement element = obj.get(key);
            if (element == null || !element.isJsonPrimitive()) {
                return fallback;
            }
            try {
                return element.getAsDouble();
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private static BlockPos readPos(JsonArray array, BlockPos fallback) {
            if (array == null || array.size() != 3) {
                return fallback;
            }
            try {
                return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
            } catch (Exception e) {
                return fallback;
            }
        }
    }
}
