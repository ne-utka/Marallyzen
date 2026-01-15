package neutka.marallys.marallyzen.replay.playback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.replay.ReplayChunkSnapshot;
import neutka.marallys.marallyzen.replay.ReplayData;
import neutka.marallys.marallyzen.replay.ReplayEntityFrame;
import neutka.marallys.marallyzen.replay.ReplayEntityInfo;
import neutka.marallys.marallyzen.replay.ReplayServerSnapshot;
import neutka.marallys.marallyzen.replay.ReplayServerTrack;
import neutka.marallys.marallyzen.replay.ReplayStorage;

public final class ReplayPlayer {
    private static final ReplayPlayer INSTANCE = new ReplayPlayer();

    private final Minecraft mc = Minecraft.getInstance();
    private ReplayData data;
    private ReplayWorld replayWorld;
    private ClientLevel previousWorld;
    private Entity previousCameraEntity;
    private Entity cameraEntity;
    private ReplayCamera camera;
    private ReplayCameraTrack cameraTrack;
    private final Map<UUID, GhostEntity> ghosts = new HashMap<>();
    private List<ReplayChunkSnapshot> chunkSnapshots = new ArrayList<>();
    private int nextChunkSnapshotIndex = 0;
    private long lastWorldTickApplied = -1;
    private long currentTick = 0;
    private boolean playing = false;
    private boolean paused = false;
    private int nextGhostEntityId = -4000;

    private ReplayPlayer() {
    }

    public static ReplayPlayer getInstance() {
        return INSTANCE;
    }

    public static boolean isReplayActive() {
        return INSTANCE.playing;
    }

    public boolean isPlaying() {
        return playing;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public float getCurrentFov(float partialTick) {
        if (camera == null) {
            return 70.0f;
        }
        return camera.getFov(currentTick + partialTick);
    }

    public void startFromDisk(String replayId, ReplayCameraMode mode) {
        try {
            ReplayData loaded = ReplayStorage.loadReplay(replayId);
            if (loaded == null) {
                Marallyzen.LOGGER.warn("Replay not found: {}", replayId);
                return;
            }
            start(loaded, mode);
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to load replay: {}", replayId, e);
        }
    }

    public void start(ReplayData data, ReplayCameraMode mode) {
        if (data == null || mc.level == null) {
            return;
        }
        stop();
        this.data = data;
        this.previousWorld = mc.level;
        this.previousCameraEntity = mc.getCameraEntity();
        this.replayWorld = ReplayWorld.create(previousWorld, data.getHeader());
        if (replayWorld == null) {
            return;
        }

        mc.setLevel(replayWorld, ReceivingLevelScreen.Reason.OTHER);
        this.cameraEntity = createCameraEntity(replayWorld);
        if (this.cameraEntity == null) {
            stop();
            return;
        }
        mc.setCameraEntity(cameraEntity);

        this.cameraTrack = new ReplayCameraTrack(data.getClientTrack());
        this.camera = createCamera(mode, cameraTrack);
        buildGhosts(data.getServerTrack());
        buildChunkSnapshots(data.getServerTrack());

        this.currentTick = 0;
        this.playing = true;
        this.paused = false;
        applyTickState(0.0f);

        Marallyzen.LOGGER.info("Replay started: {}", data.getId());
    }

    public void stop() {
        if (!playing && previousWorld == null) {
            return;
        }
        removeGhosts();
        if (previousWorld != null) {
            mc.setLevel(previousWorld, ReceivingLevelScreen.Reason.OTHER);
        }
        Entity restoreCamera = previousCameraEntity != null ? previousCameraEntity : mc.player;
        if (restoreCamera != null) {
            mc.setCameraEntity(restoreCamera);
        }
        this.data = null;
        this.replayWorld = null;
        this.previousWorld = null;
        this.cameraEntity = null;
        this.camera = null;
        this.cameraTrack = null;
        this.chunkSnapshots = new ArrayList<>();
        this.nextChunkSnapshotIndex = 0;
        this.lastWorldTickApplied = -1;
        this.currentTick = 0;
        this.playing = false;
        this.paused = false;
        Marallyzen.LOGGER.info("Replay stopped");
    }

    public void play() {
        if (playing) {
            this.paused = false;
        }
    }

    public void pause() {
        if (playing) {
            this.paused = true;
        }
    }

    public void setCameraMode(ReplayCameraMode mode) {
        if (!playing) {
            return;
        }
        this.camera = createCamera(mode, cameraTrack);
    }

    public void seek(long tick) {
        if (!playing) {
            return;
        }
        this.currentTick = Math.max(0, tick);
        applyTickState((float) this.currentTick);
    }

    public void tick() {
        if (!playing || paused) {
            return;
        }
        currentTick++;
        applyTickState((float) currentTick);
        if (data != null && data.getHeader() != null && currentTick >= data.getHeader().durationTicks()) {
            stop();
        }
    }

    private void applyTickState(float time) {
        if (replayWorld == null || data == null) {
            return;
        }
        applyWorldSnapshot(time);
        applyGhosts(time);
        if (camera != null && mc.gameRenderer != null) {
            camera.apply(mc.gameRenderer.getMainCamera(), time);
        }
    }

    private void applyWorldSnapshot(float time) {
        ReplayServerTrack serverTrack = data.getServerTrack();
        if (serverTrack != null && !serverTrack.getSnapshots().isEmpty()) {
            ReplayServerSnapshot prev = null;
            ReplayServerSnapshot next = null;
            for (ReplayServerSnapshot snapshot : serverTrack.getSnapshots()) {
                if (snapshot.tick() <= time) {
                    prev = snapshot;
                }
                if (snapshot.tick() > time) {
                    next = snapshot;
                    break;
                }
            }
            if (prev != null || next != null) {
                if (prev == null) {
                    prev = next;
                }
                if (next == null) {
                    next = prev;
                }
                replayWorld.applyWeather(prev.worldTime(), prev.raining(), prev.rainLevel(), prev.thundering(), prev.thunderLevel());
            }
        }
        applyChunkSnapshots((long) time);
    }

    private void applyChunkSnapshots(long tick) {
        if (replayWorld == null || chunkSnapshots == null || chunkSnapshots.isEmpty()) {
            return;
        }
        if (tick < lastWorldTickApplied) {
            replayWorld.clearChunks();
            nextChunkSnapshotIndex = 0;
        }
        while (nextChunkSnapshotIndex < chunkSnapshots.size()
            && chunkSnapshots.get(nextChunkSnapshotIndex).tick() <= tick) {
            replayWorld.applyChunkSnapshot(chunkSnapshots.get(nextChunkSnapshotIndex));
            nextChunkSnapshotIndex++;
        }
        lastWorldTickApplied = tick;
    }

    private void applyGhosts(float time) {
        for (GhostEntity ghost : ghosts.values()) {
            ghost.apply(time);
        }
    }

    private ReplayCamera createCamera(ReplayCameraMode mode, ReplayCameraTrack track) {
        if (mode == null) {
            return new FixedReplayCamera(track);
        }
        return switch (mode) {
            case FREE -> new FreeReplayCamera();
            case RECORDED_FIRST_PERSON, FIXED -> new FixedReplayCamera(track);
        };
    }

    private void buildGhosts(ReplayServerTrack serverTrack) {
        ghosts.clear();
        if (serverTrack == null || replayWorld == null) {
            return;
        }
        Map<UUID, List<ReplayEntitySample>> grouped = new HashMap<>();
        for (ReplayServerSnapshot snapshot : serverTrack.getSnapshots()) {
            for (ReplayEntityFrame frame : snapshot.entities()) {
                grouped.computeIfAbsent(frame.id(), k -> new ArrayList<>())
                    .add(new ReplayEntitySample(snapshot.tick(), frame));
            }
        }
        for (Map.Entry<UUID, List<ReplayEntitySample>> entry : grouped.entrySet()) {
            ReplayEntityInfo info = serverTrack.getEntities().get(entry.getKey());
            ReplayEntityTrack track = new ReplayEntityTrack(entry.getKey(), info, entry.getValue());
            Entity entity = createGhostEntity(replayWorld, info);
            if (entity == null) {
                continue;
            }
            ReplayEntityState startState = track.sample(0.0f);
            if (startState != null && startState.position() != null) {
                entity.setPos(startState.position().x, startState.position().y, startState.position().z);
                entity.setYRot(startState.yaw());
                entity.setXRot(startState.pitch());
                if (entity instanceof LivingEntity living) {
                    living.yHeadRot = startState.headYaw();
                    living.yBodyRot = startState.bodyYaw();
                }
            }
            replayWorld.addEntity(entity);
            ghosts.put(entry.getKey(), new GhostEntity(entity, track));
        }
    }

    private void buildChunkSnapshots(ReplayServerTrack serverTrack) {
        chunkSnapshots = new ArrayList<>();
        nextChunkSnapshotIndex = 0;
        lastWorldTickApplied = -1;
        if (serverTrack == null || serverTrack.getChunkSnapshots().isEmpty()) {
            return;
        }
        chunkSnapshots.addAll(serverTrack.getChunkSnapshots());
        chunkSnapshots.sort(Comparator.comparingLong(ReplayChunkSnapshot::tick));
    }

    private Entity createGhostEntity(ClientLevel level, ReplayEntityInfo info) {
        if (level == null) {
            return null;
        }
        Entity entity;
        if (info != null && info.player()) {
            String name = info.name() != null ? info.name() : "Ghost";
            GameProfile profile = new GameProfile(UUID.randomUUID(), name);
            if (info.skinValue() != null) {
                String signature = info.skinSignature() != null ? info.skinSignature() : "";
                profile.getProperties().put("textures", new Property("textures", info.skinValue(), signature));
            }
            entity = new RemotePlayer(level, profile);
        } else {
            String typeId = info != null ? info.entityTypeId() : "minecraft:pig";
            ResourceLocation key = ResourceLocation.tryParse(typeId);
            EntityType<?> type = key != null ? BuiltInRegistries.ENTITY_TYPE.get(key) : null;
            entity = type != null ? type.create(level) : null;
        }
        if (entity == null) {
            return null;
        }
        entity.setId(nextGhostEntityId--);
        entity.setNoGravity(true);
        entity.setSilent(true);
        entity.setInvulnerable(true);
        entity.setInvisible(false);
        entity.noPhysics = true;
        entity.setDeltaMovement(Vec3.ZERO);
        return entity;
    }

    private Entity createCameraEntity(ClientLevel level) {
        Marker marker = EntityType.MARKER.create(level);
        if (marker == null) {
            return null;
        }
        Vec3 startPos = mc.player != null ? mc.player.position() : Vec3.ZERO;
        marker.setPos(startPos.x, startPos.y, startPos.z);
        marker.setId(nextGhostEntityId--);
        marker.setNoGravity(true);
        marker.setSilent(true);
        marker.setInvisible(true);
        marker.noPhysics = true;
        level.addEntity(marker);
        return marker;
    }

    private void removeGhosts() {
        for (GhostEntity ghost : ghosts.values()) {
            if (ghost.getEntity() != null) {
                ghost.getEntity().remove(Entity.RemovalReason.DISCARDED);
            }
        }
        ghosts.clear();
        if (cameraEntity != null) {
            cameraEntity.remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
