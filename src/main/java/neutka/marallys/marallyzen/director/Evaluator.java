package neutka.marallys.marallyzen.director;

import java.util.List;

public interface Evaluator<T> {
    T evaluate(List<Keyframe<T>> keys, long t);
}
