package neutka.marallys.marallyzen.director;

import net.minecraft.world.phys.Vec3;

public record CameraState(
    Vec3 position,
    Vec3 rotation,
    float fov
) {}
