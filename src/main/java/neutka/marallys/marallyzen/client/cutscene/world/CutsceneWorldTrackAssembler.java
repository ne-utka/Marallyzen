package neutka.marallys.marallyzen.client.cutscene.world;

import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldStorage;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldTrack;
import neutka.marallys.marallyzen.network.CutsceneWorldTrackPacket;

import java.util.HashMap;
import java.util.Map;

public final class CutsceneWorldTrackAssembler {
    private static final Map<String, PendingTrack> PENDING = new HashMap<>();

    private CutsceneWorldTrackAssembler() {
    }

    public static void onPacket(CutsceneWorldTrackPacket packet) {
        if (packet == null || packet.sceneId() == null || packet.sceneId().isBlank()) {
            return;
        }
        PendingTrack pending = PENDING.computeIfAbsent(packet.sceneId(),
            id -> new PendingTrack(packet.totalParts()));
        if (pending.totalParts != packet.totalParts()) {
            PENDING.remove(packet.sceneId());
            return;
        }
        if (packet.partIndex() < 0 || packet.partIndex() >= pending.totalParts) {
            return;
        }
        if (pending.parts[packet.partIndex()] == null) {
            pending.parts[packet.partIndex()] = packet.payload();
            pending.received++;
        }
        if (pending.received >= pending.totalParts) {
            PENDING.remove(packet.sceneId());
            try {
                byte[] joined = join(pending.parts);
                CutsceneWorldTrack track = CutsceneWorldStorage.decode(joined);
                CutsceneWorldManager.onTrackReceived(packet.sceneId(), track);
            } catch (Exception e) {
                Marallyzen.LOGGER.warn("Failed to decode cutscene world track: {}", packet.sceneId(), e);
            }
        }
    }

    private static byte[] join(byte[][] parts) {
        int total = 0;
        for (byte[] part : parts) {
            if (part != null) {
                total += part.length;
            }
        }
        byte[] combined = new byte[total];
        int offset = 0;
        for (byte[] part : parts) {
            if (part == null) {
                continue;
            }
            System.arraycopy(part, 0, combined, offset, part.length);
            offset += part.length;
        }
        return combined;
    }

    private static final class PendingTrack {
        private final int totalParts;
        private final byte[][] parts;
        private int received;

        private PendingTrack(int totalParts) {
            this.totalParts = Math.max(1, totalParts);
            this.parts = new byte[this.totalParts][];
        }
    }
}
