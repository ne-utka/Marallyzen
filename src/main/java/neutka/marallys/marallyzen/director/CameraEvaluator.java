package neutka.marallys.marallyzen.director;

import java.util.List;
import net.minecraft.world.phys.Vec3;

public final class CameraEvaluator implements Evaluator<CameraState> {

    @Override
    public CameraState evaluate(List<Keyframe<CameraState>> keys, long t) {
        if (keys.isEmpty()) {
            return null;
        }

        Keyframe<CameraState> first = keys.get(0);
        if (t <= first.timeMs()) {
            return first.value();
        }

        Keyframe<CameraState> last = keys.get(keys.size() - 1);
        if (t >= last.timeMs()) {
            return last.value();
        }

        Keyframe<CameraState> a = first;
        Keyframe<CameraState> b = last;

        for (int i = 0; i < keys.size() - 1; i++) {
            Keyframe<CameraState> current = keys.get(i);
            Keyframe<CameraState> next = keys.get(i + 1);
            if (t >= current.timeMs() && t <= next.timeMs()) {
                a = current;
                b = next;
                break;
            }
        }

        long span = b.timeMs() - a.timeMs();
        float alpha = span > 0 ? (float) (t - a.timeMs()) / (float) span : 0.0f;
        alpha = clamp01(alpha);

        return interpolate(a.value(), b.value(), alpha);
    }

    private CameraState interpolate(CameraState a, CameraState b, float t) {
        Vec3 pos = a.position().lerp(b.position(), t);
        Vec3 rot = a.rotation().lerp(b.rotation(), t);
        float fov = a.fov() + (b.fov() - a.fov()) * t;
        return new CameraState(pos, rot, fov);
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }
}
