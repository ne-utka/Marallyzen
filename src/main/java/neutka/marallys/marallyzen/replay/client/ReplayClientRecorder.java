package neutka.marallys.marallyzen.replay.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.client.camera.CameraManager;
import neutka.marallys.marallyzen.replay.ReplayCameraFrame;
import neutka.marallys.marallyzen.replay.ReplayClientFrame;
import neutka.marallys.marallyzen.replay.ReplayClientTrack;
import neutka.marallys.marallyzen.replay.ReplaySettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public final class ReplayClientRecorder {
    private static final ReplayClientRecorder INSTANCE = new ReplayClientRecorder();

    private final Minecraft mc = Minecraft.getInstance();
    private boolean recording = false;
    private boolean paused = false;
    private long startTick = 0;
    private long pauseStartTick = 0;
    private long totalPauseTicks = 0;
    private int keyframeInterval = ReplaySettings.DEFAULT_KEYFRAME_INTERVAL;
    private ReplayClientTrack track = new ReplayClientTrack();

    private ReplayClientRecorder() {
    }

    public static ReplayClientRecorder getInstance() {
        return INSTANCE;
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getKeyframeInterval() {
        return keyframeInterval;
    }

    public void start(int keyframeInterval) {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return;
        }
        if (recording || mc.level == null) {
            return;
        }
        this.recording = true;
        this.paused = false;
        this.startTick = mc.level.getGameTime();
        this.pauseStartTick = 0;
        this.totalPauseTicks = 0;
        this.keyframeInterval = Math.max(1, keyframeInterval);
        this.track = new ReplayClientTrack();
        ReplayEmoteStateTracker.clearAll();
        ReplayEmoteVisualChannel.reset();
    }

    public ReplayClientTrack stop() {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return track;
        }
        if (!recording) {
            return track;
        }
        recording = false;
        paused = false;
        ReplayEmoteStateTracker.clearAll();
        ReplayEmoteVisualChannel.reset();
        return track;
    }

    public void pause() {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return;
        }
        if (!recording || paused || mc.level == null) {
            return;
        }
        paused = true;
        pauseStartTick = mc.level.getGameTime();
    }

    public void resume() {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return;
        }
        if (!recording || !paused || mc.level == null) {
            return;
        }
        long pauseDuration = mc.level.getGameTime() - pauseStartTick;
        totalPauseTicks += pauseDuration;
        paused = false;
    }

    public void tick() {
        if (!neutka.marallys.marallyzen.replay.LegacyReplayGate.isLegacyReplayEnabled()) {
            return;
        }
        if (!recording || paused || mc.level == null) {
            return;
        }
        long currentTick = mc.level.getGameTime();
        long relativeTick = currentTick - startTick - totalPauseTicks;
        if (relativeTick < 0) {
            return;
        }
        if (relativeTick % keyframeInterval != 0) {
            return;
        }
        ReplayCameraFrame cameraFrame = captureCamera();
        Map<UUID, CompoundTag> visuals = captureVisuals(relativeTick, cameraFrame);
        track.addFrame(new ReplayClientFrame(relativeTick, cameraFrame, visuals, new CompoundTag()));
    }

    private ReplayCameraFrame captureCamera() {
        if (mc.player == null) {
            return new ReplayCameraFrame(Vec3.ZERO, 0.0f, 0.0f, 70.0f);
        }
        var controller = CameraManager.getInstance().getCameraController();
        if (controller.isActive()) {
            return new ReplayCameraFrame(controller.getPosition(), controller.getYaw(), controller.getPitch(), controller.getFov());
        }
        Vec3 position = mc.gameRenderer.getMainCamera().getPosition();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        float fov = mc.options != null ? mc.options.fov().get().floatValue() : 70.0f;
        return new ReplayCameraFrame(position, yaw, pitch, fov);
    }

    private Map<UUID, CompoundTag> captureVisuals(long tick, ReplayCameraFrame cameraFrame) {
        List<ReplayVisualChannel> channels = ReplayVisualChannelRegistry.getChannels();
        if (channels.isEmpty() || mc.level == null) {
            return new HashMap<>();
        }
        ReplayClientCaptureContext context = new ReplayClientCaptureContext(tick, cameraFrame, mc);
        Map<UUID, CompoundTag> visuals = new HashMap<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            CompoundTag perEntity = new CompoundTag();
            for (ReplayVisualChannel channel : channels) {
                CompoundTag channelTag = new CompoundTag();
                channel.capture(entity, context, channelTag);
                if (!channelTag.isEmpty()) {
                    perEntity.put(channel.getId(), channelTag);
                }
            }
            if (!perEntity.isEmpty()) {
                visuals.put(entity.getUUID(), perEntity);
            }
        }
        return visuals;
    }
}
