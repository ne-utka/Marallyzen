package neutka.marallys.marallyzen.goals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Immutable goal script model parsed from config JSON.
 */
public final class GoalScript {
    private final String id;
    private final Display display;
    private final Zone zone;
    private final GoalSpec goal;
    private final List<Milestone> milestones;
    private final OnComplete onComplete;

    private GoalScript(String id, Display display, Zone zone, GoalSpec goal, List<Milestone> milestones, OnComplete onComplete) {
        this.id = id;
        this.display = display;
        this.zone = zone;
        this.goal = goal;
        this.milestones = milestones;
        this.onComplete = onComplete;
    }

    public String id() {
        return id;
    }

    public Display display() {
        return display;
    }

    public Zone zone() {
        return zone;
    }

    public GoalSpec goal() {
        return goal;
    }

    public List<Milestone> milestones() {
        return milestones;
    }

    public OnComplete onComplete() {
        return onComplete;
    }

    public static GoalScript fromJson(String id, JsonObject root, List<String> errors) {
        if (root == null) {
            errors.add("Root JSON object is null");
            return null;
        }
        Display display = parseDisplay(root.getAsJsonObject("display"));
        Zone zone = parseZone(root.getAsJsonObject("zone"), errors);
        GoalSpec goalSpec = parseGoal(root.getAsJsonObject("goal"), errors);
        List<Milestone> milestones = parseMilestones(root.getAsJsonArray("milestones"), errors);
        OnComplete onComplete = parseOnComplete(root.getAsJsonObject("onComplete"));
        if (goalSpec == null) {
            return null;
        }
        return new GoalScript(id, display, zone, goalSpec, milestones, onComplete);
    }

    private static Display parseDisplay(JsonObject object) {
        if (object == null) {
            return Display.defaults();
        }
        String title = readString(object, "title", "World Goal");
        List<String> lines = readStringList(object.getAsJsonArray("lines"));

        JsonObject colorsObj = object.has("colors") && object.get("colors").isJsonObject()
                ? object.getAsJsonObject("colors")
                : null;
        ColorRgb titleColor = ColorRgb.fromArray(colorsObj != null ? colorsObj.getAsJsonArray("title") : null, ColorRgb.DEFAULT_TITLE);
        ColorRgb linesColor = ColorRgb.fromArray(colorsObj != null ? colorsObj.getAsJsonArray("lines") : null, ColorRgb.DEFAULT_LINES);

        JsonObject floatingObj = object.has("floating") && object.get("floating").isJsonObject()
                ? object.getAsJsonObject("floating")
                : null;
        Floating floating = Floating.fromJson(floatingObj);
        FontConfig font = FontConfig.fromJson(object.get("font"));
        return new Display(title, lines, titleColor, linesColor, floating, font);
    }

    private static Zone parseZone(JsonObject object, List<String> errors) {
        if (object == null) {
            return Zone.defaults();
        }
        int radius = readInt(object, "radius", 3);
        if (radius <= 0) {
            errors.add("zone.radius must be > 0");
            radius = 3;
        }
        boolean protect = readBoolean(object, "protect", true);
        return new Zone(radius, protect);
    }

    private static GoalSpec parseGoal(JsonObject object, List<String> errors) {
        if (object == null) {
            errors.add("Missing required 'goal' object");
            return null;
        }
        String type = readString(object, "type", "").trim().toLowerCase(Locale.ROOT);
        if (type.isBlank()) {
            errors.add("goal.type is required");
            return null;
        }
        long target = readLong(object, "target", 0L);
        if (target < 0L) {
            errors.add("goal.target must be >= 0");
            target = 0L;
        }
        CountMode countMode = CountMode.fromString(readString(object, "count_mode", "stack_size"));
        String itemId = readString(object, "item", "");
        if (itemId.isBlank()) {
            itemId = readString(object, "item_id", "");
        }
        String itemTag = readString(object, "tag", "");
        if (itemTag.isBlank()) {
            itemTag = readString(object, "item_tag", "");
        }
        String customHandler = readString(object, "handler", "");
        return new GoalSpec(type, target, countMode, itemId, itemTag, customHandler, object.deepCopy());
    }

    private static List<Milestone> parseMilestones(JsonArray array, List<String> errors) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<Milestone> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                errors.add("milestones entry is not an object");
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            long at = readLong(obj, "at", -1);
            if (at < 0L) {
                errors.add("milestones.at must be >= 0");
                continue;
            }
            List<String> commands = readStringList(obj.getAsJsonArray("commands"));
            boolean globalMessage = readBoolean(obj, "globalMessage", false);
            result.add(new Milestone(at, commands, globalMessage));
        }
        result.sort(Comparator.comparingLong(Milestone::at));
        return List.copyOf(result);
    }

    private static OnComplete parseOnComplete(JsonObject object) {
        if (object == null) {
            return OnComplete.defaults();
        }
        List<String> commands = readStringList(object.getAsJsonArray("commands"));
        boolean globalMessage = readBoolean(object, "globalMessage", false);
        String particles = readString(object, "particles", "");
        boolean removeGoal = readBoolean(object, "removeGoal", false);
        return new OnComplete(commands, globalMessage, particles, removeGoal);
    }

    private static String readString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return value.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long readLong(JsonObject object, String key, long fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return value.getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double readDouble(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return value.getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return value.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<String> readStringList(JsonArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (element != null && element.isJsonPrimitive()) {
                result.add(element.getAsString());
            }
        }
        return List.copyOf(result);
    }

    public enum CountMode {
        STACK_SIZE,
        PER_STACK;

        public static CountMode fromString(String raw) {
            if (raw == null) {
                return STACK_SIZE;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "per_stack" -> PER_STACK;
                default -> STACK_SIZE;
            };
        }
    }

    public record Display(
            String title,
            List<String> lines,
            ColorRgb titleColor,
            ColorRgb linesColor,
            Floating floating,
            FontConfig font
    ) {
        public static Display defaults() {
            return new Display(
                    "World Goal",
                    List.of("{progress}/{target}"),
                    ColorRgb.DEFAULT_TITLE,
                    ColorRgb.DEFAULT_LINES,
                    Floating.defaults(),
                    FontConfig.defaults()
            );
        }
    }

    public record FontConfig(String title, String lines) {
        public static FontConfig defaults() {
            return new FontConfig("", "");
        }

        public static FontConfig fromJson(JsonElement element) {
            if (element == null) {
                return defaults();
            }
            if (element.isJsonPrimitive()) {
                String all = element.getAsString();
                return new FontConfig(all, all);
            }
            if (!element.isJsonObject()) {
                return defaults();
            }
            JsonObject object = element.getAsJsonObject();
            String all = readString(object, "all", "");
            String title = readString(object, "title", all);
            String lines = readString(object, "lines", all);
            return new FontConfig(title, lines);
        }
    }

    public record ColorRgb(int r, int g, int b) {
        private static final ColorRgb DEFAULT_TITLE = new ColorRgb(255, 200, 50);
        private static final ColorRgb DEFAULT_LINES = new ColorRgb(180, 255, 180);

        public static ColorRgb fromArray(JsonArray array, ColorRgb fallback) {
            if (array == null || array.size() != 3) {
                return fallback;
            }
            try {
                return new ColorRgb(clampRgb(array.get(0).getAsInt()), clampRgb(array.get(1).getAsInt()), clampRgb(array.get(2).getAsInt()));
            } catch (Exception ignored) {
                return fallback;
            }
        }

        public int packedRgb() {
            return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }

        private static int clampRgb(int value) {
            return Math.max(0, Math.min(255, value));
        }
    }

    public record Floating(double amplitude, double speed, double height) {
        public static Floating defaults() {
            return new Floating(0.12D, 0.04D, 2.2D);
        }

        public static Floating fromJson(JsonObject object) {
            if (object == null) {
                return defaults();
            }
            return new Floating(
                    readDouble(object, "amplitude", 0.12D),
                    readDouble(object, "speed", 0.04D),
                    readDouble(object, "height", 2.2D)
            );
        }
    }

    public record Zone(int radius, boolean protect) {
        public static Zone defaults() {
            return new Zone(3, true);
        }
    }

    public record GoalSpec(
            String type,
            long target,
            CountMode countMode,
            String itemId,
            String itemTag,
            String customHandler,
            JsonObject raw
    ) {
    }

    public record Milestone(long at, List<String> commands, boolean globalMessage) {
    }

    public record OnComplete(List<String> commands, boolean globalMessage, String particles, boolean removeGoal) {
        public static OnComplete defaults() {
            return new OnComplete(List.of(), false, "", false);
        }
    }
}
