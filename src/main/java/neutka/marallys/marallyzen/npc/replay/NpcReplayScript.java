package neutka.marallys.marallyzen.npc.replay;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Replay script containing compressed tick frames.
 */
public record NpcReplayScript(
        String id,
        boolean loop,
        ReplayMode mode,
        boolean interpolate,
        int maxTicks,
        String sourceNpcId,
        String nextTriggerId,
        List<NpcReplayFrame> frames
) {
    public enum ReplayMode {
        PHYSICS,
        EXACT;

        public static ReplayMode from(String raw) {
            if (raw == null || raw.isBlank()) {
                return PHYSICS;
            }
            return "exact".equalsIgnoreCase(raw.trim()) ? EXACT : PHYSICS;
        }

        public String serialized() {
            return name();
        }
    }

    public static NpcReplayScript empty(String id) {
        return new NpcReplayScript(
                normalizeId(id),
                false,
                ReplayMode.PHYSICS,
                false,
                20 * 60 * 10,
                "",
                "",
                List.of()
        );
    }

    public NpcReplayScript {
        id = normalizeId(id);
        mode = mode == null ? ReplayMode.PHYSICS : mode;
        sourceNpcId = normalizeId(sourceNpcId);
        nextTriggerId = nextTriggerId == null ? "" : nextTriggerId.trim().toLowerCase(Locale.ROOT);
        maxTicks = Math.max(1, maxTicks);
        frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public int expandedLength() {
        int total = 0;
        for (NpcReplayFrame frame : frames) {
            total += Math.max(1, frame.repeat());
        }
        return total;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("loop", loop);
        root.addProperty("mode", mode.serialized());
        root.addProperty("interpolate", interpolate);
        root.addProperty("max_ticks", maxTicks);
        if (!sourceNpcId.isBlank()) {
            root.addProperty("source_npc", sourceNpcId);
        }
        if (!nextTriggerId.isBlank()) {
            root.addProperty("next_trigger", nextTriggerId);
        }
        JsonArray array = new JsonArray();
        for (NpcReplayFrame frame : frames) {
            array.add(frame.toJson());
        }
        root.add("frames", array);
        return root;
    }

    public static NpcReplayScript fromJson(JsonObject root) {
        if (root == null) {
            return null;
        }
        String id = normalizeId(readString(root, "id", ""));
        if (id.isBlank()) {
            return null;
        }
        boolean loop = readBool(root, "loop", false);
        ReplayMode mode = ReplayMode.from(readString(root, "mode", "physics"));
        boolean interpolate = readBool(root, "interpolate", false);
        int maxTicks = Math.max(1, readInt(root, "max_ticks", 20 * 60 * 10));
        String sourceNpcId = normalizeId(readString(root, "source_npc", readString(root, "npc", "")));
        String nextTriggerId = normalizeId(readString(root, "next_trigger", ""));
        JsonArray framesArray = root.getAsJsonArray("frames");
        List<NpcReplayFrame> frames = new ArrayList<>();
        if (framesArray != null) {
            for (JsonElement element : framesArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                frames.add(NpcReplayFrame.fromJson(element.getAsJsonObject()));
            }
        }
        return new NpcReplayScript(id, loop, mode, interpolate, maxTicks, sourceNpcId, nextTriggerId, frames);
    }

    private static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String readString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int readInt(JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean readBool(JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
