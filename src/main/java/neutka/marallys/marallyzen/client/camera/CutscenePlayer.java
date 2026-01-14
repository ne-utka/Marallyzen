package neutka.marallys.marallyzen.client.camera;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays cutscenes by smoothly transitioning camera between predefined positions.
 * Supports scripted camera movements with timing and effects.
 */
public class CutscenePlayer {
    private final CameraController cameraController;
    private final List<CameraKeyframe> keyframes;
    private final List<SceneData.ActorKeyframe> actorKeyframes;

    private boolean isPlaying = false;
    private int currentKeyframeIndex = 0;
    private long sceneStartTime = 0;
    private long lastKeyframeTime = 0;
    private int actorKeyframeIndex = 0;
    private long lastActorKeyframeTime = 0;
    private net.minecraft.client.player.RemotePlayer actorGhost;

    public CutscenePlayer(CameraController cameraController) {
        this.cameraController = cameraController;
        this.keyframes = new ArrayList<>();
        this.actorKeyframes = new ArrayList<>();
    }

    /**
     * Starts playing a cutscene.
     */
    public void playScene(String sceneName) {
        SceneData sceneData = SceneLoader.getScene(sceneName);
        if (sceneData == null) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("Scene not found: {}", sceneName);
            return;
        }

        // Load keyframes from scene data
        keyframes.clear();
        for (SceneData.CameraKeyframe kf : sceneData.getKeyframes()) {
            addKeyframe(kf.position, kf.yaw, kf.pitch, kf.fov, kf.duration);
        }
        actorKeyframes.clear();
        actorKeyframes.addAll(sceneData.getActorKeyframes());

        if (!keyframes.isEmpty()) {
            isPlaying = true;
            currentKeyframeIndex = 0;
            sceneStartTime = System.currentTimeMillis();
            lastKeyframeTime = sceneStartTime;
            actorKeyframeIndex = 0;
            lastActorKeyframeTime = sceneStartTime;

            cameraController.activate(true); // Lock player input during cutscene
            cameraController.setInterpolationSpeed(sceneData.getInterpolationSpeed());

            // Set initial camera position
            CameraKeyframe firstKeyframe = keyframes.get(0);
            cameraController.setCamera(
                firstKeyframe.position,
                firstKeyframe.yaw,
                firstKeyframe.pitch,
                firstKeyframe.fov,
                false
            );

            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("Started cutscene: {}", sceneName);

            if (!actorKeyframes.isEmpty()) {
                spawnActorGhost(actorKeyframes.get(0));
            }
        }
    }

    /**
     * Stops the current cutscene.
     */
    public void stopScene() {
        isPlaying = false;
        cameraController.deactivate();
        keyframes.clear();
        actorKeyframes.clear();
        removeActorGhost();
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("Stopped cutscene");
    }

    /**
     * Updates cutscene playback.
     */
    public void tick() {
        if (!isPlaying) return;

        long currentTime = System.currentTimeMillis();

        // Check if we've reached the next keyframe
        if (currentKeyframeIndex < keyframes.size() - 1) {
            CameraKeyframe currentKeyframe = keyframes.get(currentKeyframeIndex);
            CameraKeyframe nextKeyframe = keyframes.get(currentKeyframeIndex + 1);

            long timeSinceKeyframeStart = currentTime - lastKeyframeTime;

            if (timeSinceKeyframeStart >= currentKeyframe.duration) {
                // Move to next keyframe
                currentKeyframeIndex++;
                lastKeyframeTime = currentTime;

                // Apply next keyframe
                cameraController.setCamera(
                    nextKeyframe.position,
                    nextKeyframe.yaw,
                    nextKeyframe.pitch,
                    nextKeyframe.fov,
                    true // Smooth transition
                );
            }
        } else {
            // Check if scene is finished
            CameraKeyframe lastKeyframe = keyframes.get(keyframes.size() - 1);
            long timeSinceLastKeyframe = currentTime - lastKeyframeTime;

            if (timeSinceLastKeyframe >= lastKeyframe.duration) {
                // Scene finished
                stopScene();
                return;
            }
        }

        tickActorGhost(currentTime);
    }

    /**
     * Checks if a cutscene is currently playing.
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Adds a keyframe to the current scene.
     */
    public void addKeyframe(Vec3 position, float yaw, float pitch, float fov, long duration) {
        keyframes.add(new CameraKeyframe(position, yaw, pitch, fov, duration));
    }

    private void spawnActorGhost(SceneData.ActorKeyframe keyframe) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (!(mc.level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
            return;
        }
        if (keyframe == null || keyframe.position == null) {
            return;
        }
        java.util.UUID uuid = java.util.UUID.randomUUID();
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(uuid, "CutsceneGhost");
        actorGhost = new net.minecraft.client.player.RemotePlayer(clientLevel, profile);
        actorGhost.setPos(keyframe.position.x, keyframe.position.y, keyframe.position.z);
        actorGhost.setYRot(keyframe.yaw);
        actorGhost.setXRot(keyframe.pitch);
        actorGhost.yBodyRot = keyframe.yaw;
        actorGhost.yHeadRot = keyframe.yaw;
        actorGhost.setInvisible(false);
        clientLevel.addEntity(actorGhost);
    }

    private void removeActorGhost() {
        if (actorGhost == null) {
            return;
        }
        actorGhost.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        actorGhost = null;
    }

    private void tickActorGhost(long currentTime) {
        if (actorGhost == null || actorKeyframes.isEmpty()) {
            return;
        }
        if (actorKeyframeIndex >= actorKeyframes.size() - 1) {
            SceneData.ActorKeyframe last = actorKeyframes.get(actorKeyframes.size() - 1);
            actorGhost.setPos(last.position.x, last.position.y, last.position.z);
            actorGhost.setYRot(last.yaw);
            actorGhost.setXRot(last.pitch);
            actorGhost.yBodyRot = last.yaw;
            actorGhost.yHeadRot = last.yaw;
            return;
        }

        SceneData.ActorKeyframe current = actorKeyframes.get(actorKeyframeIndex);
        SceneData.ActorKeyframe next = actorKeyframes.get(actorKeyframeIndex + 1);
        if (current.position == null || next.position == null) {
            return;
        }
        long elapsed = currentTime - lastActorKeyframeTime;
        long duration = Math.max(1, current.duration);
        float t = Math.min(1.0f, elapsed / (float) duration);

        Vec3 pos = lerp(current.position, next.position, t);
        float yaw = lerpAngle(current.yaw, next.yaw, t);
        float pitch = lerpAngle(current.pitch, next.pitch, t);

        actorGhost.setPos(pos.x, pos.y, pos.z);
        actorGhost.setYRot(yaw);
        actorGhost.setXRot(pitch);
        actorGhost.yBodyRot = yaw;
        actorGhost.yHeadRot = yaw;

        if (elapsed >= duration) {
            actorKeyframeIndex++;
            lastActorKeyframeTime = currentTime;
        }
    }

    private static Vec3 lerp(Vec3 start, Vec3 end, float t) {
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

    private static float lerpAngle(float start, float end, float t) {
        float diff = end - start;
        if (Math.abs(start) <= 180.0f && Math.abs(end) <= 180.0f) {
            if (diff > 180.0f) diff -= 360.0f;
            if (diff < -180.0f) diff += 360.0f;
        }
        return start + diff * t;
    }

    /**
     * Represents a camera keyframe in a cutscene.
     */
    public static class CameraKeyframe {
        public final Vec3 position;
        public final float yaw;
        public final float pitch;
        public final float fov;
        public final long duration; // in milliseconds

        public CameraKeyframe(Vec3 position, float yaw, float pitch, float fov, long duration) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fov = fov;
            this.duration = duration;
        }
    }
}
