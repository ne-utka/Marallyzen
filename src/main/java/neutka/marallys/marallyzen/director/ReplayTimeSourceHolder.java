package neutka.marallys.marallyzen.director;

public final class ReplayTimeSourceHolder {
    private static final ReplayTimeSource FALLBACK = new ReplayTimeSource() {
        @Override
        public long getTimestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public float getSpeed() {
            return 1.0f;
        }

        @Override
        public boolean isPlaying() {
            return true;
        }
    };

    private static volatile ReplayTimeSource current = FALLBACK;

    private ReplayTimeSourceHolder() {
    }

    public static ReplayTimeSource get() {
        return current;
    }

    public static void set(ReplayTimeSource source) {
        current = source != null ? source : FALLBACK;
    }
}
