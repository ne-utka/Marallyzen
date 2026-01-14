package neutka.marallys.marallyzen.replay.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.ReplayRecordPacket;
import neutka.marallys.marallyzen.replay.ReplayData;
import neutka.marallys.marallyzen.replay.ReplayHeader;
import neutka.marallys.marallyzen.replay.ReplayMarker;
import neutka.marallys.marallyzen.replay.ReplayServerTrack;
import neutka.marallys.marallyzen.replay.ReplayStorage;
import neutka.marallys.marallyzen.replay.ReplaySettings;
import neutka.marallys.marallyzen.replay.server.ReplayServerRecorder;
import neutka.marallys.marallyzen.replay.server.ReplayServerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ReplayManager {
    private static String activeReplayId;
    private static List<ReplayMarker> activeMarkers = new ArrayList<>();

    private ReplayManager() {
    }

    public static boolean isRecording() {
        return activeReplayId != null && ReplayClientRecorder.getInstance().isRecording();
    }

    public static void startRecording(String replayId, List<ReplayMarker> markers) {
        if (activeReplayId != null || replayId == null || replayId.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        activeReplayId = replayId;
        activeMarkers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);

        int keyframeInterval = ReplaySettings.DEFAULT_KEYFRAME_INTERVAL;
        ReplayClientRecorder.getInstance().start(keyframeInterval);
        startServerRecording(mc, replayId, keyframeInterval);
        Marallyzen.LOGGER.info("Replay recording started: {}", replayId);
    }

    public static void stopRecording() {
        if (activeReplayId == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var clientTrack = ReplayClientRecorder.getInstance().stop();
        ReplayServerResult serverResult = stopServerRecording(mc);
        ReplayServerTrack serverTrack = serverResult != null ? serverResult.track() : null;
        int keyframeInterval = serverResult != null ? serverResult.keyframeInterval()
            : ReplayClientRecorder.getInstance().getKeyframeInterval();

        long duration = Math.max(lastClientTick(clientTrack), lastServerTick(serverTrack));
        String dimension = mc.level != null ? mc.level.dimension().location().toString() : "";
        ReplayHeader header = new ReplayHeader(1, ReplaySettings.DEFAULT_TICK_RATE, keyframeInterval, duration, dimension);

        try {
            if (serverTrack != null) {
                ReplayData data = new ReplayData(activeReplayId, header, activeMarkers, serverTrack, clientTrack);
                ReplayStorage.saveReplay(data);
            } else {
                ReplayStorage.saveClientTrack(activeReplayId, header, activeMarkers, clientTrack);
            }
            Marallyzen.LOGGER.info("Replay recording saved: {}", activeReplayId);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to save replay recording: {}", activeReplayId, e);
        }

        activeReplayId = null;
        activeMarkers = new ArrayList<>();
    }

    public static void updateMarkers(List<ReplayMarker> markers) {
        if (activeReplayId == null) {
            return;
        }
        activeMarkers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
    }

    public static void pause() {
        if (!isRecording()) {
            return;
        }
        ReplayClientRecorder.getInstance().pause();
        sendServerAction(ReplayRecordPacket.ACTION_PAUSE);
    }

    public static void resume() {
        if (!isRecording()) {
            return;
        }
        ReplayClientRecorder.getInstance().resume();
        sendServerAction(ReplayRecordPacket.ACTION_RESUME);
    }

    private static void startServerRecording(Minecraft mc, String replayId, int keyframeInterval) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            CompletableFuture<Void> future = server.submit(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
                if (player != null) {
                    ReplayServerRecorder.start(player, replayId, keyframeInterval);
                }
                return null;
            });
            future.join();
            return;
        }
        NetworkHelper.sendToServer(new ReplayRecordPacket(replayId, ReplayRecordPacket.ACTION_START, keyframeInterval));
    }

    private static ReplayServerResult stopServerRecording(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            CompletableFuture<ReplayServerResult> future = server.submit(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
                return player == null ? null : ReplayServerRecorder.stop(player);
            });
            return future.join();
        }
        sendServerAction(ReplayRecordPacket.ACTION_STOP);
        return null;
    }

    private static void sendServerAction(byte action) {
        if (activeReplayId == null) {
            return;
        }
        int keyframeInterval = ReplayClientRecorder.getInstance().getKeyframeInterval();
        NetworkHelper.sendToServer(new ReplayRecordPacket(activeReplayId, action, keyframeInterval));
    }

    private static long lastClientTick(neutka.marallys.marallyzen.replay.ReplayClientTrack track) {
        if (track == null || track.getFrames().isEmpty()) {
            return 0;
        }
        return track.getFrames().get(track.getFrames().size() - 1).getTick();
    }

    private static long lastServerTick(ReplayServerTrack track) {
        if (track == null || track.getSnapshots().isEmpty()) {
            return 0;
        }
        return track.getSnapshots().get(track.getSnapshots().size() - 1).tick();
    }
}
