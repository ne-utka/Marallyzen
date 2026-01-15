package neutka.marallys.marallyzen.replay.playback;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import neutka.marallys.marallyzen.Marallyzen;

public class ReplayChunkCache extends ClientChunkCache {
    private static boolean storageResolved;
    private static Field storageField;
    private static Method storageReplaceMethod;
    private static Field storageChunksField;
    private static Method storageIndexMethod;
    private static boolean storageLogged;
    private static boolean viewCenterResolved;
    private static Method viewCenterMethod;
    private static Field viewCenterXField;
    private static Field viewCenterZField;
    private static boolean viewCenterLogged;
    private final Long2ObjectMap<LevelChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final LevelChunk emptyChunk;

    public ReplayChunkCache(ClientLevel level, int viewDistance) {
        super(level, viewDistance);
        this.emptyChunk = new EmptyLevelChunk(
            level,
            new ChunkPos(0, 0),
            level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS)
        );
    }

    @Override
    public LevelChunk getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean create) {
        LevelChunk chunk = chunks.get(ChunkPos.asLong(chunkX, chunkZ));
        if (chunk != null) {
            return chunk;
        }
        return create ? emptyChunk : null;
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return chunks.containsKey(ChunkPos.asLong(chunkX, chunkZ));
    }

    @Override
    public int getLoadedChunksCount() {
        return chunks.size();
    }

    @Override
    public String gatherStats() {
        return "Chunks[R] " + chunks.size();
    }

    public void putChunk(LevelChunk chunk) {
        if (chunk == null) {
            return;
        }
        long key = ChunkPos.asLong(chunk.getPos().x, chunk.getPos().z);
        LevelChunk previous = chunks.put(key, chunk);
        if (previous != null) {
            previous.clearAllBlockEntities();
            previous.setLoaded(false);
        }
        chunk.setLoaded(true);
        injectIntoStorage(chunk);
    }

    public void clear() {
        for (LevelChunk chunk : chunks.values()) {
            if (chunk != null) {
                chunk.clearAllBlockEntities();
                chunk.setLoaded(false);
            }
        }
        chunks.clear();
    }

    public void forceUpdateViewCenter(int chunkX, int chunkZ) {
        resolveViewCenterAccess();
        boolean updated = false;
        if (viewCenterMethod != null) {
            try {
                viewCenterMethod.invoke(this, chunkX, chunkZ);
                updated = true;
            } catch (Exception ignored) {
            }
        }
        if (!updated && viewCenterXField != null && viewCenterZField != null) {
            try {
                viewCenterXField.setInt(this, chunkX);
                viewCenterZField.setInt(this, chunkZ);
                updated = true;
            } catch (Exception ignored) {
            }
        }
        if (updated) {
            logViewCenter("chunk", chunkX, chunkZ);
        }
    }

    private void injectIntoStorage(LevelChunk chunk) {
        if (chunk == null) {
            return;
        }
        resolveStorageAccess();
        if (storageField == null) {
            return;
        }
        try {
            Object storage = storageField.get(this);
            if (storage == null) {
                return;
            }
            if (storageReplaceMethod != null) {
                storageReplaceMethod.invoke(storage, chunk.getPos().x, chunk.getPos().z, chunk);
                logStorageInjection("replace", storage.getClass(), storageReplaceMethod);
                return;
            }
            if (storageChunksField != null && storageIndexMethod != null) {
                Object array = storageChunksField.get(storage);
                int index = (int) storageIndexMethod.invoke(storage, chunk.getPos().x, chunk.getPos().z);
                if (array instanceof AtomicReferenceArray) {
                    @SuppressWarnings("rawtypes")
                    AtomicReferenceArray raw = (AtomicReferenceArray) array;
                    raw.set(index, chunk);
                    logStorageInjection("atomic", storage.getClass(), storageIndexMethod);
                    return;
                }
                if (array instanceof Object[] objects) {
                    objects[index] = chunk;
                    logStorageInjection("array", storage.getClass(), storageIndexMethod);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void resolveStorageAccess() {
        if (storageResolved) {
            return;
        }
        storageResolved = true;
        for (Field field : ClientChunkCache.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (type != null && type.getEnclosingClass() == ClientChunkCache.class) {
                storageField = field;
                storageField.setAccessible(true);
                resolveStorageHelpers(type);
                return;
            }
        }
    }

    private void resolveStorageHelpers(Class<?> storageClass) {
        for (Method method : storageClass.getDeclaredMethods()) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 3
                && params[0] == int.class
                && params[1] == int.class
                && LevelChunk.class.isAssignableFrom(params[2])) {
                storageReplaceMethod = method;
                storageReplaceMethod.setAccessible(true);
            }
            if (params.length == 2 && params[0] == int.class && params[1] == int.class
                && method.getReturnType() == int.class) {
                storageIndexMethod = method;
                storageIndexMethod.setAccessible(true);
            }
        }
        for (Field field : storageClass.getDeclaredFields()) {
            if (AtomicReferenceArray.class.isAssignableFrom(field.getType())) {
                storageChunksField = field;
                storageChunksField.setAccessible(true);
                return;
            }
            if (field.getType().isArray()) {
                storageChunksField = field;
                storageChunksField.setAccessible(true);
            }
        }
    }

    private void resolveViewCenterAccess() {
        if (viewCenterResolved) {
            return;
        }
        viewCenterResolved = true;
        try {
            viewCenterMethod = ClientChunkCache.class.getDeclaredMethod("updateViewCenter", int.class, int.class);
            viewCenterMethod.setAccessible(true);
        } catch (Exception ignored) {
            viewCenterMethod = null;
        }
        if (viewCenterMethod == null) {
            for (Method method : ClientChunkCache.class.getDeclaredMethods()) {
                if (method.getReturnType() != Void.TYPE || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] != int.class || params[1] != int.class) {
                    continue;
                }
                String name = method.getName().toLowerCase();
                if (name.contains("view") || name.contains("center")) {
                    viewCenterMethod = method;
                    viewCenterMethod.setAccessible(true);
                    break;
                }
            }
        }
        for (Field field : ClientChunkCache.class.getDeclaredFields()) {
            if (field.getType() != int.class) {
                continue;
            }
            String name = field.getName().toLowerCase();
            if (viewCenterXField == null && (name.contains("viewcenterx") || name.contains("centerx"))) {
                field.setAccessible(true);
                viewCenterXField = field;
                continue;
            }
            if (viewCenterZField == null && (name.contains("viewcenterz") || name.contains("centerz"))) {
                field.setAccessible(true);
                viewCenterZField = field;
            }
        }
    }

    private void logViewCenter(String mode, int chunkX, int chunkZ) {
        if (viewCenterLogged) {
            return;
        }
        viewCenterLogged = true;
        String methodName = viewCenterMethod != null ? viewCenterMethod.getName() : "none";
        Marallyzen.LOGGER.info(
            "ReplayChunkCache view center: mode={} chunk=({}, {}) method={} fields=({},{})",
            mode,
            chunkX,
            chunkZ,
            methodName,
            viewCenterXField != null ? viewCenterXField.getName() : "null",
            viewCenterZField != null ? viewCenterZField.getName() : "null"
        );
    }

    private void logStorageInjection(String mode, Class<?> storageClass, Method method) {
        if (storageLogged) {
            return;
        }
        storageLogged = true;
        String methodName = method != null ? method.getName() : "unknown";
        Marallyzen.LOGGER.info(
            "ReplayChunkCache storage injection: mode={} storageClass={} method={}",
            mode,
            storageClass != null ? storageClass.getName() : "null",
            methodName
        );
    }

    public DebugSummary buildDebugSummary() {
        int totalChunks = chunks.size();
        int nonAirChunks = 0;
        int nonAirSections = 0;
        int totalSections = 0;
        for (LevelChunk chunk : chunks.values()) {
            if (chunk == null) {
                continue;
            }
            boolean chunkHasNonAir = false;
            for (LevelChunkSection section : chunk.getSections()) {
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
        return new DebugSummary(totalChunks, nonAirChunks, nonAirSections, totalSections);
    }

    public record DebugSummary(int chunks, int nonAirChunks, int nonAirSections, int totalSections) {}
}
