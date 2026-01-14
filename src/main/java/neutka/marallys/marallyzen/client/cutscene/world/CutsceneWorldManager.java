package neutka.marallys.marallyzen.client.cutscene.world;

import net.minecraft.client.Minecraft;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.cutscene.editor.CutsceneEditorScreen;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldStorage;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldTrack;
import neutka.marallys.marallyzen.network.CutsceneWorldRecordPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.util.HashMap;
import java.util.Map;

public final class CutsceneWorldManager {
    private static final int DEFAULT_CHUNK_RADIUS = 6;
    private static final Map<String, CutsceneWorldTrack> TRACKS = new HashMap<>();

    private CutsceneWorldManager() {
    }

    public static void startRecording(String sceneId) {
        startRecording(sceneId, DEFAULT_CHUNK_RADIUS);
    }

    public static void startRecording(String sceneId, int centerChunkX, int centerChunkZ) {
        startRecording(sceneId, DEFAULT_CHUNK_RADIUS, centerChunkX, centerChunkZ);
    }

    public static void startRecording(String sceneId, int chunkRadius) {
        startRecording(sceneId, chunkRadius, null, null);
    }

    public static void startRecording(String sceneId, int chunkRadius, Integer centerChunkX, Integer centerChunkZ) {
        if (sceneId == null || sceneId.isBlank()) {
            return;
        }
        if (centerChunkX != null && centerChunkZ != null) {
            NetworkHelper.sendToServer(new CutsceneWorldRecordPacket(
                sceneId,
                CutsceneWorldRecordPacket.ACTION_START,
                chunkRadius,
                true,
                centerChunkX,
                centerChunkZ
            ));
            return;
        }
        NetworkHelper.sendToServer(new CutsceneWorldRecordPacket(
            sceneId,
            CutsceneWorldRecordPacket.ACTION_START,
            chunkRadius
        ));
    }

    public static void stopRecording(String sceneId) {
        if (sceneId == null || sceneId.isBlank()) {
            return;
        }
        NetworkHelper.sendToServer(new CutsceneWorldRecordPacket(sceneId, CutsceneWorldRecordPacket.ACTION_STOP, 0));
    }

    public static void pauseRecording(String sceneId) {
        if (sceneId == null || sceneId.isBlank()) {
            return;
        }
        NetworkHelper.sendToServer(new CutsceneWorldRecordPacket(sceneId, CutsceneWorldRecordPacket.ACTION_PAUSE, 0));
    }

    public static void resumeRecording(String sceneId) {
        if (sceneId == null || sceneId.isBlank()) {
            return;
        }
        NetworkHelper.sendToServer(new CutsceneWorldRecordPacket(sceneId, CutsceneWorldRecordPacket.ACTION_RESUME, 0));
    }

    public static CutsceneWorldTrack getTrack(String sceneId) {
        return sceneId == null ? null : TRACKS.get(sceneId);
    }

    public static void onTrackReceived(String sceneId, CutsceneWorldTrack track) {
        if (sceneId == null || sceneId.isBlank() || track == null) {
            return;
        }
        TRACKS.put(sceneId, track);
        try {
            CutsceneWorldStorage.save(sceneId, track);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to save cutscene world track: {}", sceneId, e);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CutsceneEditorScreen editor) {
            if (sceneId.equals(editor.getEditorData().getId())) {
                editor.getEditorData().setWorldTrack(track);
            }
        }
    }
}
