package neutka.marallys.marallyzen.npc.replay;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for active replay sessions and recording sessions.
 */
public final class NpcReplayRegistry {
    public static final class RecordingSession {
        private final UUID playerId;
        private final String npcId;
        private final String scriptId;
        private final long startedGameTick;
        private final int maxTicks;
        private final List<NpcReplayFrame> frames = new ArrayList<>();
        private int recordedTicks;

        private RecordingSession(UUID playerId, String npcId, String scriptId, long startedGameTick, int maxTicks) {
            this.playerId = playerId;
            this.npcId = npcId;
            this.scriptId = scriptId;
            this.startedGameTick = startedGameTick;
            this.maxTicks = Math.max(1, maxTicks);
        }

        public UUID playerId() {
            return playerId;
        }

        public String npcId() {
            return npcId;
        }

        public String scriptId() {
            return scriptId;
        }

        public long startedGameTick() {
            return startedGameTick;
        }

        public int maxTicks() {
            return maxTicks;
        }

        public int recordedTicks() {
            return recordedTicks;
        }

        public void incrementTicks() {
            this.recordedTicks++;
        }

        public List<NpcReplayFrame> frames() {
            return frames;
        }
    }

    private final Map<String, NpcReplayState> activeReplays = new ConcurrentHashMap<>();
    private final Map<UUID, RecordingSession> recordings = new ConcurrentHashMap<>();

    public NpcReplayState getActive(String npcId) {
        if (npcId == null || npcId.isBlank()) {
            return null;
        }
        return activeReplays.get(npcId.trim().toLowerCase());
    }

    public void putActive(NpcReplayState state) {
        if (state == null || state.npcId() == null || state.npcId().isBlank()) {
            return;
        }
        activeReplays.put(state.npcId().trim().toLowerCase(), state);
    }

    public NpcReplayState removeActive(String npcId) {
        if (npcId == null || npcId.isBlank()) {
            return null;
        }
        return activeReplays.remove(npcId.trim().toLowerCase());
    }

    public List<NpcReplayState> activeSnapshot() {
        return new ArrayList<>(activeReplays.values());
    }

    public RecordingSession getRecording(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return recordings.get(playerId);
    }

    public RecordingSession getRecording(ServerPlayer player) {
        return player == null ? null : getRecording(player.getUUID());
    }

    public RecordingSession startRecording(ServerPlayer player, String npcId, String scriptId, long gameTick, int maxTicks) {
        if (player == null || npcId == null || npcId.isBlank() || scriptId == null || scriptId.isBlank()) {
            return null;
        }
        RecordingSession session = new RecordingSession(
                player.getUUID(),
                npcId.trim().toLowerCase(),
                scriptId.trim().toLowerCase(),
                gameTick,
                maxTicks
        );
        recordings.put(player.getUUID(), session);
        return session;
    }

    public RecordingSession stopRecording(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return recordings.remove(player.getUUID());
    }

    public List<RecordingSession> recordingSnapshot() {
        return new ArrayList<>(recordings.values());
    }
}
