package neutka.marallys.marallyzen.client.cutscene.editor;

import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for cutscene editor.
 * Stores keyframes with timestamps and supports various keyframe types.
 */
public class CutsceneEditorData {
    private String id;
    private final List<EditorKeyframe> keyframes;
    private long totalDuration; // in ticks
    private CutsceneRecorder.RecordedActorTracks recordedActorTracks;
    private CutsceneWorldTrack worldTrack;

    public CutsceneEditorData(String id) {
        this.id = id;
        this.keyframes = new ArrayList<>();
        this.totalDuration = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<EditorKeyframe> getKeyframes() {
        return new ArrayList<>(keyframes);
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public CutsceneRecorder.RecordedActorTracks getRecordedActorTracks() {
        return recordedActorTracks;
    }

    public void setRecordedActorTracks(CutsceneRecorder.RecordedActorTracks recordedActorTracks) {
        this.recordedActorTracks = recordedActorTracks;
    }

    public CutsceneWorldTrack getWorldTrack() {
        return worldTrack;
    }

    public void setWorldTrack(CutsceneWorldTrack worldTrack) {
        this.worldTrack = worldTrack;
    }

    public void addKeyframe(EditorKeyframe keyframe) {
        keyframes.add(keyframe);
        keyframes.sort((a, b) -> Long.compare(a.getTime(), b.getTime()));
        updateTotalDuration();
    }

    public void removeKeyframe(EditorKeyframe keyframe) {
        keyframes.remove(keyframe);
        updateTotalDuration();
    }

    public void removeKeyframe(int index) {
        if (index >= 0 && index < keyframes.size()) {
            keyframes.remove(index);
            updateTotalDuration();
        }
    }

    public void updateKeyframe(int index, EditorKeyframe newKeyframe) {
        if (index >= 0 && index < keyframes.size()) {
            keyframes.set(index, newKeyframe);
            keyframes.sort((a, b) -> Long.compare(a.getTime(), b.getTime()));
            updateTotalDuration();
        }
    }

    private void updateTotalDuration() {
        long maxTime = 0;
        for (EditorKeyframe kf : keyframes) {
            long endTime = kf.getTime();
            if (kf instanceof CameraKeyframe cameraKf) {
                // Camera keyframes have duration
                endTime += cameraKf.getDuration();
            } else if (kf instanceof ActorKeyframe actorKf) {
                endTime += actorKf.getDuration();
            } else if (kf instanceof PauseKeyframe pauseKf) {
                endTime += pauseKf.getDuration();
            }
            maxTime = Math.max(maxTime, endTime);
        }
        this.totalDuration = maxTime;
    }

    /**
     * Base class for all keyframe types in the editor.
     */
    public static abstract class EditorKeyframe {
        protected final long time; // in ticks
        protected final KeyframeType type;
        protected int groupId = -1;

        public EditorKeyframe(long time, KeyframeType type) {
            this.time = time;
            this.type = type;
        }

        public long getTime() {
            return time;
        }

        public KeyframeType getType() {
            return type;
        }

        public int getGroupId() {
            return groupId;
        }

        public void setGroupId(int groupId) {
            this.groupId = groupId;
        }
    }

    /**
     * Camera keyframe - defines camera position, rotation, and FOV.
     */
    public static class CameraKeyframe extends EditorKeyframe {
        private final Vec3 position;
        private final float yaw;
        private final float pitch;
        private final float fov;
        private final long duration; // in ticks
        private final boolean smooth;

        public CameraKeyframe(long time, Vec3 position, float yaw, float pitch, float fov, long duration, boolean smooth) {
            super(time, KeyframeType.CAMERA);
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fov = fov;
            this.duration = duration;
            this.smooth = smooth;
        }

        public Vec3 getPosition() {
            return position;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public float getFov() {
            return fov;
        }

        public long getDuration() {
            return duration;
        }

        public boolean isSmooth() {
            return smooth;
        }
    }

    /**
     * Actor keyframe - defines player/actor position and rotation for playback.
     */
    public static class ActorKeyframe extends EditorKeyframe {
        private final Vec3 position;
        private final float yaw;
        private final float pitch;
        private final long duration; // in ticks

        public ActorKeyframe(long time, Vec3 position, float yaw, float pitch, long duration) {
            super(time, KeyframeType.ACTOR);
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.duration = duration;
        }

        public Vec3 getPosition() {
            return position;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public long getDuration() {
            return duration;
        }
    }

    /**
     * Pause keyframe - pauses cutscene playback for specified duration.
     */
    public static class PauseKeyframe extends EditorKeyframe {
        private final long duration; // in ticks

        public PauseKeyframe(long time, long duration) {
            super(time, KeyframeType.PAUSE);
            this.duration = duration;
        }

        public long getDuration() {
            return duration;
        }
    }

    /**
     * Emotion keyframe - plays an emote on an NPC.
     */
    public static class EmotionKeyframe extends EditorKeyframe {
        private final String npcId;
        private final String emoteId;

        public EmotionKeyframe(long time, String npcId, String emoteId) {
            super(time, KeyframeType.EMOTION);
            this.npcId = npcId;
            this.emoteId = emoteId;
        }

        public String getNpcId() {
            return npcId;
        }

        public String getEmoteId() {
            return emoteId;
        }
    }

    /**
     * Camera mode keyframe - switches between first and third person view.
     */
    public static class CameraModeKeyframe extends EditorKeyframe {
        private final CameraMode mode;

        public CameraModeKeyframe(long time, CameraMode mode) {
            super(time, KeyframeType.CAMERA_MODE);
            this.mode = mode;
        }

        public CameraMode getMode() {
            return mode;
        }
    }

    /**
     * Keyframe type enumeration.
     */
    public enum KeyframeType {
        CAMERA,
        PAUSE,
        EMOTION,
        CAMERA_MODE,
        ACTOR
    }

    /**
     * Camera mode enumeration.
     */
    public enum CameraMode {
        FIRST_PERSON,
        THIRD_PERSON
    }
}


