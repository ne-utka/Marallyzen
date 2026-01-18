package neutka.marallys.marallyzen.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.audio.AudioMetadata;
import neutka.marallys.marallyzen.audio.MarallyzenAudioService;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public final class RadioPlaybackManager {
    private static final Map<java.util.UUID, RadioSession> sessions = new HashMap<>();
    private static final float RADIO_RADIUS = 16.0f;

    private RadioPlaybackManager() {}

    public static void toggle(ServerPlayer player, BlockPos pos, String stationName) {
        if (player == null || pos == null) {
            return;
        }
        RadioSession session = sessions.get(player.getUUID());
        if (session != null && session.enabled && session.pos.equals(pos)) {
            stopSession(player);
            return;
        }
        startSession(player, pos, stationName);
    }

    public static void switchTrack(ServerPlayer player, BlockPos pos, String stationName) {
        if (player == null || pos == null) {
            return;
        }
        RadioSession session = sessions.get(player.getUUID());
        if (session == null || !session.enabled || !session.pos.equals(pos)) {
            return;
        }
        setStationByName(session, stationName);
        restartPlayback(player, session);
    }

    private static void startSession(ServerPlayer player, BlockPos pos, String stationName) {
        ServerLevel level = player.serverLevel();
        if (level == null) {
            return;
        }
        if (!level.getBlockState(pos).is(MarallyzenBlocks.RADIO.get())) {
            return;
        }
        List<RadioTrackRegistry.Station> stations = RadioTrackRegistry.getStations();
        if (stations.isEmpty()) {
            Marallyzen.LOGGER.warn("RadioPlaybackManager: no tracks found for radio at {}", pos);
            return;
        }
        stopSession(player);
        RadioSession session = new RadioSession(pos, stations);
        setStationByName(session, stationName);
        sessions.put(player.getUUID(), session);
        playNext(player, session);
    }

    private static void stopSession(ServerPlayer player) {
        RadioSession session = sessions.remove(player.getUUID());
        if (session == null) {
            return;
        }
        session.enabled = false;
        if (session.timer != null) {
            session.timer.cancel();
            session.timer = null;
        }
        if (session.currentPlayback != null) {
            session.currentPlayback.stop();
            session.currentPlayback = null;
        }
    }

    private static void playNext(ServerPlayer player, RadioSession session) {
        if (player == null || session == null || !session.enabled) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level == null) {
            return;
        }
        if (!level.getBlockState(session.pos).is(MarallyzenBlocks.RADIO.get())) {
            stopSession(player);
            return;
        }

        int totalTracks = session.totalTracks();
        if (totalTracks <= 0) {
            stopSession(player);
            return;
        }

        String track = null;
        for (int attempts = 0; attempts < totalTracks; attempts++) {
            RadioTrackRegistry.Station station = session.stations.get(session.stationIndex);
            if (station.tracks().isEmpty()) {
                session.stationIndex = (session.stationIndex + 1) % session.stations.size();
                session.trackIndex = 0;
                continue;
            }
            String candidate = station.tracks().get(session.trackIndex);
            session.trackIndex = (session.trackIndex + 1) % station.tracks().size();
            Path audioPath = MarallyzenAudioService.getAudioBaseDir().resolve(candidate);
            if (java.nio.file.Files.exists(audioPath)) {
                track = candidate;
                break;
            }
            Marallyzen.LOGGER.warn("RadioPlaybackManager: missing track {}, skipping", audioPath);
            if (session.trackIndex == 0) {
                session.stationIndex = (session.stationIndex + 1) % session.stations.size();
            }
        }
        if (track == null) {
            stopSession(player);
            return;
        }

        Vec3 position = Vec3.atCenterOf(session.pos);
        MarallyzenAudioService.PlaybackHandle handle = MarallyzenAudioService.playRadioAudioWithHandle(
            level,
            position,
            track,
            RADIO_RADIUS,
            true,
            java.util.Collections.singletonList(player)
        );
        session.currentPlayback = handle;

        long durationMs = handle.durationMs();
        long delayMs = durationMs > 0 ? durationMs : Math.max(3000L, AudioMetadata.getDurationMs(track));
        scheduleNext(player, session, delayMs + 50L);
    }

    private static void scheduleNext(ServerPlayer player, RadioSession session, long delayMs) {
        if (!session.enabled) {
            return;
        }
        if (session.timer != null) {
            session.timer.cancel();
        }
        session.timer = new Timer();
        session.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (player.server == null) {
                    return;
                }
                player.server.execute(() -> playNext(player, session));
            }
        }, delayMs);
    }

    private static void restartPlayback(ServerPlayer player, RadioSession session) {
        if (session.timer != null) {
            session.timer.cancel();
            session.timer = null;
        }
        if (session.currentPlayback != null) {
            session.currentPlayback.stop();
            session.currentPlayback = null;
        }
        session.trackIndex = 0;
        playNext(player, session);
    }

    private static void setStationByName(RadioSession session, String stationName) {
        if (session == null || session.stations.isEmpty()) {
            return;
        }
        if (stationName == null || stationName.isBlank()) {
            session.stationIndex = 0;
            session.trackIndex = 0;
            return;
        }
        for (int i = 0; i < session.stations.size(); i++) {
            if (session.stations.get(i).name().equals(stationName)) {
                session.stationIndex = i;
                session.trackIndex = 0;
                return;
            }
        }
        session.stationIndex = 0;
        session.trackIndex = 0;
    }

    private static final class RadioSession {
        private final BlockPos pos;
        private final List<RadioTrackRegistry.Station> stations;
        private int stationIndex;
        private int trackIndex;
        private boolean enabled = true;
        private Timer timer;
        private MarallyzenAudioService.PlaybackHandle currentPlayback;

        private RadioSession(BlockPos pos, List<RadioTrackRegistry.Station> stations) {
            this.pos = pos;
            this.stations = stations;
            this.stationIndex = 0;
            this.trackIndex = 0;
        }

        private int totalTracks() {
            int total = 0;
            for (RadioTrackRegistry.Station station : stations) {
                total += station.tracks().size();
            }
            return total;
        }
    }
}
