package neutka.marallys.marallyzen.replay.timeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TimelineTrack {
    private final String id;
    private final boolean loop;
    private final double durationSeconds;
    private final List<TimelineEvent> events;

    public TimelineTrack(String id, boolean loop, double durationSeconds, List<TimelineEvent> events) {
        this.id = id;
        this.loop = loop;
        this.durationSeconds = durationSeconds;
        this.events = new ArrayList<>(events);
        this.events.sort(Comparator.comparingDouble(TimelineEvent::getTimeSeconds));
    }

    public String getId() {
        return id;
    }

    public boolean isLoop() {
        return loop;
    }

    public List<TimelineEvent> getEvents() {
        return events;
    }

    public double getStartTime() {
        if (events.isEmpty()) {
            return 0.0;
        }
        return events.get(0).getTimeSeconds();
    }

    public double getEndTime() {
        if (events.isEmpty()) {
            return 0.0;
        }
        double last = events.get(events.size() - 1).getTimeSeconds();
        if (durationSeconds > 0.0) {
            return Math.max(last, durationSeconds);
        }
        return last;
    }

    public double getDurationSeconds() {
        double computed = getEndTime() - getStartTime();
        if (durationSeconds > 0.0) {
            return Math.max(durationSeconds, computed);
        }
        return computed;
    }

    public void reset() {
        for (TimelineEvent event : events) {
            event.reset();
        }
    }
}
