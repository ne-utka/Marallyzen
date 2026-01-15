package neutka.marallys.marallyzen.replay.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReplayVisualChannelRegistry {
    private static final List<ReplayVisualChannel> CHANNELS = new ArrayList<>();

    private ReplayVisualChannelRegistry() {
    }

    public static void register(ReplayVisualChannel channel) {
        if (channel == null) {
            return;
        }
        CHANNELS.add(channel);
    }

    public static List<ReplayVisualChannel> getChannels() {
        return Collections.unmodifiableList(CHANNELS);
    }
}
