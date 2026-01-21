package neutka.marallys.marallyzen.director;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DirectorEventRunner {
    private static final Set<Keyframe<?>> FIRED = new HashSet<>();
    private static final Map<String, DirectorEvent> STICKY = new HashMap<>();
    private static long lastTimeMs = -1L;

    private DirectorEventRunner() {
    }

    public static void tick(long timeMs, DirectorProject project) {
        if (project == null) {
            return;
        }
        if (lastTimeMs >= 0L && timeMs < lastTimeMs) {
            FIRED.clear();
            STICKY.clear();
        }
        lastTimeMs = timeMs;
        for (DirectorTrack<?> track : project.tracks) {
            if (track instanceof EventTrack events) {
                for (Keyframe<DirectorEvent> key : events.keyframes()) {
                    if (timeMs < key.timeMs()) {
                        continue;
                    }
                    DirectorEvent event = key.value();
                    if (event == null) {
                        continue;
                    }
                    if (event.sticky()) {
                        String group = event.group();
                        if (group != null && !group.isBlank()) {
                            STICKY.put(group, event);
                        }
                        continue;
                    }
                    if (FIRED.contains(key)) {
                        continue;
                    }
                    if (event.action() != null) {
                        event.action().run();
                    }
                    FIRED.add(key);
                }
            }
        }
        if (!STICKY.isEmpty()) {
            for (DirectorEvent event : STICKY.values()) {
                if (event != null && event.action() != null) {
                    event.action().run();
                }
            }
        }
    }

    public static void reset() {
        FIRED.clear();
        STICKY.clear();
        lastTimeMs = -1L;
    }
}
