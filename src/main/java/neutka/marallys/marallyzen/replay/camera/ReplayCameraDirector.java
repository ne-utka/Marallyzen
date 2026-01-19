package neutka.marallys.marallyzen.replay.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.client.camera.CameraController;
import neutka.marallys.marallyzen.client.camera.CameraManager;
import neutka.marallys.marallyzen.client.cutscene.editor.CutscenePreviewPlayer;
import neutka.marallys.marallyzen.replay.ReplayCompat;

public final class ReplayCameraDirector {
    private static final ReplayCameraDirector INSTANCE = new ReplayCameraDirector();

    private ReplayCameraTrack activeTrack;
    private boolean active;

    private ReplayCameraDirector() {
    }

    public static ReplayCameraDirector getInstance() {
        return INSTANCE;
    }

    public boolean playTrack(String id) {
        ReplayCameraTrack track = ReplayCameraTrackLoader.getTrack(id);
        if (track == null) {
            return false;
        }
        this.activeTrack = track;
        this.active = true;
        CameraManager.getInstance().getCameraController().activate(true);
        return true;
    }

    public void stop() {
        this.active = false;
        this.activeTrack = null;
        CameraManager.getInstance().getCameraController().deactivate();
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        if (!active || activeTrack == null) {
            return;
        }
        if (!ReplayCompat.isReplayActive()) {
            stop();
            return;
        }
        if (Minecraft.getInstance().level == null) {
            return;
        }
        if (CutscenePreviewPlayer.isPreviewActive() || CameraManager.getInstance().isScenePlaying()) {
            return;
        }

        double timeSeconds = ReplayCompat.getReplayTimeSeconds();
        double start = activeTrack.getStartTime();
        double end = activeTrack.getEndTime();
        double duration = activeTrack.getDurationSeconds();

        if (duration > 0.0 && activeTrack.isLoop()) {
            double local = (timeSeconds - start) % duration;
            if (local < 0.0) {
                local += duration;
            }
            timeSeconds = start + local;
        } else if (timeSeconds > end) {
            stop();
            return;
        }

        ReplayCameraState state = activeTrack.sample(timeSeconds);
        if (state == null) {
            return;
        }

        CameraController cameraController = CameraManager.getInstance().getCameraController();
        Vec3 position = state.position();
        cameraController.setRawState(position, state.yaw(), state.pitch(), state.fov());
    }
}
