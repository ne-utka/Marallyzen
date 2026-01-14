package neutka.marallys.marallyzen.replay.playback;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class FreeReplayCamera implements ReplayCamera {
    private Vec3 position = Vec3.ZERO;
    private boolean initialized = false;

    @Override
    public void apply(Camera camera, float time) {
        if (camera == null) {
            return;
        }
        Entity entity = camera.getEntity();
        if (entity == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!initialized) {
            position = entity.position();
            initialized = true;
        }
        Vec3 prevPos = position;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        if (mc.player != null) {
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
        }

        float speed = mc.options != null && mc.options.keySprint.isDown() ? 0.35f : 0.15f;
        float forward = 0.0f;
        float strafe = 0.0f;
        float vertical = 0.0f;
        if (mc.options != null) {
            if (mc.options.keyUp.isDown()) {
                forward += 1.0f;
            }
            if (mc.options.keyDown.isDown()) {
                forward -= 1.0f;
            }
            if (mc.options.keyLeft.isDown()) {
                strafe += 1.0f;
            }
            if (mc.options.keyRight.isDown()) {
                strafe -= 1.0f;
            }
            if (mc.options.keyJump.isDown()) {
                vertical += 1.0f;
            }
            if (mc.options.keyShift.isDown()) {
                vertical -= 1.0f;
            }
        }

        if (forward != 0.0f || strafe != 0.0f || vertical != 0.0f) {
            double rad = Math.toRadians(yaw);
            Vec3 forwardVec = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
            Vec3 rightVec = new Vec3(Math.cos(rad), 0.0, Math.sin(rad));
            Vec3 move = forwardVec.scale(forward * speed)
                .add(rightVec.scale(strafe * speed))
                .add(0.0, vertical * speed, 0.0);
            position = position.add(move);
        }

        entity.xo = prevPos.x;
        entity.yo = prevPos.y;
        entity.zo = prevPos.z;
        entity.setPos(position.x, position.y, position.z);
        entity.setYRot(yaw);
        entity.setXRot(pitch);
    }

    @Override
    public float getFov(float time) {
        Minecraft mc = Minecraft.getInstance();
        return mc.options != null ? mc.options.fov().get().floatValue() : 70.0f;
    }
}
