package neutka.marallys.marallyzen.client.camera;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Controls camera position, rotation, and FOV for cutscenes.
 * Provides smooth interpolation between camera states.
 */
public class CameraController {
    private static final Minecraft minecraft = Minecraft.getInstance();

    // Camera state
    private Vec3 position = Vec3.ZERO;
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private float fov = 70.0f; // Default FOV
    private Vec3 prevPosition = Vec3.ZERO;
    private float prevYaw = 0.0f;
    private float prevPitch = 0.0f;
    private float prevFov = 70.0f;
    private boolean hasPrev = false;

    // Interpolation state
    private Vec3 targetPosition = Vec3.ZERO;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private float targetFov = 70.0f;

    private float interpolationProgress = 1.0f; // 1.0 = at target
    private float interpolationSpeed = 0.05f; // How fast to interpolate

    // Camera attachment
    private Entity attachedEntity = null;
    private Vec3 attachmentOffset = Vec3.ZERO;

    // Control flags
    private boolean isActive = false;
    private boolean lockPlayerInput = false;
    private boolean manualStatePending = false;

    public void tick() {
        if (!isActive) return;

        Vec3 lastPosition = position;
        float lastYaw = yaw;
        float lastPitch = pitch;
        float lastFov = fov;

        // Update interpolation
        if (interpolationProgress < 1.0f) {
            interpolationProgress = Math.min(1.0f, interpolationProgress + interpolationSpeed);

            // Interpolate position
            position = lerp(position, targetPosition, interpolationProgress);

            // Interpolate rotation (handle angle wrapping)
            yaw = lerpAngle(yaw, targetYaw, interpolationProgress);
            pitch = lerp(pitch, targetPitch, interpolationProgress);

            // Interpolate FOV
            fov = lerp(fov, targetFov, interpolationProgress);
        }

        if (manualStatePending) {
            manualStatePending = false;
            return;
        }
        prevPosition = lastPosition;
        prevYaw = lastYaw;
        prevPitch = lastPitch;
        prevFov = lastFov;
        hasPrev = true;

        // Update attached entity position
        if (attachedEntity != null) {
            Vec3 entityPos = attachedEntity.position();
            targetPosition = entityPos.add(attachmentOffset);
        }

        // Apply camera transformations
        applyCameraTransformations();
    }

    private void applyCameraTransformations() {
        // Note: Direct camera manipulation is complex in modern Minecraft
        // This would typically require mixins or other advanced techniques
        // For now, we'll prepare the camera data for potential future implementation

        // Camera camera = minecraft.gameRenderer.getMainCamera();
        // camera.setPosition(position.toVector3f());
        // camera.setRotation(yaw, pitch);

        // FOV manipulation would require additional work
    }

    /**
     * Sets the camera to a specific position and rotation.
     */
    public void setCamera(Vec3 position, float yaw, float pitch, float fov, boolean smooth) {
        if (position == null) {
            position = this.position == null ? Vec3.ZERO : this.position;
        }
        prevPosition = this.position;
        prevYaw = this.yaw;
        prevPitch = this.pitch;
        prevFov = this.fov;
        hasPrev = true;
        if (smooth) {
            this.targetPosition = position;
            this.targetYaw = yaw;
            this.targetPitch = pitch;
            this.targetFov = fov;
            this.interpolationProgress = 0.0f;
        } else {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fov = fov;
            this.targetPosition = position;
            this.targetYaw = yaw;
            this.targetPitch = pitch;
            this.targetFov = fov;
            this.interpolationProgress = 1.0f;
        }
    }

    public void setRawState(Vec3 position, float yaw, float pitch, float fov) {
        if (position == null) {
            position = this.position == null ? Vec3.ZERO : this.position;
        }
        prevPosition = this.position;
        prevYaw = this.yaw;
        prevPitch = this.pitch;
        prevFov = this.fov;
        hasPrev = true;
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.fov = fov;
        this.targetPosition = position;
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.targetFov = fov;
        this.interpolationProgress = 1.0f;
        manualStatePending = true;
    }

    /**
     * Attaches camera to an entity with offset.
     */
    public void attachToEntity(Entity entity, Vec3 offset) {
        this.attachedEntity = entity;
        this.attachmentOffset = offset;
        Vec3 entityPos = entity.position();
        setCamera(entityPos.add(offset), yaw, pitch, fov, false);
    }

    /**
     * Detaches camera from entity.
     */
    public void detachFromEntity() {
        this.attachedEntity = null;
        this.attachmentOffset = Vec3.ZERO;
    }

    /**
     * Activates camera control.
     */
    public void activate(boolean lockPlayerInput) {
        this.isActive = true;
        this.lockPlayerInput = lockPlayerInput;
    }

    /**
     * Deactivates camera control and returns to normal.
     */
    public void deactivate() {
        this.isActive = false;
        this.lockPlayerInput = false;
        this.attachedEntity = null;
    }

    /**
     * Checks if camera control is active.
     */
    public boolean isActive() {
        return isActive;
    }

    public Vec3 getPosition() {
        return position;
    }

    public Vec3 getPrevPosition() {
        return prevPosition;
    }

    public Vec3 getInterpolatedPosition(float partialTick) {
        if (!hasPrev) {
            return position;
        }
        return lerp(prevPosition, position, partialTick);
    }

    public float getYaw() {
        return yaw;
    }

    public float getInterpolatedYaw(float partialTick) {
        if (!hasPrev) {
            return yaw;
        }
        return lerpAngle(prevYaw, yaw, partialTick);
    }

    public float getPitch() {
        return pitch;
    }

    public float getInterpolatedPitch(float partialTick) {
        if (!hasPrev) {
            return pitch;
        }
        return lerpAngle(prevPitch, pitch, partialTick);
    }

    public float getFov() {
        return fov;
    }

    public float getInterpolatedFov(float partialTick) {
        if (!hasPrev) {
            return fov;
        }
        return lerp(prevFov, fov, partialTick);
    }

    /**
     * Checks if player input should be locked.
     */
    public boolean shouldLockPlayerInput() {
        return lockPlayerInput;
    }

    /**
     * Sets interpolation speed (0.0 = instant, higher = slower).
     */
    public void setInterpolationSpeed(float speed) {
        this.interpolationSpeed = Math.max(0.001f, speed);
    }

    // Utility methods
    private static Vec3 lerp(Vec3 start, Vec3 end, float t) {
        if (start == null || end == null) {
            return start == null ? Vec3.ZERO : start;
        }
        return new Vec3(
            start.x + (end.x - start.x) * t,
            start.y + (end.y - start.y) * t,
            start.z + (end.z - start.z) * t
        );
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private static float lerpAngle(float start, float end, float t) {
        float diff = end - start;
        if (Math.abs(start) <= 180.0f && Math.abs(end) <= 180.0f) {
            // Handle angle wrapping for standard yaw range.
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;
        }
        return start + diff * t;
    }
}
