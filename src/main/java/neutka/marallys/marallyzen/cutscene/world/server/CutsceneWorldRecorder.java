package neutka.marallys.marallyzen.cutscene.world.server;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.cutscene.world.CutsceneLightAccess;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldRegion;
import neutka.marallys.marallyzen.cutscene.world.CutsceneWorldTrack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CutsceneWorldRecorder {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final ThreadLocal<Long2ObjectMap<BlockState>> OLD_STATES =
        ThreadLocal.withInitial(Long2ObjectOpenHashMap::new);
    private static final int DEFAULT_SEEK_INTERVAL = 100;
    private static final int DEFAULT_BE_THROTTLE = 5;

    private CutsceneWorldRecorder() {
    }

    public static boolean isRecording(ServerPlayer player) {
        return player != null && SESSIONS.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player, String sceneId, int chunkRadius) {
        start(player, sceneId, chunkRadius, null);
    }

    public static void start(ServerPlayer player, String sceneId, int chunkRadius, int centerChunkX, int centerChunkZ) {
        start(player, sceneId, chunkRadius, new ChunkPos(centerChunkX, centerChunkZ));
    }

    private static void start(ServerPlayer player, String sceneId, int chunkRadius, ChunkPos centerOverride) {
        if (player == null || sceneId == null || sceneId.isBlank()) {
            return;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int radius = Math.max(1, chunkRadius);
        ChunkPos center = centerOverride != null ? centerOverride : player.chunkPosition();
        CutsceneWorldRegion region = buildRegion(level, center, radius);
        Session session = new Session(player.getUUID(), sceneId, level, region, center, radius);
        session.startTick = level.getGameTime();
        captureInitialSnapshots(level, session, false);
        if (session.chunkSnapshots.isEmpty()) {
            captureInitialSnapshots(level, session, true);
        }
        captureInitialWeather(level, session);
        SESSIONS.put(player.getUUID(), session);
        Marallyzen.LOGGER.info("Cutscene world recording started: {} (player={})", sceneId, player.getGameProfile().getName());
    }

    public static CutsceneWorldTrack stop(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return null;
        }
        if (!session.dirtyChunks.isEmpty()) {
            captureSeekFrame(session, getRelativeTick(session.level, session));
        }
        CutsceneWorldTrack.Header header = new CutsceneWorldTrack.Header(
            session.level.dimension().location().toString(),
            session.startWorldTime,
            session.region.minX(),
            session.region.minY(),
            session.region.minZ(),
            session.region.maxX(),
            session.region.maxY(),
            session.region.maxZ(),
            20
        );
        CutsceneWorldTrack track = new CutsceneWorldTrack(
            header,
            session.chunkSnapshots,
            session.blockChanges,
            session.blockEntityChanges,
            session.weatherChanges,
            session.particleEvents,
            session.seekFrames
        );
        Marallyzen.LOGGER.info(
            "Cutscene world recording stopped: {} chunks={} blocks={} block_entities={} seek_frames={}",
            session.sceneId,
            session.chunkSnapshots.size(),
            session.blockChanges.size(),
            session.blockEntityChanges.size(),
            session.seekFrames.size()
        );
        return track;
    }

    public static void pause(ServerPlayer player) {
        Session session = player == null ? null : SESSIONS.get(player.getUUID());
        if (session == null || session.paused) {
            return;
        }
        session.paused = true;
        session.pauseStartTick = session.level.getGameTime();
    }

    public static void resume(ServerPlayer player) {
        Session session = player == null ? null : SESSIONS.get(player.getUUID());
        if (session == null || !session.paused) {
            return;
        }
        long pauseDuration = session.level.getGameTime() - session.pauseStartTick;
        session.totalPauseTicks += pauseDuration;
        session.paused = false;
    }

    public static void tick(ServerLevel level) {
        if (level == null || SESSIONS.isEmpty()) {
            return;
        }
        for (Session session : SESSIONS.values()) {
            if (session.level != level) {
                continue;
            }
            if (session.paused) {
                continue;
            }
            long relativeTick = getRelativeTick(level, session);
            if (relativeTick < 0) {
                continue;
            }
            captureWeather(level, session, relativeTick);
            if (relativeTick - session.lastSeekTick >= session.seekInterval && !session.dirtyChunks.isEmpty()) {
                captureSeekFrame(session, relativeTick);
            }
        }
    }

    public static void capturePre(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || SESSIONS.isEmpty()) {
            return;
        }
        if (!hasSessionFor(level)) {
            return;
        }
        Long2ObjectMap<BlockState> map = OLD_STATES.get();
        map.put(pos.asLong(), level.getBlockState(pos));
    }

    public static void capturePost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || SESSIONS.isEmpty()) {
            return;
        }
        if (!hasSessionFor(level)) {
            return;
        }
        Long2ObjectMap<BlockState> map = OLD_STATES.get();
        BlockState oldState = map.remove(pos.asLong());
        if (oldState == null) {
            return;
        }
        BlockState newState = level.getBlockState(pos);
        if (newState == oldState || newState.equals(oldState)) {
            return;
        }
        for (Session session : sessionsFor(level)) {
            if (!session.region.contains(pos)) {
                continue;
            }
            if (session.paused) {
                continue;
            }
            long relativeTick = getRelativeTick(level, session);
            if (relativeTick < 0) {
                continue;
            }
            CompoundTag stateTag = NbtUtils.writeBlockState(newState);
            CompoundTag blockEntityTag = null;
            if (newState.hasBlockEntity() || oldState.hasBlockEntity()) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    blockEntityTag = blockEntity.saveWithFullMetadata(level.registryAccess());
                }
            }
            session.blockChanges.add(new CutsceneWorldTrack.BlockChange(relativeTick, pos.asLong(), stateTag, blockEntityTag));
            session.dirtyChunks.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        }
    }

    public static void onBlockEntityChanged(ServerLevel level, BlockEntity blockEntity) {
        if (level == null || blockEntity == null || SESSIONS.isEmpty()) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        for (Session session : sessionsFor(level)) {
            if (session.paused || !session.region.contains(pos)) {
                continue;
            }
            long relativeTick = getRelativeTick(level, session);
            if (relativeTick < 0) {
                continue;
            }
            long posKey = pos.asLong();
            long lastTick = session.lastBlockEntityTicks.get(posKey);
            if (lastTick > 0 && relativeTick - lastTick < session.blockEntityThrottle) {
                continue;
            }
            session.lastBlockEntityTicks.put(posKey, relativeTick);
            CompoundTag tag = blockEntity.saveWithFullMetadata(level.registryAccess());
            session.blockEntityChanges.add(new CutsceneWorldTrack.BlockEntityChange(relativeTick, posKey, tag));
            session.dirtyChunks.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        }
    }

    public static void onParticle(
        ServerLevel level,
        String typeId,
        double x,
        double y,
        double z,
        double dx,
        double dy,
        double dz,
        int count,
        byte[] optionsData
    ) {
        if (level == null || SESSIONS.isEmpty()) {
            return;
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        for (Session session : sessionsFor(level)) {
            if (session.paused || !session.region.contains(pos)) {
                continue;
            }
            long relativeTick = getRelativeTick(level, session);
            if (relativeTick < 0) {
                continue;
            }
            session.particleEvents.add(new CutsceneWorldTrack.ParticleEvent(
                relativeTick,
                typeId,
                x,
                y,
                z,
                dx,
                dy,
                dz,
                count,
                optionsData == null ? new byte[0] : optionsData
            ));
        }
    }

    private static void captureInitialSnapshots(ServerLevel level, Session session, boolean forceLoad) {
        ChunkPos center = session.centerChunk;
        int radius = session.chunkRadius;
        if (forceLoad) {
            session.chunkSnapshots.clear();
        }
        int nonAirChunks = 0;
        int nonAirSections = 0;
        int totalSections = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = center.x + dx;
                int chunkZ = center.z + dz;
                var access = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, forceLoad);
                if (access instanceof LevelChunk levelChunk) {
                    session.chunkSnapshots.add(captureChunkSnapshot(level, levelChunk));
                    boolean chunkHasNonAir = false;
                    for (LevelChunkSection section : levelChunk.getSections()) {
                        if (section == null) {
                            continue;
                        }
                        totalSections++;
                        if (!section.hasOnlyAir()) {
                            nonAirSections++;
                            chunkHasNonAir = true;
                        }
                    }
                    if (chunkHasNonAir) {
                        nonAirChunks++;
                    }
                }
            }
        }
        session.chunkSnapshots.sort(Comparator.comparingInt(CutsceneWorldTrack.ChunkSnapshot::chunkX)
            .thenComparingInt(CutsceneWorldTrack.ChunkSnapshot::chunkZ));
        Marallyzen.LOGGER.info(
            "Cutscene world snapshot summary: chunks={} nonAirChunks={} nonAirSections={}/{} forceLoad={}",
            session.chunkSnapshots.size(),
            nonAirChunks,
            nonAirSections,
            totalSections,
            forceLoad
        );
    }

    private static void captureInitialWeather(ServerLevel level, Session session) {
        session.startWorldTime = level.getDayTime();
        session.lastWeatherRaining = level.isRaining();
        session.lastWeatherThundering = level.isThundering();
        session.lastRainLevel = level.getRainLevel(1.0f);
        session.lastThunderLevel = level.getThunderLevel(1.0f);
        session.lastWorldTime = level.getDayTime();
        session.weatherChanges.add(new CutsceneWorldTrack.WeatherChange(
            0L,
            session.startWorldTime,
            session.lastWeatherRaining,
            session.lastRainLevel,
            session.lastWeatherThundering,
            session.lastThunderLevel
        ));
    }

    private static void captureWeather(ServerLevel level, Session session, long relativeTick) {
        boolean raining = level.isRaining();
        boolean thundering = level.isThundering();
        float rainLevel = level.getRainLevel(1.0f);
        float thunderLevel = level.getThunderLevel(1.0f);
        long worldTime = level.getDayTime();
        boolean changed = raining != session.lastWeatherRaining
            || thundering != session.lastWeatherThundering
            || Math.abs(rainLevel - session.lastRainLevel) > 0.01f
            || Math.abs(thunderLevel - session.lastThunderLevel) > 0.01f;
        long expectedTime = session.lastWorldTime + 1;
        if (worldTime != expectedTime) {
            changed = true;
        }
        if (changed) {
            session.weatherChanges.add(new CutsceneWorldTrack.WeatherChange(
                relativeTick,
                worldTime,
                raining,
                rainLevel,
                thundering,
                thunderLevel
            ));
            session.lastWeatherRaining = raining;
            session.lastWeatherThundering = thundering;
            session.lastRainLevel = rainLevel;
            session.lastThunderLevel = thunderLevel;
        }
        session.lastWorldTime = worldTime;
    }

    private static void captureSeekFrame(Session session, long relativeTick) {
        if (session.dirtyChunks.isEmpty()) {
            session.lastSeekTick = relativeTick;
            return;
        }
        List<CutsceneWorldTrack.ChunkSnapshot> snapshots = new ArrayList<>();
        for (long chunkKey : session.dirtyChunks) {
            ChunkPos pos = new ChunkPos(chunkKey);
            var access = session.level.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
            if (access instanceof LevelChunk levelChunk) {
                snapshots.add(captureChunkSnapshot(session.level, levelChunk));
            }
        }
        session.seekFrames.add(new CutsceneWorldTrack.SeekFrame(
            relativeTick,
            session.blockChanges.size(),
            session.blockEntityChanges.size(),
            session.weatherChanges.size(),
            session.particleEvents.size(),
            snapshots
        ));
        session.dirtyChunks.clear();
        session.lastSeekTick = relativeTick;
    }

    private static CutsceneWorldTrack.ChunkSnapshot captureChunkSnapshot(ServerLevel level, LevelChunk chunk) {
        List<CutsceneWorldTrack.SectionData> sections = new ArrayList<>();
        LevelChunkSection[] chunkSections = chunk.getSections();
        for (int i = 0; i < chunkSections.length; i++) {
            LevelChunkSection section = chunkSections[i];
            if (section == null) {
                continue;
            }
            net.minecraft.network.RegistryFriendlyByteBuf buffer =
                new net.minecraft.network.RegistryFriendlyByteBuf(
                    io.netty.buffer.Unpooled.buffer(section.getSerializedSize()),
                    level.registryAccess()
                );
            section.write(buffer);
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            sections.add(new CutsceneWorldTrack.SectionData(i, data));
        }
        List<CompoundTag> blockEntities = new ArrayList<>();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity == null || blockEntity.isRemoved()) {
                continue;
            }
            blockEntities.add(blockEntity.saveWithFullMetadata(level.registryAccess()));
        }
        int minSection = level.getMinSection();
        List<CutsceneWorldTrack.LightSectionData> skyLight =
            CutsceneLightAccess.capture(level, chunk.getPos(), chunkSections.length, minSection, LightLayer.SKY);
        List<CutsceneWorldTrack.LightSectionData> blockLight =
            CutsceneLightAccess.capture(level, chunk.getPos(), chunkSections.length, minSection, LightLayer.BLOCK);
        return new CutsceneWorldTrack.ChunkSnapshot(
            chunk.getPos().x,
            chunk.getPos().z,
            sections,
            blockEntities,
            skyLight,
            blockLight
        );
    }

    private static long getRelativeTick(ServerLevel level, Session session) {
        return level.getGameTime() - session.startTick - session.totalPauseTicks;
    }

    private static CutsceneWorldRegion buildRegion(ServerLevel level, ChunkPos center, int radius) {
        int minX = (center.x - radius) << 4;
        int minZ = (center.z - radius) << 4;
        int maxX = ((center.x + radius) << 4) + 15;
        int maxZ = ((center.z + radius) << 4) + 15;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        return new CutsceneWorldRegion(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean hasSessionFor(ServerLevel level) {
        for (Session session : SESSIONS.values()) {
            if (session.level == level) {
                return true;
            }
        }
        return false;
    }

    private static List<Session> sessionsFor(ServerLevel level) {
        List<Session> sessions = new ArrayList<>();
        for (Session session : SESSIONS.values()) {
            if (session.level == level) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    private static final class Session {
        private final UUID playerId;
        private final String sceneId;
        private final ServerLevel level;
        private final CutsceneWorldRegion region;
        private final ChunkPos centerChunk;
        private final int chunkRadius;
        private final List<CutsceneWorldTrack.ChunkSnapshot> chunkSnapshots = new ArrayList<>();
        private final List<CutsceneWorldTrack.BlockChange> blockChanges = new ArrayList<>();
        private final List<CutsceneWorldTrack.BlockEntityChange> blockEntityChanges = new ArrayList<>();
        private final List<CutsceneWorldTrack.WeatherChange> weatherChanges = new ArrayList<>();
        private final List<CutsceneWorldTrack.ParticleEvent> particleEvents = new ArrayList<>();
        private final List<CutsceneWorldTrack.SeekFrame> seekFrames = new ArrayList<>();
        private final LongSet dirtyChunks = new LongOpenHashSet();
        private final Long2LongOpenHashMap lastBlockEntityTicks = new Long2LongOpenHashMap();
        private long startTick;
        private long pauseStartTick;
        private long totalPauseTicks;
        private boolean paused;
        private long startWorldTime;
        private long lastWorldTime;
        private boolean lastWeatherRaining;
        private boolean lastWeatherThundering;
        private float lastRainLevel;
        private float lastThunderLevel;
        private long lastSeekTick;
        private int seekInterval = DEFAULT_SEEK_INTERVAL;
        private int blockEntityThrottle = DEFAULT_BE_THROTTLE;

        private Session(UUID playerId, String sceneId, ServerLevel level, CutsceneWorldRegion region,
                        ChunkPos centerChunk, int chunkRadius) {
            this.playerId = playerId;
            this.sceneId = sceneId;
            this.level = level;
            this.region = region;
            this.centerChunk = centerChunk;
            this.chunkRadius = chunkRadius;
        }
    }
}
