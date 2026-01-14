package neutka.marallys.marallyzen.replay.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import neutka.marallys.marallyzen.replay.ReplayEntityInfo;

public final class ReplayEntityTrack {
    private final UUID id;
    private final ReplayEntityInfo info;
    private final List<ReplayEntitySample> samples;
    private int lastIndex = 0;

    public ReplayEntityTrack(UUID id, ReplayEntityInfo info, List<ReplayEntitySample> samples) {
        this.id = id;
        this.info = info;
        this.samples = samples == null ? new ArrayList<>() : new ArrayList<>(samples);
    }

    public UUID getId() {
        return id;
    }

    public ReplayEntityInfo getInfo() {
        return info;
    }

    public List<ReplayEntitySample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    public ReplayEntityState sample(float time) {
        if (samples.isEmpty()) {
            return null;
        }
        ReplayEntitySample prev = null;
        ReplayEntitySample next = null;

        int start = Math.min(lastIndex, samples.size() - 1);
        if (start > 0 && samples.get(start).tick() > time) {
            start = 0;
            lastIndex = 0;
        }
        for (int i = start; i < samples.size(); i++) {
            ReplayEntitySample sample = samples.get(i);
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
}
