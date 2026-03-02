package neutka.marallys.marallyzen.activity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ActivityScript {
    public static final int CURRENT_FORMAT = 1;

    private final String id;
    private final String type;
    private final int formatVersion;
    private final Animation animation;
    private final Physics physics;
    private final Effects effects;
    private final JsonObject data;

    public ActivityScript(
            String id,
            String type,
            int formatVersion,
            Animation animation,
            Physics physics,
            Effects effects,
            JsonObject data
    ) {
        this.id = normalize(id);
        this.type = normalize(type);
        this.formatVersion = Math.max(1, formatVersion);
        this.animation = animation == null ? Animation.defaults() : animation;
        this.physics = physics == null ? Physics.defaults() : physics;
        this.effects = effects == null ? Effects.defaults() : effects;
        this.data = data == null ? new JsonObject() : data.deepCopy();
    }

    public String id() {
        return id;
    }

    public String type() {
        return type;
    }

    public int formatVersion() {
        return formatVersion;
    }

    public Animation animation() {
        return animation;
    }

    public Physics physics() {
        return physics;
    }

    public Effects effects() {
        return effects;
    }

    public JsonObject data() {
        return data.deepCopy();
    }

    public String getString(String key, String fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        if (data.has(key)) {
            try {
                return data.get(key).getAsString();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    public boolean getBoolean(String key, boolean fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        if (data.has(key)) {
            try {
                return data.get(key).getAsBoolean();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    public int getInt(String key, int fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        if (data.has(key)) {
            try {
                return data.get(key).getAsInt();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    public double getDouble(String key, double fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        if (data.has(key)) {
            try {
                return data.get(key).getAsDouble();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", formatVersion);
        root.addProperty("type", type);
        root.add("animation", animation.toJson());
        root.add("physics", physics.toJson());
        root.add("effects", effects.toJson());
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (isReservedKey(entry.getKey())) {
                continue;
            }
            root.add(entry.getKey(), entry.getValue());
        }
        return root;
    }

    public static ActivityScript fromJson(String fallbackId, String fallbackType, JsonObject root, java.util.List<String> errors) {
        if (root == null) {
            errors.add("Root JSON is null");
            return null;
        }
        String type = readString(root, "type", fallbackType);
        String id = normalize(fallbackId);
        if (id.isBlank()) {
            errors.add("id is required");
            return null;
        }
        int format = Math.max(1, readInt(root, "format_version", CURRENT_FORMAT));
        Animation animation = Animation.fromJson(root.getAsJsonObject("animation"));
        Physics physics = Physics.fromJson(root.getAsJsonObject("physics"));
        Effects effects = Effects.fromJson(root.getAsJsonObject("effects"));
        JsonObject data = collectExtraData(root);
        return new ActivityScript(id, type, format, animation, physics, effects, data);
    }

    private static JsonObject collectExtraData(JsonObject root) {
        JsonObject extra = new JsonObject();
        JsonObject dataObj = root.getAsJsonObject("data");
        if (dataObj != null) {
            for (Map.Entry<String, JsonElement> entry : dataObj.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank()) {
                    extra.add(entry.getKey(), entry.getValue());
                }
            }
        }
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (isReservedKey(entry.getKey())) {
                continue;
            }
            extra.add(entry.getKey(), entry.getValue());
        }
        return extra;
    }

    private static boolean isReservedKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return "format_version".equals(normalized)
                || "type".equals(normalized)
                || "animation".equals(normalized)
                || "physics".equals(normalized)
                || "effects".equals(normalized)
                || "data".equals(normalized);
    }

    private static String readString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsString();
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

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public record Animation(
            String mode,
            String direction,
            int durationTicks,
            String easing,
            BlockPos spawnOffset,
            double distance,
            double overshoot
    ) {
        public static Animation defaults() {
            return new Animation("vertical", "down", 60, "ease_out", BlockPos.ZERO, 0.0D, 0.0D);
        }

        public static Animation fromJson(JsonObject obj) {
            if (obj == null) {
                return defaults();
            }
            String mode = readString(obj, "mode", "vertical");
            String direction = readString(obj, "direction", "down");
            int duration = Math.max(1, readInt(obj, "duration", readInt(obj, "duration_ticks", 60)));
            String easing = readString(obj, "easing", "ease_out");
            BlockPos spawnOffset = readPos(obj.getAsJsonArray("spawn_offset"));
            double distance = readDouble(obj, "distance", 0.0D);
            double overshoot = readDouble(obj, "overshoot", 0.0D);
            return new Animation(mode, direction, duration, easing, spawnOffset, distance, overshoot);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("mode", mode == null ? "" : mode);
            obj.addProperty("direction", direction == null ? "" : direction);
            obj.addProperty("duration", durationTicks);
            obj.addProperty("easing", easing == null ? "" : easing);
            if (spawnOffset != null && !spawnOffset.equals(BlockPos.ZERO)) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                arr.add(spawnOffset.getX());
                arr.add(spawnOffset.getY());
                arr.add(spawnOffset.getZ());
                obj.add("spawn_offset", arr);
            }
            if (distance != 0.0D) {
                obj.addProperty("distance", distance);
            }
            if (overshoot != 0.0D) {
                obj.addProperty("overshoot", overshoot);
            }
            return obj;
        }

        private static BlockPos readPos(com.google.gson.JsonArray array) {
            if (array == null || array.size() != 3) {
                return BlockPos.ZERO;
            }
            try {
                return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
            } catch (Exception e) {
                return BlockPos.ZERO;
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
    }

    public record Physics(
            boolean collision,
            boolean solidOnSpawn
    ) {
        public static Physics defaults() {
            return new Physics(true, true);
        }

        public static Physics fromJson(JsonObject obj) {
            if (obj == null) {
                return defaults();
            }
            boolean collision = readBoolean(obj, "collision", true);
            boolean solidOnSpawn = readBoolean(obj, "solid_on_spawn", true);
            return new Physics(collision, solidOnSpawn);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("collision", collision);
            obj.addProperty("solid_on_spawn", solidOnSpawn);
            return obj;
        }

        private static boolean readBoolean(JsonObject obj, String key, boolean fallback) {
            if (obj == null || !obj.has(key)) {
                return fallback;
            }
            JsonElement element = obj.get(key);
            if (element == null || !element.isJsonPrimitive()) {
                return fallback;
            }
            try {
                return element.getAsBoolean();
            } catch (Exception ignored) {
                return fallback;
            }
        }
    }

    public record Effects(
            String soundStart,
            String soundEnd,
            ParticleSettings particles,
            ShakeSettings shake
    ) {
        public static Effects defaults() {
            return new Effects("", "", ParticleSettings.defaults(), ShakeSettings.defaults());
        }

        public static Effects fromJson(JsonObject obj) {
            if (obj == null) {
                return defaults();
            }
            String soundStart = readString(obj, "sound_start", "");
            String soundEnd = readString(obj, "sound_end", "");
            ParticleSettings particles = ParticleSettings.fromJson(obj.getAsJsonObject("particles"));
            ShakeSettings shake = ShakeSettings.fromJson(obj.getAsJsonObject("shake"));
            return new Effects(soundStart, soundEnd, particles, shake);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            if (soundStart != null && !soundStart.isBlank()) {
                obj.addProperty("sound_start", soundStart);
            }
            if (soundEnd != null && !soundEnd.isBlank()) {
                obj.addProperty("sound_end", soundEnd);
            }
            obj.add("particles", particles.toJson());
            obj.add("shake", shake.toJson());
            return obj;
        }
    }

    public record ParticleSettings(
            String type,
            int count,
            double spread,
            double speed
    ) {
        public static ParticleSettings defaults() {
            return new ParticleSettings("", 0, 0.25D, 0.01D);
        }

        public static ParticleSettings fromJson(JsonObject obj) {
            if (obj == null) {
                return defaults();
            }
            String type = readString(obj, "type", "");
            int count = Math.max(0, readInt(obj, "count", 0));
            double spread = readDouble(obj, "spread", 0.25D);
            double speed = readDouble(obj, "speed", 0.01D);
            return new ParticleSettings(type, count, spread, speed);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            if (type != null && !type.isBlank()) {
                obj.addProperty("type", type);
            }
            obj.addProperty("count", count);
            obj.addProperty("spread", spread);
            obj.addProperty("speed", speed);
            return obj;
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
    }

    public record ShakeSettings(
            int radius,
            float maxIntensity,
            String falloff,
            int durationTicks
    ) {
        public static ShakeSettings defaults() {
            return new ShakeSettings(0, 0.0F, "linear", 15);
        }

        public static ShakeSettings fromJson(JsonObject obj) {
            if (obj == null) {
                return defaults();
            }
            int radius = Math.max(0, readInt(obj, "radius", 0));
            float intensity = (float) readDouble(obj, "max_intensity", 0.0D);
            String falloff = readString(obj, "falloff", "linear");
            int duration = Math.max(1, readInt(obj, "duration", 15));
            return new ShakeSettings(radius, intensity, falloff, duration);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("radius", radius);
            obj.addProperty("max_intensity", maxIntensity);
            obj.addProperty("falloff", falloff);
            obj.addProperty("duration", durationTicks);
            return obj;
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
    }
}
