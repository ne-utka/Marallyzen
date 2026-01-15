package neutka.marallys.marallyzen.client.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side swing state cache for rendering animated chains.
 */
public final class InteractiveChainSwingVisuals {
    private static final Map<BlockPos, SwingState> STATES = new ConcurrentHashMap<>();
    private static final int MAX_SAMPLES = 8;
    private static final long RENDER_DELAY_MS = 75L;
    private static final long SETTLE_DURATION_MS = 900L;

    private InteractiveChainSwingVisuals() {}

    private static final class SwingState {
        private Vec3 anchor;
        private long prevTick;
        private long currTick;
        private final Deque<Sample> samples = new ArrayDeque<>();
        private boolean settling;
        private long settleStartMs;
        private long settleEndMs;
        private Vec3 settleStartOffset;

        private SwingState(Vec3 anchor, Vec3 offset, long tick, long clientTime) {
            this.anchor = anchor;
            this.prevTick = tick;
            this.currTick = tick;
            this.samples.addLast(new Sample(offset, clientTime, tick));
        }
    }

    private record Sample(Vec3 offset, long timeMs, long tick) {}

    public static void update(BlockPos root, Vec3 anchor, Vec3 offset, long tick, long clientTime) {
        if (root == null || offset == null || anchor == null) {
            return;
        }
        final long initialClientTime = clientTime;
        STATES.compute(root, (key, existing) -> {
            long time = initialClientTime;
            if (existing == null) {
                return new SwingState(anchor, offset, tick, time);
            }
            existing.settling = false;
            if (!existing.samples.isEmpty()) {
                Sample last = existing.samples.peekLast();
                if (last != null && time < last.timeMs) {
                    time = last.timeMs;
                }
            }
            if (tick <= existing.currTick) {
                existing.anchor = anchor;
                return existing;
            }
            existing.anchor = anchor;
            existing.prevTick = existing.currTick;
            existing.currTick = tick;
            existing.samples.addLast(new Sample(offset, time, tick));
            while (existing.samples.size() > MAX_SAMPLES) {
                existing.samples.removeFirst();
            }
            return existing;
        });
    }

    public static boolean beginSettle(BlockPos root, long nowMillis) {
        if (root == null) {
            return true;
        }
        SwingState state = STATES.get(root);
        if (state == null) {
            return true;
        }
        Sample last = state.samples.peekLast();
        if (last == null) {
            STATES.remove(root);
            return true;
        }
        state.settling = true;
        state.settleStartMs = nowMillis;
        state.settleEndMs = nowMillis + SETTLE_DURATION_MS;
        state.settleStartOffset = last.offset;
        return false;
    }

    public static void clear(BlockPos root) {
        if (root == null) {
            return;
        }
        if (root.equals(BlockPos.ZERO)) {
            STATES.clear();
            return;
        }
        STATES.remove(root);
    }

    public static Vec3 getInterpolatedOffset(BlockPos root, long nowMillis) {
        SwingState state = STATES.get(root);
        if (state == null) {
            return null;
        }
        if (state.settling) {
            Vec3 start = state.settleStartOffset;
            if (start == null || start.lengthSqr() < 1.0E-6) {
                start = new Vec3(0.0, -1.0, 0.0);
            }
            float t = (float) (nowMillis - state.settleStartMs) / (float) SETTLE_DURATION_MS;
            t = easeOutCubic(Mth.clamp(t, 0.0f, 1.0f));
            return lerpVec(t, start, new Vec3(0.0, -1.0, 0.0));
        }
        if (state.samples.isEmpty()) {
            return null;
        }

        long targetTime = nowMillis - RENDER_DELAY_MS;
        Sample first = state.samples.peekFirst();
        Sample last = state.samples.peekLast();
        if (first == null || last == null) {
            return null;
        }
        if (targetTime <= first.timeMs) {
            return first.offset;
        }
        if (targetTime >= last.timeMs) {
            return last.offset;
        }

        Sample prev = null;
        for (Sample sample : state.samples) {
            if (sample.timeMs >= targetTime) {
                if (prev == null) {
                    return sample.offset;
                }
                long span = sample.timeMs - prev.timeMs;
                if (span <= 0L) {
                    return sample.offset;
                }
                float t = (float) (targetTime - prev.timeMs) / (float) span;
                t = Mth.clamp(t, 0.0f, 1.0f);
                return lerpVec(t, prev.offset, sample.offset);
            }
            prev = sample;
        }

        return last.offset;
    }

    public static boolean isSettled(BlockPos root, long nowMillis) {
        SwingState state = STATES.get(root);
        if (state == null || !state.settling) {
            return false;
        }
        return nowMillis >= state.settleEndMs;
    }

    public static Vec3 getAnchor(BlockPos root) {
        SwingState state = STATES.get(root);
        return state != null ? state.anchor : null;
    }

    public static Map<BlockPos, SwingStateView> getSnapshot() {
        Map<BlockPos, SwingStateView> snapshot = new java.util.HashMap<>();
        for (Map.Entry<BlockPos, SwingState> entry : STATES.entrySet()) {
            SwingState state = entry.getValue();
            if (state == null) {
                continue;
            }
            Vec3 prevOffset = null;
            Vec3 currOffset = null;
            for (Sample sample : state.samples) {
                prevOffset = currOffset;
                currOffset = sample.offset;
            }
            if (currOffset == null) {
                currOffset = Vec3.ZERO;
            }
            snapshot.put(
                entry.getKey(),
                new SwingStateView(state.anchor, prevOffset, currOffset, state.prevTick, state.currTick)
            );
        }
        return snapshot;
    }

    public static Vec3 getLatestOffset(BlockPos root) {
        SwingState state = STATES.get(root);
        if (state == null || state.samples.isEmpty()) {
            return null;
        }
        Sample last = state.samples.peekLast();
        return last != null ? last.offset : null;
    }

    public static Vec3 getVelocity(BlockPos root) {
        SwingState state = STATES.get(root);
        if (state == null || state.samples.size() < 2) {
            return Vec3.ZERO;
        }
        Sample prev = null;
        Sample last = null;
        for (Sample sample : state.samples) {
            prev = last;
            last = sample;
        }
        if (last == null || prev == null) {
            return Vec3.ZERO;
        }
        long dt = last.timeMs - prev.timeMs;
        if (dt <= 0L) {
            return Vec3.ZERO;
        }
        Vec3 delta = last.offset.subtract(prev.offset);
        return delta.scale(1.0 / (double) dt);
    }

    public record SwingStateView(Vec3 anchor, Vec3 prevOffset, Vec3 currOffset, long prevTick, long currTick) {}

    private static Vec3 lerpVec(float t, Vec3 from, Vec3 to) {
        if (from == null || to == null) {
            return to != null ? to : Vec3.ZERO;
        }
        return new Vec3(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t
        );
    }

    private static float easeOutCubic(float t) {
        float u = 1.0f - t;
        return 1.0f - (u * u * u);
    }
}
