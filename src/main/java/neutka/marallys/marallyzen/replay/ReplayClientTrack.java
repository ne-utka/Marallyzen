package neutka.marallys.marallyzen.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReplayClientTrack {
    private final List<ReplayClientFrame> frames = new ArrayList<>();

    public List<ReplayClientFrame> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    public void addFrame(ReplayClientFrame frame) {
        if (frame == null) {
            return;
        }
        frames.add(frame);
    }
}
