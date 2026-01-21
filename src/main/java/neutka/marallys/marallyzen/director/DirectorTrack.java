package neutka.marallys.marallyzen.director;

import java.util.List;

public interface DirectorTrack<T> {
    List<Keyframe<T>> keyframes();
}
