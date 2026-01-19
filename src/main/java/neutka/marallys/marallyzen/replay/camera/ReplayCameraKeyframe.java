package neutka.marallys.marallyzen.replay.camera;

import net.minecraft.world.phys.Vec3;

public record ReplayCameraKeyframe(double timeSeconds, Vec3 position, float yaw, float pitch, float fov, CameraEase ease) {
}
