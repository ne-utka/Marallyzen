package neutka.marallys.marallyzen.replay;

public final class LegacyReplayGate {
    private static final boolean LEGACY_REPLAY_ENABLED = false;

    private LegacyReplayGate() {
    }

    public static boolean isLegacyReplayEnabled() {
        return LEGACY_REPLAY_ENABLED;
    }
}
