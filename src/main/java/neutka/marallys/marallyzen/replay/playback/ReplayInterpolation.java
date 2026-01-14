package neutka.marallys.marallyzen.replay.playback;

import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.replay.ReplayCameraFrame;
import neutka.marallys.marallyzen.replay.ReplayEntityFrame;

public final class ReplayInterpolation {
    private ReplayInterpolation() {
    }

    public static ReplayEntityState lerp(ReplayEntityFrame prev, ReplayEntityFrame next, float t) {
        Vec3 pos = lerp(prev.position(), next.position(), t);
        float yaw = lerpAngle(prev.yaw(), next.yaw(), t);
        float pitch = lerpAngle(prev.pitch(), next.pitch(), t);
        float headYaw = lerpAngle(prev.headYaw(), next.headYaw(), t);
        float bodyYaw = lerpAngle(prev.bodyYaw(), next.bodyYaw(), t);
        return new ReplayEntityState(pos, yaw, pitch, headYaw, bodyYaw);
    }

    public static ReplayCameraFrame lerp(ReplayCameraFrame prev, ReplayCameraFrame next, float t) {
        Vec3 pos = lerp(prev.position(), next.position(), t);
        float yaw = lerpAngle(prev.yaw(), next.yaw(), t);
        float pitch = lerpAngle(prev.pitch(), next.pitch(), t);
        float fov = lerp(prev.fov(), next.fov(), t);
        return new ReplayCameraFrame(pos, yaw, pitch, fov);
    }

    public static Vec3 lerp(Vec3 start, Vec3 end, float t) {
        if (start == null && end == null) {
            return Vec3.ZERO;
        }
        if (start == null) {
            return end;
        }
        if (end == null) {
            return start;
        }
        return new Vec3(
            start.x + (end.x - start.x) * t,
            start.y + (end.y - start.y) * t,
            start.z + (end.z - start.z) * t
        );
    }

    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    public static float lerpAngle(float start, float end, float t) {
        float diff = end - start;
        if (Math.abs(start) <= 180.0f && Math.abs(end) <= 180.0f) {
            if (diff > 180.0f) {
                diff -= 360.0f;
            } else if (diff < -180.0f) {
                diff += 360.0f;
            }
        }
        return start + diff * t;
    }
}
