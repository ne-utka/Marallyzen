package neutka.marallys.marallyzen.replay.camera;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReplayCameraTrack {
    private final String id;
    private final boolean loop;
    private final double durationSeconds;
    private final CameraEase defaultEase;
    private final List<ReplayCameraKeyframe> keyframes;

    public ReplayCameraTrack(String id, boolean loop, double durationSeconds, CameraEase defaultEase, List<ReplayCameraKeyframe> keyframes) {
        this.id = id;
        this.loop = loop;
        this.durationSeconds = durationSeconds;
        this.defaultEase = defaultEase == null ? CameraEase.LINEAR : defaultEase;
        this.keyframes = new ArrayList<>(keyframes);
        this.keyframes.sort(Comparator.comparingDouble(ReplayCameraKeyframe::timeSeconds));
    }

    public String getId() {
        return id;
    }

    public boolean isLoop() {
        return loop;
    }

    public double getDurationSeconds() {
        double computed = getEndTime() - getStartTime();
        if (durationSeconds > 0.0) {
            return Math.max(durationSeconds, computed);
        }
        return computed;
    }

    public CameraEase getDefaultEase() {
        return defaultEase;
    }

    public double getStartTime() {
        if (keyframes.isEmpty()) {
            return 0.0;
        }
        return keyframes.get(0).timeSeconds();
    }

    public double getEndTime() {
        if (keyframes.isEmpty()) {
            return 0.0;
        }
        double lastTime = keyframes.get(keyframes.size() - 1).timeSeconds();
        if (durationSeconds > 0.0) {
            return Math.max(lastTime, durationSeconds);
        }
        return lastTime;
    }

    public ReplayCameraState sample(double timeSeconds) {
        if (keyframes.isEmpty()) {
            return null;
        }
        if (keyframes.size() == 1) {
            return toState(keyframes.get(0));
        }

        ReplayCameraKeyframe first = keyframes.get(0);
        ReplayCameraKeyframe last = keyframes.get(keyframes.size() - 1);

        if (timeSeconds <= first.timeSeconds()) {
            return toState(first);
        }
        if (timeSeconds >= last.timeSeconds()) {
            return toState(last);
        }

        ReplayCameraKeyframe prev = first;
        ReplayCameraKeyframe next = last;
        for (int i = 1; i < keyframes.size(); i++) {
            next = keyframes.get(i);
            if (timeSeconds <= next.timeSeconds()) {
                prev = keyframes.get(i - 1);
                break;
            }
        }

        double span = Math.max(1.0E-6, next.timeSeconds() - prev.timeSeconds());
        double t = (timeSeconds - prev.timeSeconds()) / span;
        CameraEase ease = next.ease() == null ? defaultEase : next.ease();
        return CameraInterpolator.interpolate(prev, next, t, ease);
    }

    private static ReplayCameraState toState(ReplayCameraKeyframe keyframe) {
        return new ReplayCameraState(keyframe.position(), keyframe.yaw(), keyframe.pitch(), keyframe.fov());
    }
}
