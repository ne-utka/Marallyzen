package neutka.marallys.marallyzen.director;

import java.util.ArrayList;
import java.util.List;

public final class CameraTrack implements DirectorTrack<CameraState> {
    private final List<Keyframe<CameraState>> keys = new ArrayList<>();

    @Override
    public List<Keyframe<CameraState>> keyframes() {
        return keys;
    }
}
