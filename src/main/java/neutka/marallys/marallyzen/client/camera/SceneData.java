package neutka.marallys.marallyzen.client.camera;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for cutscene definitions.
 * Loaded from JSON configuration files.
 */
public class SceneData {
    private final String id;
    private final List<CameraKeyframe> keyframes;
    private final List<ActorKeyframe> actorKeyframes;
    private final List<AudioKeyframe> audioKeyframes;
    private boolean loop = false;
    private float interpolationSpeed = 0.05f;

    public SceneData(String id) {
        this.id = id;
        this.keyframes = new ArrayList<>();
        this.actorKeyframes = new ArrayList<>();
        this.audioKeyframes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<CameraKeyframe> getKeyframes() {
        return keyframes;
    }

    public List<ActorKeyframe> getActorKeyframes() {
        return actorKeyframes;
    }

    public void addKeyframe(Vec3 position, float yaw, float pitch, float fov, long duration) {
        keyframes.add(new CameraKeyframe(position, yaw, pitch, fov, duration));
    }

    public void addActorKeyframe(Vec3 position, float yaw, float pitch, long duration) {
        actorKeyframes.add(new ActorKeyframe(position, yaw, pitch, duration));
    }
    
    public void addAudioKeyframe(AudioKeyframe audioKeyframe) {
        audioKeyframes.add(audioKeyframe);
    }
    
    public List<AudioKeyframe> getAudioKeyframes() {
        return audioKeyframes;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public float getInterpolationSpeed() {
        return interpolationSpeed;
    }

    public void setInterpolationSpeed(float interpolationSpeed) {
        this.interpolationSpeed = interpolationSpeed;
    }

    /**
     * Represents a camera keyframe in a scene.
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

    public static class ActorKeyframe {
        public final Vec3 position;
        public final float yaw;
        public final float pitch;
        public final long duration;

        public ActorKeyframe(Vec3 position, float yaw, float pitch, long duration) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.duration = duration;
        }
    }
    
    /**
     * Represents an audio keyframe in a scene.
     */
    public static class AudioKeyframe {
        public final String filePath;      // Path to audio file (relative to config/marallyzen/audio/)
        public final boolean positional;   // Whether audio is positional or global
        public final Vec3 position;        // Position for positional audio (null for global)
        public final float radius;         // Sound radius for positional audio
        public final boolean block;        // Whether to block cutscene progression until audio finishes
        
        public AudioKeyframe(String filePath, boolean positional, Vec3 position, float radius, boolean block) {
            this.filePath = filePath;
            this.positional = positional;
            this.position = position;
            this.radius = radius;
            this.block = block;
        }
    }
}


