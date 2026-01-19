package neutka.marallys.marallyzen.replay.camera;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class CameraInterpolator {
    private CameraInterpolator() {
    }

    public static ReplayCameraState interpolate(ReplayCameraKeyframe from, ReplayCameraKeyframe to, double t, CameraEase ease) {
        double eased = (ease == null ? CameraEase.LINEAR : ease).apply(t);
        Vec3 position = lerp(from.position(), to.position(), eased);
        float yaw = lerpAngle(from.yaw(), to.yaw(), (float) eased);
        float pitch = Mth.lerp((float) eased, from.pitch(), to.pitch());
        float fov = Mth.lerp((float) eased, from.fov(), to.fov());
        return new ReplayCameraState(position, yaw, pitch, fov);
    }

    private static Vec3 lerp(Vec3 start, Vec3 end, double t) {
        if (start == null || end == null) {
            return start == null ? Vec3.ZERO : start;
        }
        return new Vec3(
            Mth.lerp(t, start.x, end.x),
            Mth.lerp(t, start.y, end.y),
            Mth.lerp(t, start.z, end.z)
        );
    }

    private static float lerpAngle(float start, float end, float t) {
        float diff = end - start;
        if (Math.abs(start) <= 180.0f && Math.abs(end) <= 180.0f) {
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;
        }
        return start + diff * t;
    }
}
