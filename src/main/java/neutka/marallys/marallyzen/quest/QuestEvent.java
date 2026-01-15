package neutka.marallys.marallyzen.quest;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QuestEvent {
    private final String type;
    private final Map<String, String> data;
    private final BlockPos position;

    public QuestEvent(String type, Map<String, String> data, BlockPos position) {
        this.type = type;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.position = position;
    }

    public String type() {
        return type;
    }

    public Map<String, String> data() {
        return Collections.unmodifiableMap(data);
    }

    public BlockPos position() {
        return position;
    }

    public String getString(String key, String fallback) {
        return data.getOrDefault(key, fallback);
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(data.get(key));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
