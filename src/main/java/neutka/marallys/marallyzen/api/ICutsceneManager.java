package neutka.marallys.marallyzen.api;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * API for managing cutscenes in Marallyzen.
 * Allows other mods to create and control cinematic camera movements.
 */
public interface ICutsceneManager {

    /**
     * Play a cutscene by name.
     *
     * @param sceneName The scene ID to play
     */
    void playScene(String sceneName);

    /**
     * Stop the currently playing cutscene.
     */
    void stopScene();

    /**
     * Check if a cutscene is currently playing.
     *
     * @return true if a cutscene is active
     */
    boolean isScenePlaying();

    /**
     * Register a cutscene programmatically.
     *
     * @param sceneId The unique scene ID
     * @param keyframes List of camera keyframes
     * @param loop Whether the scene should loop
     * @param interpolationSpeed How fast to interpolate (0.0-1.0)
     */
    void registerScene(String sceneId, List<CameraKeyframe> keyframes, boolean loop, float interpolationSpeed);

    /**
     * Get all registered scene IDs.
     *
     * @return List of scene IDs
     */
    List<String> getAllSceneIds();

    /**
     * Check if a scene is registered.
     *
     * @param sceneId The scene ID
     * @return true if the scene exists
     */
    boolean hasScene(String sceneId);

    /**
     * Represents a camera keyframe in a cutscene.
     */
    class CameraKeyframe {
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



