package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class QuestJsonUtils {
    private QuestJsonUtils() {
    }

    public static String getString(JsonObject obj, String key, String fallback) {
        if (obj == null || key == null || !obj.has(key)) {
            return fallback;
        }
        JsonElement value = obj.get(key);
        return value != null ? value.getAsString() : fallback;
    }

    public static int getInt(JsonObject obj, String key, int fallback) {
        if (obj == null || key == null || !obj.has(key)) {
            return fallback;
        }
        JsonElement value = obj.get(key);
        return value != null ? value.getAsInt() : fallback;
    }

    public static long getLong(JsonObject obj, String key, long fallback) {
        if (obj == null || key == null || !obj.has(key)) {
            return fallback;
        }
        JsonElement value = obj.get(key);
        return value != null ? value.getAsLong() : fallback;
    }

    public static double getDouble(JsonObject obj, String key, double fallback) {
        if (obj == null || key == null || !obj.has(key)) {
            return fallback;
        }
        JsonElement value = obj.get(key);
        return value != null ? value.getAsDouble() : fallback;
    }

    public static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (obj == null || key == null || !obj.has(key)) {
            return fallback;
        }
        JsonElement value = obj.get(key);
        return value != null ? value.getAsBoolean() : fallback;
    }

    public static JsonArray array(JsonObject obj, String key) {
        if (obj == null || key == null) {
            return null;
        }
        return obj.getAsJsonArray(key);
    }
}
