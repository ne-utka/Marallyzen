package neutka.marallys.marallyzen.replay.playback;

import net.minecraft.world.phys.Vec3;

public record ReplayEntityState(Vec3 position, float yaw, float pitch, float headYaw, float bodyYaw) {
}
