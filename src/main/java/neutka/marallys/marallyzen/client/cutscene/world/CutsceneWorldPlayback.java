package neutka.marallys.marallyzen.client.cutscene.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.cutscene.world.CutsceneLightAccess;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldTrack;
import neutka.marallys.marallyzen.replay.playback.ReplayWorld;

import java.util.List;

public class CutsceneWorldPlayback {
    private final Minecraft mc = Minecraft.getInstance();
    private static java.lang.reflect.Method cachedBlockChanged;
    private static java.lang.reflect.Method cachedSetBlockDirty;
    private static boolean cachedRenderUpdateResolved = false;
    private static java.lang.reflect.Method cachedDestroyParticles;
    private static boolean cachedDestroyParticlesResolved = false;
    private CutsceneWorldTrack track;
    private ReplayWorld replayWorld;
    private ClientLevel previousWorld;
    private Entity previousCameraEntity;
    private Entity cameraEntity;
    private long lastAppliedTick = -1;
    private int blockIndex;
    private int blockEntityIndex;
    private int weatherIndex;
    private int particleIndex;

    public void start(CutsceneWorldTrack track) {
        if (track == null || mc.level == null) {
            Marallyzen.LOGGER.warn("CutsceneWorldPlayback.start skipped (track={} level={})",
                track != null ? "present" : "null",
                mc.level != null ? "present" : "null");
            return;
        }
        stop();
        this.track = track;
        this.previousWorld = mc.level;
        this.previousCameraEntity = mc.getCameraEntity();
        this.replayWorld = ReplayWorld.create(previousWorld, null);
        if (replayWorld == null) {
            this.track = null;
            this.previousWorld = null;
            this.previousCameraEntity = null;
            return;
        }
        logTrackSummary(track);
        swapLevel(replayWorld);
        this.cameraEntity = null;
        applyInitialSnapshot();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
        logReplayWorldState();
        resetIndices();
        lastAppliedTick = -1;
        applyTickState(0L);
        updateViewCenter();
    }

    public void stop() {
        if (replayWorld == null && previousWorld == null) {
            return;
        }
        if (cameraEntity != null) {
            cameraEntity.remove(Entity.RemovalReason.DISCARDED);
        }
        if (previousWorld != null) {
            swapLevel(previousWorld);
        }
        if (previousCameraEntity != null) {
            mc.setCameraEntity(previousCameraEntity);
        }
        replayWorld = null;
        previousWorld = null;
        previousCameraEntity = null;
        cameraEntity = null;
        track = null;
        lastAppliedTick = -1;
        resetIndices();
    }

    public void useCameraEntity(net.minecraft.world.phys.Vec3 position, float yaw, float pitch) {
        if (replayWorld == null || position == null) {
            return;
        }
        if (cameraEntity == null) {
            cameraEntity = createCameraEntity(replayWorld);
            if (cameraEntity == null) {
                return;
            }
        }
        cameraEntity.setPos(position.x, position.y, position.z);
        cameraEntity.xo = position.x;
        cameraEntity.yo = position.y;
        cameraEntity.zo = position.z;
        cameraEntity.setYRot(yaw);
        cameraEntity.setXRot(pitch);
        cameraEntity.yRotO = yaw;
        cameraEntity.xRotO = pitch;
        if (mc.getCameraEntity() != cameraEntity) {
            mc.setCameraEntity(cameraEntity);
        }
    }

    public void tick(long tick) {
        if (track == null || replayWorld == null) {
            return;
        }
        applyTickState(tick);
        updateViewCenter();
    }

    private void applyTickState(long tick) {
        if (track == null || replayWorld == null) {
            return;
        }
        if (tick < lastAppliedTick) {
            applySeekFrame(tick);
        }
        applyWeather(tick);
        applyBlockChanges(tick);
        applyBlockEntityChanges(tick);
        applyParticles(tick);
        lastAppliedTick = tick;
    }

    private void applyInitialSnapshot() {
        for (CutsceneWorldTrack.ChunkSnapshot snapshot : track.getChunks()) {
            replayWorld.applyChunkSnapshot(
                new neutka.marallys.marallyzen.replay.ReplayChunkSnapshot(
                    0L,
                    snapshot.chunkX(),
                    snapshot.chunkZ(),
                    snapshot.sections().stream()
                        .map(section -> new neutka.marallys.marallyzen.replay.ReplayChunkSectionData(
                            section.sectionIndex(),
                            section.data()
                        ))
                        .toList(),
                        snapshot.blockEntities()
                )
            );
            CutsceneLightAccess.apply(replayWorld, snapshot.chunkX(), snapshot.chunkZ(), snapshot.skyLight(),
                replayWorld.getMinSection(), LightLayer.SKY);
            CutsceneLightAccess.apply(replayWorld, snapshot.chunkX(), snapshot.chunkZ(), snapshot.blockLight(),
                replayWorld.getMinSection(), LightLayer.BLOCK);
        }
    }

    private void applySeekFrame(long tick) {
        replayWorld.clearChunks();
        applyInitialSnapshot();
        resetIndices();

        CutsceneWorldTrack.SeekFrame frame = findSeekFrame(tick);
        if (frame == null) {
            return;
        }
        if (frame.chunks() != null) {
            for (CutsceneWorldTrack.ChunkSnapshot snapshot : frame.chunks()) {
                replayWorld.applyChunkSnapshot(
                    new neutka.marallys.marallyzen.replay.ReplayChunkSnapshot(
                        frame.tick(),
                        snapshot.chunkX(),
                        snapshot.chunkZ(),
                        snapshot.sections().stream()
                            .map(section -> new neutka.marallys.marallyzen.replay.ReplayChunkSectionData(
                                section.sectionIndex(),
                                section.data()
                            ))
                            .toList(),
                        snapshot.blockEntities()
                    )
                );
                CutsceneLightAccess.apply(replayWorld, snapshot.chunkX(), snapshot.chunkZ(), snapshot.skyLight(),
                    replayWorld.getMinSection(), LightLayer.SKY);
                CutsceneLightAccess.apply(replayWorld, snapshot.chunkX(), snapshot.chunkZ(), snapshot.blockLight(),
                    replayWorld.getMinSection(), LightLayer.BLOCK);
            }
        }
        blockIndex = clampIndex(frame.blockIndex(), track.getBlockChanges().size());
        blockEntityIndex = clampIndex(frame.blockEntityIndex(), track.getBlockEntityChanges().size());
        particleIndex = clampIndex(frame.particleIndex(), track.getParticleEvents().size());
        weatherIndex = resolveWeatherIndex(frame.weatherIndex());
    }

    private CutsceneWorldTrack.SeekFrame findSeekFrame(long tick) {
        CutsceneWorldTrack.SeekFrame best = null;
        for (CutsceneWorldTrack.SeekFrame frame : track.getSeekFrames()) {
            if (frame.tick() <= tick) {
                if (best == null || frame.tick() > best.tick()) {
                    best = frame;
                }
            }
        }
        return best;
    }

    private void applyWeather(long tick) {
        List<CutsceneWorldTrack.WeatherChange> changes = track.getWeatherChanges();
        if (changes.isEmpty()) {
            CutsceneWorldTrack.Header header = track.getHeader();
            replayWorld.applyWeather(
                header.startWorldTime() + tick,
                false,
                0.0f,
                false,
                0.0f
            );
            return;
        }
        while (weatherIndex + 1 < changes.size() && changes.get(weatherIndex + 1).tick() <= tick) {
            weatherIndex++;
        }
        CutsceneWorldTrack.WeatherChange change = changes.get(weatherIndex);
        replayWorld.applyWeather(change.worldTime(), change.raining(), change.rainLevel(), change.thundering(), change.thunderLevel());
    }

    private void applyBlockChanges(long tick) {
        List<CutsceneWorldTrack.BlockChange> changes = track.getBlockChanges();
        while (blockIndex < changes.size() && changes.get(blockIndex).tick() <= tick) {
            CutsceneWorldTrack.BlockChange change = changes.get(blockIndex);
            applyBlockChange(change);
            blockIndex++;
        }
    }

    private void applyBlockEntityChanges(long tick) {
        List<CutsceneWorldTrack.BlockEntityChange> changes = track.getBlockEntityChanges();
        while (blockEntityIndex < changes.size() && changes.get(blockEntityIndex).tick() <= tick) {
            CutsceneWorldTrack.BlockEntityChange change = changes.get(blockEntityIndex);
            applyBlockEntityChange(change);
            blockEntityIndex++;
        }
    }

    private void applyParticles(long tick) {
        List<CutsceneWorldTrack.ParticleEvent> events = track.getParticleEvents();
        while (particleIndex < events.size() && events.get(particleIndex).tick() <= tick) {
            spawnParticle(events.get(particleIndex));
            particleIndex++;
        }
    }

    private void applyBlockChange(CutsceneWorldTrack.BlockChange change) {
        if (change == null || replayWorld == null) {
            return;
        }
        BlockPos pos = BlockPos.of(change.pos());
        var lookup = replayWorld.registryAccess().lookup(net.minecraft.core.registries.Registries.BLOCK);
        if (lookup.isEmpty()) {
            return;
        }
        BlockState state = NbtUtils.readBlockState(lookup.get(), change.stateTag());
        LevelChunk chunk = replayWorld.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
        if (chunk == null) {
            return;
        }
        BlockState oldState = chunk.getBlockState(pos);
        if (oldState != null && !oldState.isAir() && (state == null || state.isAir())) {
            spawnBreakParticles(pos, oldState);
        }
        chunk.setBlockState(pos, state, false);
        if (change.blockEntityTag() != null && !change.blockEntityTag().isEmpty()) {
            BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, change.blockEntityTag(), replayWorld.registryAccess());
            if (blockEntity != null) {
                chunk.setBlockEntity(blockEntity);
            }
        } else {
            chunk.removeBlockEntity(pos);
        }
        markDirty(pos, oldState, state);
    }

    private void applyBlockEntityChange(CutsceneWorldTrack.BlockEntityChange change) {
        if (change == null || replayWorld == null) {
            return;
        }
        BlockPos pos = BlockPos.of(change.pos());
        LevelChunk chunk = replayWorld.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
        if (chunk == null) {
            return;
        }
        BlockState state = chunk.getBlockState(pos);
        BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, change.tag(), replayWorld.registryAccess());
        if (blockEntity != null) {
            chunk.setBlockEntity(blockEntity);
            markDirty(pos, state, state);
        }
    }

    private void spawnParticle(CutsceneWorldTrack.ParticleEvent event) {
        if (event == null || mc.particleEngine == null) {
            return;
        }
        ParticleOptions options = decodeParticle(event);
        if (options == null) {
            return;
        }
        for (int i = 0; i < Math.max(1, event.count()); i++) {
            mc.particleEngine.createParticle(options, event.x(), event.y(), event.z(), event.dx(), event.dy(), event.dz());
        }
    }

    private ParticleOptions decodeParticle(CutsceneWorldTrack.ParticleEvent event) {
        if (event.typeId() == null || event.typeId().isBlank()) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(event.typeId());
        ParticleType<?> type = key != null ? BuiltInRegistries.PARTICLE_TYPE.get(key) : null;
        if (type == null) {
            return null;
        }
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        try {
            net.minecraft.network.RegistryFriendlyByteBuf buffer =
                new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(event.optionsData()),
                    replayWorld.registryAccess());
            @SuppressWarnings("unchecked")
            net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ParticleOptions> codec =
                (net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ParticleOptions>)
                    type.streamCodec();
            return codec.decode(buffer);
        } catch (Exception e) {
            return null;
        }
    }

    private void markDirty(BlockPos pos, BlockState oldState, BlockState newState) {
        if (replayWorld == null || mc.levelRenderer == null || pos == null) {
            return;
        }
        if (!cachedRenderUpdateResolved) {
            cachedRenderUpdateResolved = true;
            try {
                cachedBlockChanged = mc.levelRenderer.getClass()
                    .getMethod("blockChanged", BlockPos.class, BlockState.class, BlockState.class, int.class);
                cachedBlockChanged.setAccessible(true);
            } catch (Exception ignored) {
            }
            if (cachedBlockChanged == null) {
                try {
                    cachedSetBlockDirty = mc.levelRenderer.getClass()
                        .getMethod("setBlockDirty", BlockPos.class, BlockState.class, BlockState.class);
                    cachedSetBlockDirty.setAccessible(true);
                } catch (Exception ignored) {
                }
            }
        }
        try {
            if (cachedBlockChanged != null) {
                cachedBlockChanged.invoke(mc.levelRenderer, pos, oldState, newState, 0);
                return;
            }
            if (cachedSetBlockDirty != null) {
                cachedSetBlockDirty.invoke(mc.levelRenderer, pos, oldState, newState);
                return;
            }
        } catch (Exception ignored) {
        }
        mc.levelRenderer.allChanged();
    }

    private void spawnBreakParticles(BlockPos pos, BlockState state) {
        if (mc.particleEngine == null || replayWorld == null || pos == null || state == null || state.isAir()) {
            return;
        }
        if (!cachedDestroyParticlesResolved) {
            cachedDestroyParticlesResolved = true;
            try {
                cachedDestroyParticles = mc.particleEngine.getClass()
                    .getMethod("destroy", BlockPos.class, BlockState.class);
                cachedDestroyParticles.setAccessible(true);
            } catch (Exception ignored) {
            }
            if (cachedDestroyParticles == null) {
                try {
                    cachedDestroyParticles = mc.particleEngine.getClass()
                        .getMethod("destroyBlock", BlockPos.class, BlockState.class);
                    cachedDestroyParticles.setAccessible(true);
                } catch (Exception ignored) {
                }
            }
        }
        if (cachedDestroyParticles != null) {
            try {
                cachedDestroyParticles.invoke(mc.particleEngine, pos, state);
            } catch (Exception ignored) {
            }
        }
    }

    private Entity createCameraEntity(ClientLevel level) {
        Marker marker = EntityType.MARKER.create(level);
        if (marker == null) {
            return null;
        }
        marker.setPos(0, 0, 0);
        marker.setId(-4500);
        marker.setNoGravity(true);
        marker.setSilent(true);
        marker.setInvisible(true);
        marker.noPhysics = true;
        level.addEntity(marker);
        return marker;
    }

    private void resetIndices() {
        blockIndex = 0;
        blockEntityIndex = 0;
        weatherIndex = 0;
        particleIndex = 0;
    }

    private void logTrackSummary(CutsceneWorldTrack track) {
        CutsceneWorldTrack.Header header = track.getHeader();
        int blockEntityCount = 0;
        for (CutsceneWorldTrack.ChunkSnapshot snapshot : track.getChunks()) {
            if (snapshot.blockEntities() != null) {
                blockEntityCount += snapshot.blockEntities().size();
            }
        }
        Marallyzen.LOGGER.info(
            "CutsceneWorldPlayback track summary: chunks={} blockEntities={} bounds=({}, {}, {})..({}, {}, {}) dim={}",
            track.getChunks().size(),
            blockEntityCount,
            header.minX(),
            header.minY(),
            header.minZ(),
            header.maxX(),
            header.maxY(),
            header.maxZ(),
            header.dimension()
        );
    }

    private void logReplayWorldState() {
        if (replayWorld == null) {
            return;
        }
        int loaded = replayWorld.getChunkSource().getLoadedChunksCount();
        neutka.marallys.marallyzen.replay.playback.ReplayChunkCache.DebugSummary summary =
            replayWorld.buildDebugSummary();
        Marallyzen.LOGGER.info(
            "CutsceneWorldPlayback replay world: loadedChunks={} minSection={} sections={} nonAirChunks={} nonAirSections={}/{}",
            loaded,
            replayWorld.getMinSection(),
            replayWorld.getSectionsCount(),
            summary.nonAirChunks(),
            summary.nonAirSections(),
            summary.totalSections()
        );
        if (track != null) {
            CutsceneWorldTrack.Header header = track.getHeader();
            int sampleX = (header.minX() + header.maxX()) / 2;
            int sampleZ = (header.minZ() + header.maxZ()) / 2;
            int minY = replayWorld.getMinBuildHeight();
            int maxY = replayWorld.getMaxBuildHeight() - 1;
            int sampleY = (header.minY() + header.maxY()) / 2;
            if (sampleY < minY) {
                sampleY = minY;
            } else if (sampleY > maxY) {
                sampleY = maxY;
            }
            BlockPos samplePos = new BlockPos(sampleX, sampleY, sampleZ);
            BlockState sampleState = replayWorld.getBlockState(samplePos);
            LevelChunk sampleChunk = replayWorld.getChunkSource().getChunk(sampleX >> 4, sampleZ >> 4, ChunkStatus.FULL, false);
            String chunkInfo = sampleChunk != null
                ? String.format("chunk=%d,%d sections=%d", sampleChunk.getPos().x, sampleChunk.getPos().z,
                sampleChunk.getSections().length)
                : "chunk=null";
            Marallyzen.LOGGER.info(
                "CutsceneWorldPlayback sample block: pos={} state={} air={} {}",
                samplePos,
                sampleState,
                sampleState.isAir(),
                chunkInfo
            );
            if (sampleState.isAir()) {
                BlockState firstSolid = null;
                int firstSolidY = Integer.MIN_VALUE;
                for (int y = maxY; y >= minY; y--) {
                    BlockState state = replayWorld.getBlockState(new BlockPos(sampleX, y, sampleZ));
                    if (!state.isAir()) {
                        firstSolid = state;
                        firstSolidY = y;
                        break;
                    }
                }
                if (firstSolid != null) {
                    Marallyzen.LOGGER.info(
                        "CutsceneWorldPlayback column sample: firstSolidY={} state={}",
                        firstSolidY,
                        firstSolid
                    );
                } else {
                    Marallyzen.LOGGER.info("CutsceneWorldPlayback column sample: all air");
                }
            }
        }
    }

    private void swapLevel(ClientLevel level) {
        if (level == null) {
            return;
        }
        if (mc.level == level) {
            return;
        }
        mc.level = level;
        updatePlayerLevel(level);
        updateGameModeLevel(level);
        syncConnectionLevel(level);
        updateLevelInEngines(level);
        if (mc.levelRenderer != null) {
            mc.levelRenderer.setLevel(level);
        }
        if (mc.particleEngine != null) {
            mc.particleEngine.setLevel(level);
        }
        updateGameRendererLevel(level);
    }

    private void updatePlayerLevel(ClientLevel level) {
        if (mc.player == null) {
            return;
        }
        try {
            java.lang.reflect.Method method =
                net.minecraft.world.entity.Entity.class.getDeclaredMethod("setLevel", net.minecraft.world.level.Level.class);
            method.setAccessible(true);
            method.invoke(mc.player, level);
            return;
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("level");
            field.setAccessible(true);
            field.set(mc.player, level);
        } catch (Exception ignored) {
        }
    }

    private void updateGameModeLevel(ClientLevel level) {
        if (mc.gameMode == null) {
            return;
        }
        try {
            java.lang.reflect.Method method = mc.gameMode.getClass().getDeclaredMethod("setLevel", ClientLevel.class);
            method.setAccessible(true);
            method.invoke(mc.gameMode, level);
            return;
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field field = mc.gameMode.getClass().getDeclaredField("level");
            field.setAccessible(true);
            field.set(mc.gameMode, level);
        } catch (Exception ignored) {
        }
    }

    private void updateGameRendererLevel(ClientLevel level) {
        if (mc.gameRenderer == null) {
            return;
        }
        try {
            java.lang.reflect.Method method = mc.gameRenderer.getClass().getDeclaredMethod("setLevel", ClientLevel.class);
            method.setAccessible(true);
            method.invoke(mc.gameRenderer, level);
        } catch (Exception ignored) {
        }
    }

    private void syncConnectionLevel(ClientLevel level) {
        net.minecraft.client.multiplayer.ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return;
        }
        try {
            java.lang.reflect.Method method = connection.getClass().getDeclaredMethod("setLevel", ClientLevel.class);
            method.setAccessible(true);
            method.invoke(connection, level);
            return;
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field field = connection.getClass().getDeclaredField("level");
            field.setAccessible(true);
            field.set(connection, level);
        } catch (Exception ignored) {
        }
    }

    private void updateLevelInEngines(ClientLevel level) {
        try {
            java.lang.reflect.Method method = Minecraft.class.getDeclaredMethod("updateLevelInEngines", ClientLevel.class);
            method.setAccessible(true);
            method.invoke(mc, level);
        } catch (Exception ignored) {
        }
    }

    private void updateViewCenter() {
        if (replayWorld == null || mc.gameRenderer == null) {
            return;
        }
        net.minecraft.world.phys.Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        int chunkX = Mth.floor(camPos.x) >> 4;
        int chunkZ = Mth.floor(camPos.z) >> 4;
        replayWorld.getChunkSource().forceUpdateViewCenter(chunkX, chunkZ);
    }


    private int resolveWeatherIndex(int frameIndex) {
        List<CutsceneWorldTrack.WeatherChange> changes = track.getWeatherChanges();
        if (changes.isEmpty()) {
            return 0;
        }
        int index = frameIndex - 1;
        if (index < 0) {
            return 0;
        }
        if (index >= changes.size()) {
            return changes.size() - 1;
        }
        return index;
    }

    private static int clampIndex(int value, int size) {
        if (size <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(value, size));
    }
}
