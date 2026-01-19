package neutka.marallys.marallyzen.replay.timeline;

import com.google.gson.JsonObject;

public class TimelineEvent {
    private final double timeSeconds;
    private final String type;
    private final String target;
    private final String value;
    private final JsonObject data;
    private boolean executed;

    public TimelineEvent(double timeSeconds, String type, String target, String value, JsonObject data) {
        this.timeSeconds = timeSeconds;
        this.type = type;
        this.target = target;
        this.value = value;
        this.data = data;
        this.executed = false;
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public String getValue() {
        return value;
    }

    public JsonObject getData() {
        return data;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void markExecuted() {
        this.executed = true;
    }

    public void reset() {
        this.executed = false;
    }
}
