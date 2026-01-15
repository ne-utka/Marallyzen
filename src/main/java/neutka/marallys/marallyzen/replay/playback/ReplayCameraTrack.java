package neutka.marallys.marallyzen.replay.playback;

import java.util.ArrayList;
import java.util.List;
import neutka.marallys.marallyzen.replay.ReplayCameraFrame;
import neutka.marallys.marallyzen.replay.ReplayClientFrame;
import neutka.marallys.marallyzen.replay.ReplayClientTrack;

public final class ReplayCameraTrack {
    private final List<ReplayCameraSample> samples = new ArrayList<>();
    private int lastIndex = 0;

    public ReplayCameraTrack(ReplayClientTrack track) {
        if (track == null) {
            return;
        }
        for (ReplayClientFrame frame : track.getFrames()) {
            ReplayCameraFrame camera = frame.getCamera();
            if (camera != null) {
                samples.add(new ReplayCameraSample(frame.getTick(), camera));
            }
        }
    }

    public ReplayCameraFrame sample(float time) {
        if (samples.isEmpty()) {
            return null;
        }
        ReplayCameraSample prev = null;
        ReplayCameraSample next = null;

        int start = Math.min(lastIndex, samples.size() - 1);
        if (start > 0 && samples.get(start).tick() > time) {
            start = 0;
            lastIndex = 0;
        }
        for (int i = start; i < samples.size(); i++) {
            ReplayCameraSample sample = samples.get(i);
            if (sample.tick() <= time) {
                prev = sample;
                lastIndex = i;
            }
            if (sample.tick() > time) {
                next = sample;
                break;
            }
        }

        if (prev == null && next == null) {
            return null;
        }
        if (prev == null) {
            prev = next;
        }
        if (next == null) {
            next = prev;
        }

        long span = Math.max(1L, next.tick() - prev.tick());
        float t = (time - prev.tick()) / (float) span;
        t = Math.max(0.0f, Math.min(1.0f, t));
        return ReplayInterpolation.lerp(prev.frame(), next.frame(), t);
    }

    private record ReplayCameraSample(long tick, ReplayCameraFrame frame) {}
}
