package neutka.marallys.marallyzen.director;

import java.util.ArrayList;
import java.util.List;

public final class EventTrack implements DirectorTrack<DirectorEvent> {
    private final List<Keyframe<DirectorEvent>> events = new ArrayList<>();

    @Override
    public List<Keyframe<DirectorEvent>> keyframes() {
        return events;
    }
}
