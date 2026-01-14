package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class QuestInstance {
    public enum State {
        NOT_STARTED,
        ACTIVE,
        COMPLETED,
        FAILED
    }

    private final String questId;
    private int definitionVersion;
    private State state;
    private int currentStepIndex;
    private final Map<String, Integer> stepProgress;
    private long startedAt;
    private long completedAt;
    private long lastUpdated;
    private boolean waitingHandover;
    private String handoverStepId;

    public QuestInstance(String questId, int definitionVersion) {
        this.questId = questId;
        this.definitionVersion = definitionVersion;
        this.state = State.NOT_STARTED;
        this.currentStepIndex = 0;
        this.stepProgress = new HashMap<>();
    }

    public String questId() {
        return questId;
    }

    public int definitionVersion() {
        return definitionVersion;
    }

    public void setDefinitionVersion(int definitionVersion) {
        this.definitionVersion = definitionVersion;
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int currentStepIndex() {
        return currentStepIndex;
    }

    public void setCurrentStepIndex(int currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
        this.lastUpdated = System.currentTimeMillis();
    }

    public Map<String, Integer> stepProgress() {
        return stepProgress;
    }

    public long startedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long completedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public long lastUpdated() {
        return lastUpdated;
    }

    public boolean isWaitingHandover() {
        return waitingHandover;
    }

    public String handoverStepId() {
        return handoverStepId;
    }

    public void setWaitingHandover(boolean waitingHandover, String handoverStepId) {
        this.waitingHandover = waitingHandover;
        this.handoverStepId = waitingHandover ? handoverStepId : null;
        this.lastUpdated = System.currentTimeMillis();
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("questId", questId);
        obj.addProperty("version", definitionVersion);
        obj.addProperty("state", state.name());
        obj.addProperty("currentStep", currentStepIndex);
        obj.addProperty("startedAt", startedAt);
        obj.addProperty("completedAt", completedAt);
        obj.addProperty("lastUpdated", lastUpdated);
        if (waitingHandover) {
            obj.addProperty("waitingHandover", true);
            if (handoverStepId != null) {
                obj.addProperty("handoverStepId", handoverStepId);
            }
        }
        JsonObject progressObj = new JsonObject();
        for (Map.Entry<String, Integer> entry : stepProgress.entrySet()) {
            progressObj.addProperty(entry.getKey(), entry.getValue());
        }
        obj.add("progress", progressObj);
        return obj;
    }

    public static QuestInstance fromJson(JsonObject obj) {
        if (obj == null || !obj.has("questId")) {
            return null;
        }
        String questId = QuestJsonUtils.getString(obj, "questId", "");
        int version = QuestJsonUtils.getInt(obj, "version", 1);
        QuestInstance instance = new QuestInstance(questId, version);
        String stateStr = QuestJsonUtils.getString(obj, "state", State.NOT_STARTED.name());
        try {
            instance.state = State.valueOf(stateStr);
        } catch (IllegalArgumentException ignored) {
            instance.state = State.NOT_STARTED;
        }
        instance.currentStepIndex = QuestJsonUtils.getInt(obj, "currentStep", 0);
        instance.startedAt = QuestJsonUtils.getLong(obj, "startedAt", 0L);
        instance.completedAt = QuestJsonUtils.getLong(obj, "completedAt", 0L);
        instance.lastUpdated = QuestJsonUtils.getLong(obj, "lastUpdated", 0L);
        instance.waitingHandover = QuestJsonUtils.getBoolean(obj, "waitingHandover", false);
        instance.handoverStepId = QuestJsonUtils.getString(obj, "handoverStepId", null);
        JsonObject progress = obj.getAsJsonObject("progress");
        if (progress != null) {
            for (String key : progress.keySet()) {
                instance.stepProgress.put(key, progress.get(key).getAsInt());
            }
        }
        return instance;
    }
}
