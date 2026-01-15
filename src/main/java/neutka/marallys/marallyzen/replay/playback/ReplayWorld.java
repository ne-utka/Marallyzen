package neutka.marallys.marallyzen.replay.playback;

import io.netty.buffer.Unpooled;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.level.lighting.LevelLightEngine;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.replay.ReplayChunkSectionData;
import neutka.marallys.marallyzen.replay.ReplayChunkSnapshot;
import neutka.marallys.marallyzen.replay.ReplayHeader;

public class ReplayWorld extends ClientLevel {
    private static boolean chunkStatusResolved;
    private static java.lang.reflect.Method chunkSetStatusMethod;
    private static java.lang.reflect.Field chunkStatusField;
    private static boolean heightmapResolved;
    private static java.lang.reflect.Method heightmapPrimeMethod;
    private static java.lang.reflect.Method chunkHeightmapRecalcMethod;
    private static boolean lightEngineResolved;
    private static java.lang.reflect.Method lightEnableMethod;
    private static boolean lightEnableMethodUsesLong;

    private final ClientLevel sourceLevel;
    private final ReplayChunkCache replayChunkCache;

    private ReplayWorld(
        ClientPacketListener connection,
        ClientLevelData levelData,
        ResourceKey<Level> dimension,
        Holder<DimensionType> dimensionType,
        int viewDistance,
        int simulationDistance,
        Supplier<ProfilerFiller> profiler,
        LevelRenderer renderer,
        boolean debug,
        long seed,
        ClientLevel sourceLevel
    ) {
        super(connection, levelData, dimension, dimensionType, viewDistance, simulationDistance, profiler, renderer, debug, seed);
        this.sourceLevel = sourceLevel;
        this.replayChunkCache = new ReplayChunkCache(this, viewDistance);
    }

    public static ReplayWorld create(ClientLevel baseLevel, ReplayHeader header) {
        if (baseLevel == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.levelRenderer == null) {
            return null;
        }
        LevelRenderer renderer = mc.levelRenderer;
        ResourceKey<Level> dimension = baseLevel.dimension();
        Holder<DimensionType> dimensionType = baseLevel.dimensionTypeRegistration();
        int viewDistance = mc.options != null ? mc.options.renderDistance().get() : 8;
        int simulationDistance = mc.options != null ? mc.options.simulationDistance().get() : 8;
        long seed = extractSeed(baseLevel);
        boolean debug = baseLevel.isDebug();
        boolean flat = false;
        Difficulty difficulty = baseLevel.getDifficulty();

        ClientLevel.ClientLevelData levelData =
            new ClientLevel.ClientLevelData(difficulty, baseLevel.getLevelData().isHardcore(), flat);
        ReplayWorld world = new ReplayWorld(
            connection,
            levelData,
            dimension,
            dimensionType,
            viewDistance,
            simulationDistance,
            mc::getProfiler,
            renderer,
            debug,
            seed,
            baseLevel
        );
        world.replaceChunkSource();

        if (header != null) {
            world.setGameTime(header.durationTicks());
            world.setDayTime(header.durationTicks());
        }
        world.setDefaultSpawnPos(BlockPos.ZERO, 0.0f);
        world.setRainLevel(0.0f);
        world.setThunderLevel(0.0f);
        return world;
    }

    public ClientLevel getSourceLevel() {
        return sourceLevel;
    }

    @Override
    public ReplayChunkCache getChunkSource() {
        return replayChunkCache;
    }

    private void replaceChunkSource() {
        boolean replaced = false;
        for (java.lang.reflect.Field field : ClientLevel.class.getDeclaredFields()) {
            if (ClientChunkCache.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    field.set(this, replayChunkCache);
                    replaced = true;
                    break;
                } catch (Exception ignored) {
                }
            }
        }
        if (!replaced) {
            Marallyzen.LOGGER.warn("ReplayWorld: failed to replace ClientLevel chunkSource");
        } else {
            Marallyzen.LOGGER.info("ReplayWorld: replaced ClientLevel chunkSource");
        }
    }

    public ReplayChunkCache.DebugSummary buildDebugSummary() {
        return replayChunkCache.buildDebugSummary();
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return replayChunkCache.hasChunk(chunkX, chunkZ);
    }

    @Override
    public void tick(BooleanSupplier hasTimeLeft) {
        // ReplayWorld is read-only: no ticking.
    }

    @Override
    public void tickEntities() {
        // ReplayWorld does not tick entities.
    }

    @Override
    public void tickNonPassenger(net.minecraft.world.entity.Entity entity) {
        // ReplayWorld does not tick entities.
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
        return false;
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        // no-op
    }

    @Override
    public void addParticle(net.minecraft.core.particles.ParticleOptions particle, boolean force,
                             double x, double y, double z, double dx, double dy, double dz) {
        // no-op
    }

    @Override
    public void addParticle(net.minecraft.core.particles.ParticleOptions particle, double x, double y, double z,
                             double dx, double dy, double dz) {
        // no-op
    }

    public void applyWeather(long worldTime, boolean raining, float rainLevel, boolean thundering, float thunderLevel) {
        this.setGameTime(worldTime);
        this.setDayTime(worldTime);
        this.setRainLevel(rainLevel);
        this.setThunderLevel(thunderLevel);
        if (this.getLevelData() instanceof net.minecraft.world.level.storage.WritableLevelData writable) {
            writable.setRaining(raining);
        }
    }

    public void applyChunkSnapshot(ReplayChunkSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        LevelChunk chunk = buildChunkFromSnapshot(snapshot);
        markChunkReady(chunk);
        replayChunkCache.putChunk(chunk);
        onChunkLoaded(chunk.getPos());
        int minSection = getMinSection();
        for (int i = 0; i < chunk.getSections().length; i++) {
            setSectionDirtyWithNeighbors(chunk.getPos().x, minSection + i, chunk.getPos().z);
        }
    }

    public void clearChunks() {
        replayChunkCache.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    private static long extractSeed(Level level) {
        if (level == null) {
            return 0L;
        }
        if (level.getLevelData() instanceof WorldData data && data.worldGenOptions() != null) {
            return data.worldGenOptions().seed();
        }
        return 0L;
    }

    private LevelChunk buildChunkFromSnapshot(ReplayChunkSnapshot snapshot) {
        ChunkPos pos = new ChunkPos(snapshot.chunkX(), snapshot.chunkZ());
        LevelChunkSection[] sections = new LevelChunkSection[getSectionsCount()];
        var biomeRegistry = registryAccess().registryOrThrow(Registries.BIOME);
        if (snapshot.sections() != null) {
            for (ReplayChunkSectionData sectionData : snapshot.sections()) {
                if (sectionData == null) {
                    continue;
                }
                int index = sectionData.sectionIndex();
                if (index < 0 || index >= sections.length) {
                    continue;
                }
                LevelChunkSection section = new LevelChunkSection(biomeRegistry);
                RegistryFriendlyByteBuf buffer =
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(sectionData.data()), registryAccess());
                section.read(buffer);
                sections[index] = section;
            }
        }
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) {
                sections[i] = new LevelChunkSection(biomeRegistry);
            }
        }
        LevelChunk chunk = new LevelChunk(
            this,
            pos,
            UpgradeData.EMPTY,
            new LevelChunkTicks<>(),
            new LevelChunkTicks<>(),
            0L,
            sections,
            null,
            null
        );
        if (snapshot.blockEntities() != null) {
            for (CompoundTag tag : snapshot.blockEntities()) {
                if (tag == null) {
                    continue;
                }
                BlockPos blockPos = BlockEntity.getPosFromTag(tag);
                BlockState state = chunk.getBlockState(blockPos);
                BlockEntity blockEntity = BlockEntity.loadStatic(blockPos, state, tag, registryAccess());
                if (blockEntity != null) {
                    chunk.setBlockEntity(blockEntity);
                }
            }
        }
        chunk.setLightCorrect(true);
        chunk.initializeLightSources();
        return chunk;
    }

    private void markChunkReady(LevelChunk chunk) {
        if (chunk == null) {
            return;
        }
        setChunkStatus(chunk);
        recalcHeightmaps(chunk);
        enableChunkLight(chunk.getPos());
    }

    private void setChunkStatus(LevelChunk chunk) {
        resolveChunkStatusAccess();
        if (chunkSetStatusMethod != null) {
            try {
                chunkSetStatusMethod.invoke(chunk, net.minecraft.world.level.chunk.status.ChunkStatus.FULL);
                return;
            } catch (Exception ignored) {
            }
        }
        if (chunkStatusField != null) {
            try {
                chunkStatusField.set(chunk, net.minecraft.world.level.chunk.status.ChunkStatus.FULL);
            } catch (Exception ignored) {
            }
        }
    }

    private void recalcHeightmaps(LevelChunk chunk) {
        resolveHeightmapAccess();
        if (heightmapPrimeMethod != null) {
            try {
                java.util.EnumSet<Heightmap.Types> types = java.util.EnumSet.of(
                    Heightmap.Types.MOTION_BLOCKING,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Heightmap.Types.OCEAN_FLOOR,
                    Heightmap.Types.WORLD_SURFACE
                );
                heightmapPrimeMethod.invoke(null, chunk, types);
                return;
            } catch (Exception ignored) {
            }
        }
        if (chunkHeightmapRecalcMethod != null) {
            try {
                chunkHeightmapRecalcMethod.invoke(chunk);
            } catch (Exception ignored) {
            }
        }
    }

    private void enableChunkLight(ChunkPos pos) {
        LevelLightEngine engine = getChunkSource().getLightEngine();
        if (engine == null || pos == null) {
            return;
        }
        resolveLightEngineAccess(engine.getClass());
        if (lightEnableMethod == null) {
            return;
        }
        try {
            if (lightEnableMethodUsesLong) {
                lightEnableMethod.invoke(engine, pos.toLong(), true);
            } else {
                lightEnableMethod.invoke(engine, pos, true);
            }
        } catch (Exception ignored) {
        }
    }

    private static void resolveChunkStatusAccess() {
        if (chunkStatusResolved) {
            return;
        }
        chunkStatusResolved = true;
        try {
            chunkSetStatusMethod = LevelChunk.class.getDeclaredMethod(
                "setStatus",
                net.minecraft.world.level.chunk.status.ChunkStatus.class
            );
            chunkSetStatusMethod.setAccessible(true);
        } catch (Exception ignored) {
            chunkSetStatusMethod = null;
        }
        if (chunkSetStatusMethod == null) {
            for (java.lang.reflect.Field field : LevelChunk.class.getDeclaredFields()) {
                if (field.getType() == net.minecraft.world.level.chunk.status.ChunkStatus.class) {
                    field.setAccessible(true);
                    chunkStatusField = field;
                    break;
                }
            }
        }
    }

    private static void resolveHeightmapAccess() {
        if (heightmapResolved) {
            return;
        }
        heightmapResolved = true;
        try {
            heightmapPrimeMethod = Heightmap.class.getDeclaredMethod(
                "primeHeightmaps",
                net.minecraft.world.level.chunk.ChunkAccess.class,
                java.util.Set.class
            );
            heightmapPrimeMethod.setAccessible(true);
        } catch (Exception ignored) {
            heightmapPrimeMethod = null;
        }
        if (heightmapPrimeMethod == null) {
            try {
                chunkHeightmapRecalcMethod = LevelChunk.class.getDeclaredMethod("recalculateHeightmaps");
                chunkHeightmapRecalcMethod.setAccessible(true);
            } catch (Exception ignored) {
                chunkHeightmapRecalcMethod = null;
            }
        }
    }

    private static void resolveLightEngineAccess(Class<?> engineClass) {
        if (lightEngineResolved) {
            return;
        }
        lightEngineResolved = true;
        for (java.lang.reflect.Method method : engineClass.getDeclaredMethods()) {
            if (!method.getName().contains("setLightEnabled") || method.getParameterCount() != 2) {
                continue;
            }
            Class<?> first = method.getParameterTypes()[0];
            Class<?> second = method.getParameterTypes()[1];
            if (second != boolean.class) {
                continue;
            }
            if (first == ChunkPos.class || first == long.class) {
                method.setAccessible(true);
                lightEnableMethod = method;
                lightEnableMethodUsesLong = first == long.class;
                break;
            }
        }
    }
}
