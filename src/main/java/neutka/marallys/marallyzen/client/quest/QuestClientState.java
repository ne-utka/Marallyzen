package neutka.marallys.marallyzen.client.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.quest.QuestDefinition;
import neutka.marallys.marallyzen.quest.QuestInstance;
import neutka.marallys.marallyzen.quest.QuestPlayerData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class QuestClientState {
    private static final QuestClientState INSTANCE = new QuestClientState();
    private static final Gson GSON = new GsonBuilder().create();

    private Map<String, QuestDefinition> definitions = new HashMap<>();
    private QuestPlayerData playerData = new QuestPlayerData();
    private QuestZoneVisual activeZone;
    private long lastSyncMs;
    private QuestClientConfig config;

    private QuestClientState() {
    }
    
    private QuestClientConfig getConfig() {
        if (config == null) {
            config = QuestClientConfig.load();
        }
        return config;
    }
    
    public static QuestClientState getInstance() {
        return INSTANCE;
    }

    public synchronized void applySync(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(payloadJson, JsonObject.class);
            if (root == null) {
                return;
            }
            Map<String, QuestDefinition> updated = new HashMap<>();
            JsonObject defs = root.getAsJsonObject("definitions");
            if (defs != null) {
                for (String key : defs.keySet()) {
                    QuestDefinition definition = QuestDefinition.fromJson(defs.getAsJsonObject(key));
                    if (definition != null) {
                        updated.put(key, definition);
                    }
                }
            }
            QuestPlayerData data = QuestPlayerData.fromJson(root.getAsJsonObject("player"));
            QuestZoneVisual zone = QuestZoneVisual.fromJson(root.getAsJsonObject("activeZone"));
            definitions = updated;
            playerData = data;
            activeZone = zone;
            lastSyncMs = System.currentTimeMillis();
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestClientState: failed to apply sync", e);
        }
    }

    public Map<String, QuestDefinition> definitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public QuestPlayerData playerData() {
        return playerData;
    }

    public QuestZoneVisual activeZone() {
        return activeZone;
    }

    public long lastSyncMs() {
        return lastSyncMs;
    }

    public boolean isQuestHudEnabled() {
        return getConfig().isQuestHudEnabled();
    }

    public void setQuestHudEnabled(boolean questHudEnabled) {
        QuestClientConfig cfg = getConfig();
        cfg.setQuestHudEnabled(questHudEnabled);
        cfg.save();
    }

    public QuestDefinition getActiveDefinition() {
        if (playerData == null) {
            return null;
        }
        String id = playerData.activeQuestId();
        if (id == null) {
            return null;
        }
        return definitions.get(id);
    }

    public QuestInstance getActiveInstance() {
        if (playerData == null) {
            return null;
        }
        String id = playerData.activeQuestId();
        if (id == null) {
            return null;
        }
        return playerData.quests().get(id);
    }
}
