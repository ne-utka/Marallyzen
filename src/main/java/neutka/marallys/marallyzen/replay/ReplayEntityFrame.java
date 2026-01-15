package neutka.marallys.marallyzen.replay;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record ReplayEntityFrame(UUID id, Vec3 position, float yaw, float pitch, float headYaw, float bodyYaw) {
}
