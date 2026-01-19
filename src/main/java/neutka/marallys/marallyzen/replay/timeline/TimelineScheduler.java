package neutka.marallys.marallyzen.replay.timeline;

import java.util.HashMap;
import java.util.Map;
import neutka.marallys.marallyzen.replay.ReplayCompat;

public final class TimelineScheduler {
    private static final TimelineScheduler INSTANCE = new TimelineScheduler();

    private TimelineTrack activeTrack;
    private boolean active;
    private double lastTimeSeconds = Double.NaN;
    private int lastCycle = 0;
    private final Map<String, String> flags = new HashMap<>();

    private TimelineScheduler() {
    }

    public static TimelineScheduler getInstance() {
        return INSTANCE;
    }

    public boolean playTrack(String id) {
        TimelineTrack track = TimelineLoader.getTrack(id);
        if (track == null) {
            return false;
        }
        this.activeTrack = track;
        this.active = true;
        this.lastTimeSeconds = Double.NaN;
        this.lastCycle = 0;
        track.reset();
        return true;
    }

    public void stop() {
        this.active = false;
        this.activeTrack = null;
    }

    public boolean isActive() {
        return active;
    }

    public void setFlag(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        flags.put(key, value == null ? "" : value);
    }

    public String getFlag(String key) {
        return flags.get(key);
    }

    public void tick() {
        if (!active || activeTrack == null) {
            return;
        }
        if (!ReplayCompat.isReplayActive()) {
            stop();
            return;
        }

        double timeSeconds = ReplayCompat.getReplayTimeSeconds();
        if (Double.isNaN(timeSeconds)) {
            return;
        }

        double start = activeTrack.getStartTime();
        double duration = activeTrack.getDurationSeconds();
        double localTime = timeSeconds;

        if (activeTrack.isLoop() && duration > 0.0) {
            int cycle = (int) Math.floor((timeSeconds - start) / duration);
            if (cycle != lastCycle) {
                activeTrack.reset();
                lastCycle = cycle;
            }
            double offset = (timeSeconds - start) % duration;
            if (offset < 0.0) {
                offset += duration;
            }
            localTime = start + offset;
        } else if (timeSeconds > activeTrack.getEndTime()) {
            stop();
            return;
        }

        if (!Double.isNaN(lastTimeSeconds) && localTime + 1.0E-6 < lastTimeSeconds) {
            activeTrack.reset();
        }
        lastTimeSeconds = localTime;

        for (TimelineEvent event : activeTrack.getEvents()) {
            if (event.isExecuted()) {
                continue;
            }
            if (localTime + 1.0E-6 >= event.getTimeSeconds()) {
                TimelineAction action = TimelineActionRegistry.get(event.getType());
                if (action != null) {
                    action.execute(event, this);
                }
                event.markExecuted();
            } else {
                break;
            }
        }
    }
}
