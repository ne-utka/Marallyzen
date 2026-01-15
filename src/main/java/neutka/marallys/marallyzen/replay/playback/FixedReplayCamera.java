package neutka.marallys.marallyzen.replay.playback;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.replay.ReplayCameraFrame;

public final class FixedReplayCamera implements ReplayCamera {
    private final ReplayCameraTrack track;

    public FixedReplayCamera(ReplayCameraTrack track) {
        this.track = track;
    }

    @Override
    public void apply(Camera camera, float time) {
        if (camera == null || track == null) {
            return;
        }
        ReplayCameraFrame prev = track.sample(time - 1.0f);
        ReplayCameraFrame cur = track.sample(time);
        if (cur == null) {
            return;
        }
        Entity entity = camera.getEntity();
        if (entity == null) {
            return;
        }
        if (prev != null && prev.position() != null) {
            entity.xo = prev.position().x;
            entity.yo = prev.position().y;
            entity.zo = prev.position().z;
        } else if (cur.position() != null) {
            entity.xo = cur.position().x;
            entity.yo = cur.position().y;
            entity.zo = cur.position().z;
        }
        Vec3 pos = cur.position();
        if (pos != null) {
            entity.setPos(pos.x, pos.y, pos.z);
        }
        entity.setYRot(cur.yaw());
        entity.setXRot(cur.pitch());
    }

    @Override
    public float getFov(float time) {
        ReplayCameraFrame frame = track == null ? null : track.sample(time);
        return frame != null ? frame.fov() : 70.0f;
    }
}
