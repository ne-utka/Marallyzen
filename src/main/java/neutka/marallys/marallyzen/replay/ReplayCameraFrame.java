package neutka.marallys.marallyzen.replay;

import net.minecraft.world.phys.Vec3;

public record ReplayCameraFrame(Vec3 position, float yaw, float pitch, float fov) {
}
