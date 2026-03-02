package neutka.marallys.marallyzen.door;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DoorDefinition {
    private final String id;
    private final String dimensionId;
    private final Bounds bounds;
    private final BlockPos anchor;
    private final List<BlockEntry> blocks;
    private final AnimationSettings animation;
    private final ShakeSettings shake;

    public DoorDefinition(
            String id,
            String dimensionId,
            Bounds bounds,
            BlockPos anchor,
            List<BlockEntry> blocks,
            AnimationSettings animation,
            ShakeSettings shake
    ) {
        this.id = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        this.dimensionId = dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId;
        this.bounds = bounds == null ? Bounds.empty() : bounds;
        this.anchor = anchor == null ? BlockPos.ZERO : anchor.immutable();
        this.blocks = blocks == null ? List.of() : List.copyOf(blocks);
        this.animation = animation == null ? AnimationSettings.defaults() : animation;
        this.shake = shake == null ? ShakeSettings.defaults() : shake;
    }

    public String id() {
        return id;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public Bounds bounds() {
        return bounds;
    }

    public BlockPos anchor() {
        return anchor;
    }

    public List<BlockEntry> blocks() {
        return blocks;
    }

    public AnimationSettings animation() {
        return animation;
    }

    public ShakeSettings shake() {
        return shake;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("dimension", dimensionId);
        root.add("bounds", bounds.toJson());
        root.add("anchor", posToJson(anchor));

        JsonArray blocksArray = new JsonArray();
        for (BlockEntry block : blocks) {
            blocksArray.add(block.toJson());
        }
        root.add("blocks", blocksArray);
        root.add("animation", animation.toJson());
        root.add("shake", shake.toJson());
        return root;
    }

    public static DoorDefinition fromJson(String fallbackId, JsonObject root, List<String> errors) {
        if (root == null) {
            errors.add("Root JSON is null");
            return null;
        }
        String id = readString(root, "id", fallbackId).trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            errors.add("id is required");
            return null;
        }
        String dimensionId = readString(root, "dimension", "minecraft:overworld");
        Bounds bounds = Bounds.fromJson(root.getAsJsonObject("bounds"), errors);
        BlockPos anchor = readPos(root.getAsJsonObject("anchor"), bounds.center());
        List<BlockEntry> blocks = parseBlocks(root.getAsJsonArray("blocks"), errors);
        AnimationSettings animation = AnimationSettings.fromJson(root.getAsJsonObject("animation"));
        ShakeSettings shake = ShakeSettings.fromJson(root.getAsJsonObject("shake"));
        return new DoorDefinition(id, dimensionId, bounds, anchor, blocks, animation, shake);
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

    private static BlockPos readPos(JsonObject obj, BlockPos fallback) {
        if (obj == null) {
            return fallback;
        }
        try {
            int x = obj.get("x").getAsInt();
            int y = obj.get("y").getAsInt();
            int z = obj.get("z").getAsInt();
            return new BlockPos(x, y, z);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static JsonObject posToJson(BlockPos pos) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", pos.getX());
        obj.addProperty("y", pos.getY());
        obj.addProperty("z", pos.getZ());
        return obj;
    }

    public record BlockEntry(int x, int y, int z, String state) {
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", x);
            obj.addProperty("y", y);
            obj.addProperty("z", z);
            obj.addProperty("state", state);
            return obj;
        }

        public static BlockEntry fromJson(JsonObject obj, List<String> errors) {
            if (obj == null) {
                return null;
            }
            try {
                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();
                int z = obj.get("z").getAsInt();
                String state = obj.get("state").getAsString();
                if (state == null || state.isBlank()) {
                    errors.add("block.state is required");
                    return null;
                }
                return new BlockEntry(x, y, z, state);
            } catch (Exception e) {
                errors.add("block entry has invalid values");
                return null;
            }
        }
    }

    public record Bounds(BlockPos min, BlockPos max) {
        public static Bounds empty() {
            return new Bounds(BlockPos.ZERO, BlockPos.ZERO);
        }

        public static Bounds fromJson(JsonObject obj, List<String> errors) {
            if (obj == null) {
                errors.add("bounds is required");
                return empty();
            }
            try {
                int minX = obj.get("minX").getAsInt();
                int minY = obj.get("minY").getAsInt();
                int minZ = obj.get("minZ").getAsInt();
                int maxX = obj.get("maxX").getAsInt();
                int maxY = obj.get("maxY").getAsInt();
                int maxZ = obj.get("maxZ").getAsInt();
                return new Bounds(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
            } catch (Exception e) {
                errors.add("bounds has invalid values");
                return empty();
            }
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("minX", min.getX());
            obj.addProperty("minY", min.getY());
            obj.addProperty("minZ", min.getZ());
            obj.addProperty("maxX", max.getX());
            obj.addProperty("maxY", max.getY());
            obj.addProperty("maxZ", max.getZ());
            return obj;
        }

        public BlockPos center() {
            return new BlockPos(
                    (min.getX() + max.getX()) / 2,
                    (min.getY() + max.getY()) / 2,
                    (min.getZ() + max.getZ()) / 2
            );
        }
    }

    public record AnimationSettings(
            int durationTicks,
            String easing,
            boolean stagger,
            int randomOffsetTicks
    ) {
        public static AnimationSettings defaults() {
            return new AnimationSettings(60, "ease_out", true, 5);
        }

        public static AnimationSettings fromJson(JsonObject obj) {
            if (obj == null) {
                return defaults();
            }
            int duration = readInt(obj, "duration_ticks", 60);
            String easing = readString(obj, "easing", "ease_out");
            boolean stagger = readBoolean(obj, "stagger", true);
            int randomOffset = Math.max(0, readInt(obj, "random_offset_ticks", 5));
            return new AnimationSettings(Math.max(1, duration), easing, stagger, randomOffset);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("duration_ticks", durationTicks);
            obj.addProperty("easing", easing);
            obj.addProperty("stagger", stagger);
            obj.addProperty("random_offset_ticks", randomOffsetTicks);
            return obj;
        }
    }

    public record ShakeSettings(
            int radius,
            float maxIntensity,
            String falloff
    ) {
        public static ShakeSettings defaults() {
            return new ShakeSettings(20, 1.0F, "linear");
        }

        public static ShakeSettings fromJson(JsonObject obj) {
            if (obj == null) {
                return defaults();
            }
            int radius = Math.max(1, readInt(obj, "radius", 20));
            float intensity = (float) readDouble(obj, "max_intensity", 1.0D);
            String falloff = readString(obj, "falloff", "linear");
            return new ShakeSettings(radius, intensity, falloff);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("radius", radius);
            obj.addProperty("max_intensity", maxIntensity);
            obj.addProperty("falloff", falloff);
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
