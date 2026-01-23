package neutka.marallys.marallyzen.instance;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.WorldDataConfiguration;
import neutka.marallys.marallyzen.Marallyzen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import net.minecraft.core.Holder;

public class InstanceWorldManager {
    private static final int MAX_REFLECTION_DEPTH = 64;
    private final Map<ResourceKey<Level>, LevelStorageSource.LevelStorageAccess> accessByLevel = new HashMap<>();
    private final Map<String, ResourceKey<Level>> keysByWorldName = new HashMap<>();

    public ServerLevel getOrLoadWorld(MinecraftServer server, String worldName) {
        if (server == null || worldName == null || worldName.isBlank()) {
            return null;
        }
        ServerLevel loaded = findLoadedLevel(server, worldName);
        if (loaded != null) {
            return loaded;
        }
        return loadWorldByName(server, worldName);
    }

    public void preRegisterInstanceWorld(MinecraftServer server, String worldName) {
        if (server == null || worldName == null || worldName.isBlank()) {
            return;
        }
        Path path = resolveWorldPath(server, worldName);
        if (path == null) {
            Marallyzen.LOGGER.warn("InstanceWorldManager: pre-register skipped, world '{}' not found", worldName);
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        ResourceKey<Level> instanceKey = buildInstanceKey(worldName);
        Holder<DimensionType> instanceType = overworld.dimensionTypeRegistration();
        LevelStem stem = new LevelStem(instanceType, overworld.getChunkSource().getGenerator());
        Marallyzen.LOGGER.info("InstanceWorldManager: pre-register stem {} from {}", instanceKey.location(), path);
        ensureLevelStemRegistered(server, instanceKey, stem);
    }

    public ServerLevel loadWorldByName(MinecraftServer server, String worldName) {
        if (server == null || worldName == null || worldName.isBlank()) {
            return null;
        }
        ServerLevel existing = findLoadedLevel(server, worldName);
        if (existing != null) {
            return existing;
        }
        Path worldPath = resolveWorldPath(server, worldName);
        if (worldPath == null) {
            Marallyzen.LOGGER.warn("InstanceWorldManager: world '{}' not found in storage", worldName);
            return null;
        }
        Path baseDir = worldPath.getParent();
        if (baseDir == null) {
            Marallyzen.LOGGER.warn("InstanceWorldManager: invalid base directory for {}", worldPath);
            return null;
        }
        String levelId = worldPath.getFileName().toString();
        LevelStorageSource storageSource = LevelStorageSource.createDefault(baseDir);
        LevelStorageSource.LevelStorageAccess access;
        try {
            access = storageSource.createAccess(levelId);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("InstanceWorldManager: failed to open level access for {}", worldPath, e);
            return null;
        }
        try {
            Dynamic<?> dataTag = access.getDataTag();
            WorldDataConfiguration dataConfig = LevelStorageSource.readDataConfig(dataTag);
            var registryAccess = server.registryAccess();
            Registry<LevelStem> levelStemRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
            LevelDataAndDimensions dataAndDims = LevelStorageSource.getLevelDataAndDimensions(
                    dataTag,
                    dataConfig,
                    levelStemRegistry,
                    registryAccess
            );
            ServerLevel overworld = server.overworld();
            if (overworld == null) {
                access.close();
                return null;
            }
            WorldData worldData = dataAndDims.worldData();
            ServerLevelData levelData = new DerivedLevelData(worldData, worldData.overworldData());
            ResourceKey<Level> instanceKey = buildInstanceKeyFromWorldName(worldName);
            Holder<DimensionType> instanceType = overworld.dimensionTypeRegistration();
            LevelStem stem = new LevelStem(instanceType, overworld.getChunkSource().getGenerator());
            ensureLevelStemRegistered(server, instanceKey, stem);
            ensureWorldDataDimensions(dataAndDims, worldData, instanceKey, stem);
            ensureDimensionStorageLayout(worldPath, instanceKey);
            long seed = worldData.worldGenOptions().seed();
            RandomSequences randomSequences = new RandomSequences(seed);
            Executor executor = server;
            ServerLevel level = new ServerLevel(
                    server,
                    executor,
                    access,
                    levelData,
                    instanceKey,
                    stem,
                    LoggerChunkProgressListener.create(0),
                    false,
                    seed,
                    java.util.List.of(),
                    true,
                    randomSequences
            );
            registerLevel(server, level);
            accessByLevel.put(level.dimension(), access);
            keysByWorldName.put(worldName, level.dimension());
            Marallyzen.LOGGER.info("InstanceWorldManager: loaded instance world '{}' from {}", worldName, worldPath);
            return level;
        } catch (Exception e) {
            try {
                access.close();
            } catch (Exception ignored) {
            }
            Marallyzen.LOGGER.error("InstanceWorldManager: failed to load world '{}'", worldName, e);
            return null;
        }
    }

    public boolean unloadWorld(MinecraftServer server, String worldName) {
        if (server == null || worldName == null || worldName.isBlank()) {
            return false;
        }
        ResourceKey<Level> key = keysByWorldName.get(worldName);
        if (key == null) {
            return false;
        }
        ServerLevel level = server.getLevel(key);
        if (level == null) {
            keysByWorldName.remove(worldName);
            return false;
        }
        try {
            level.save(null, true, false);
        } catch (Exception ignored) {
        }
        try {
            level.getChunkSource().close();
        } catch (Exception ignored) {
        }
        try {
            unregisterLevel(server, key);
        } catch (IllegalAccessException e) {
            Marallyzen.LOGGER.warn("InstanceWorldManager: failed to unregister level {}", key.location(), e);
        }
        LevelStorageSource.LevelStorageAccess access = accessByLevel.remove(key);
        if (access != null) {
            try {
                access.close();
            } catch (Exception ignored) {
            }
        }
        keysByWorldName.remove(worldName);
        Marallyzen.LOGGER.info("InstanceWorldManager: unloaded instance world '{}'", worldName);
        return true;
    }

    private ServerLevel findLoadedLevel(MinecraftServer server, String worldName) {
        ResourceKey<Level> instanceKey = buildInstanceKeyFromWorldName(worldName);
        if (instanceKey != null) {
            ServerLevel instanceLevel = server.getLevel(instanceKey);
            if (instanceLevel != null) {
                return instanceLevel;
            }
        }
        ResourceKey<Level> key = resolveDimensionKey(server, worldName);
        if (key != null) {
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                return level;
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation id = level.dimension().location();
            if (id != null && (id.toString().equals(worldName) || id.getPath().equals(worldName))) {
                return level;
            }
        }
        return null;
    }

    public ResourceKey<Level> resolveDimensionKey(MinecraftServer server, String worldName) {
        if (server == null || worldName == null || worldName.isBlank()) {
            return null;
        }
        if (!worldName.contains(":")) {
            return null;
        }
        ResourceLocation id;
        try {
            id = ResourceLocation.parse(worldName);
        } catch (Exception ignored) {
            id = null;
        }
        if (id == null) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        return server.getLevel(key) != null ? key : null;
    }

    public Path resolveWorldPath(MinecraftServer server, String worldName) {
        if (server == null || worldName == null || worldName.isBlank()) {
            return null;
        }
        if (looksLikePath(worldName)) {
            Path path = Path.of(worldName);
            return Files.exists(path) ? path : null;
        }
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path localWorld = root.resolve("worlds").resolve(worldName);
        if (Files.exists(localWorld)) {
            return localWorld;
        }
        Path serverDim = root.resolve("DIM-1").resolve(worldName);
        if (Files.exists(serverDim)) {
            return serverDim;
        }
        Path custom = root.resolve(worldName);
        if (Files.exists(custom)) {
            return custom;
        }
        return null;
    }

    private boolean looksLikePath(String value) {
        return value.contains("/") || value.contains("\\") || value.matches("^[A-Za-z]:.*");
    }

    private ResourceKey<Level> buildInstanceKey(String worldName) {
        return buildInstanceKeyFromWorldName(worldName);
    }

    public static ResourceKey<Level> buildInstanceKeyFromWorldName(String worldName) {
        String safe = worldName == null ? "" : worldName.toLowerCase().replaceAll("[^a-z0-9_./-]", "_");
        String path = "instance/" + safe;
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, path));
    }

    private void ensureDimensionStorageLayout(Path worldRoot, ResourceKey<Level> levelKey) {
        if (worldRoot == null || levelKey == null) {
            return;
        }
        Path srcRegion = worldRoot.resolve("region");
        if (!Files.exists(srcRegion)) {
            return;
        }
        ResourceLocation id = levelKey.location();
        Path dimRoot = worldRoot.resolve("dimensions").resolve(id.getNamespace()).resolve(id.getPath());
        copyDirIfMissingOrEmpty(srcRegion, dimRoot.resolve("region"));
        copyDirIfMissingOrEmpty(worldRoot.resolve("poi"), dimRoot.resolve("poi"));
        copyDirIfMissingOrEmpty(worldRoot.resolve("entities"), dimRoot.resolve("entities"));
    }

    private void copyDirIfMissingOrEmpty(Path src, Path dest) {
        if (src == null || dest == null || !Files.exists(src)) {
            return;
        }
        try {
            if (Files.exists(dest) && hasNonEmptyFiles(dest)) {
                return;
            }
            Files.createDirectories(dest);
            try (var stream = Files.walk(src)) {
                stream.forEach(path -> {
                    try {
                        Path rel = src.relativize(path);
                        Path out = dest.resolve(rel);
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(out);
                        } else {
                            Files.createDirectories(out.getParent());
                            Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception e) {
                        Marallyzen.LOGGER.warn("InstanceWorldManager: failed copying {} to {}", path, dest, e);
                    }
                });
            }
            Marallyzen.LOGGER.info("InstanceWorldManager: copied {} -> {}", src, dest);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("InstanceWorldManager: failed to prepare dimension storage {}", dest, e);
        }
    }

    private boolean hasNonEmptyFiles(Path dest) {
        try (var stream = Files.walk(dest)) {
            return stream.anyMatch(path -> {
                try {
                    return Files.isRegularFile(path) && Files.size(path) > 0;
                } catch (Exception ignored) {
                    return false;
                }
            });
        } catch (Exception ignored) {
            return false;
        }
    }

    private void ensureLevelStemRegistered(MinecraftServer server, ResourceKey<Level> levelKey, LevelStem stem) {
        if (server == null || levelKey == null || stem == null) {
            return;
        }
        ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, levelKey.location());
        try {
            var registryAccess = server.registryAccess();
            Registry<LevelStem> stemRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
            ensureStemInRegistry(stemRegistry, stemKey, stem);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("InstanceWorldManager: failed to register level stem {}", levelKey.location(), e);
        }
    }

    private void ensureStemInRegistry(Registry<LevelStem> registry, ResourceKey<LevelStem> stemKey, LevelStem stem) {
        if (registry == null || stemKey == null || stem == null) {
            return;
        }
        if (registry.get(stemKey) != null) {
            return;
        }
        try {
            Registry.register(registry, stemKey.location(), stem);
            Marallyzen.LOGGER.info("InstanceWorldManager: registered stem {}", stemKey.location());
            return;
        } catch (IllegalStateException frozen) {
            if (!isRegistryFrozen(registry)) {
                throw frozen;
            }
            if (!setRegistryFrozen(registry, false)) {
                throw frozen;
            }
            try {
                Registry.register(registry, stemKey.location(), stem);
                Marallyzen.LOGGER.info("InstanceWorldManager: registered stem {}", stemKey.location());
            } finally {
                setRegistryFrozen(registry, true);
            }
        }
    }

    private void ensureWorldDataDimensions(Object dataAndDims, WorldData worldData, ResourceKey<Level> levelKey, LevelStem stem) {
        if (levelKey == null || stem == null) {
            return;
        }
        ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, levelKey.location());
        boolean injected = false;
        injected |= tryInjectStemIntoHost(dataAndDims, stemKey, stem);
        injected |= tryInjectStemIntoHost(worldData, stemKey, stem);
        if (injected) {
            Marallyzen.LOGGER.info("InstanceWorldManager: injected world dimensions entry {}", stemKey.location());
        } else {
            Marallyzen.LOGGER.warn("InstanceWorldManager: failed to inject world dimensions entry {}", stemKey.location());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean tryInjectStemIntoHost(Object host, ResourceKey<LevelStem> stemKey, LevelStem stem) {
        return tryInjectStemIntoHost(host, stemKey, stem, java.util.Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    @SuppressWarnings("unchecked")
    private boolean tryInjectStemIntoHost(Object host, ResourceKey<LevelStem> stemKey, LevelStem stem, Set<Object> visited, int depth) {
        if (host == null || stemKey == null || stem == null) {
            return false;
        }
        if (depth > MAX_REFLECTION_DEPTH || visited.contains(host)) {
            return false;
        }
        visited.add(host);
        if (host instanceof Registry<?> registry && registry.get(stemKey.location()) == null) {
            try {
                ensureStemInRegistry((Registry<LevelStem>) registry, stemKey, stem);
                return registry.get(stemKey.location()) != null;
            } catch (Exception ignored) {
                return false;
            }
        }
        if (host instanceof Map<?, ?> map) {
            try {
                ((Map<ResourceKey<LevelStem>, LevelStem>) map).put(stemKey, stem);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
        Object dimensions = tryGetDimensionsObject(host);
        if (dimensions != null && dimensions != host) {
            if (tryInjectStemIntoHost(dimensions, stemKey, stem, visited, depth + 1)) {
                return true;
            }
        }
        for (Field field : host.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(host);
                if (value == null) {
                    continue;
                }
                if (value == host) {
                    continue;
                }
                if (tryInjectStemIntoHost(value, stemKey, stem, visited, depth + 1)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private Object tryGetDimensionsObject(Object host) {
        if (host == null) {
            return null;
        }
        Object direct = tryInvokeNoArgs(host, "dimensions");
        if (direct != null) {
            return direct;
        }
        Object alt = tryInvokeNoArgs(host, "worldDimensions");
        if (alt != null) {
            return alt;
        }
        Object worldGen = tryInvokeNoArgs(host, "worldGenSettings");
        if (worldGen != null) {
            Object dims = tryInvokeNoArgs(worldGen, "dimensions");
            if (dims != null) {
                return dims;
            }
        }
        return null;
    }

    private Object tryInvokeNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isRegistryFrozen(Registry<?> registry) {
        if (registry == null) {
            return false;
        }
        try {
            Method method = registry.getClass().getMethod("isFrozen");
            Object result = method.invoke(registry);
            if (result instanceof Boolean frozen) {
                return frozen;
            }
        } catch (Exception ignored) {
        }
        try {
            Field field = findBooleanField(registry.getClass(), "frozen");
            if (field != null) {
                field.setAccessible(true);
                return field.getBoolean(registry);
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private boolean setRegistryFrozen(Registry<?> registry, boolean frozen) {
        if (registry == null) {
            return false;
        }
        try {
            Field field = findBooleanField(registry.getClass(), "frozen");
            if (field == null) {
                return false;
            }
            field.setAccessible(true);
            field.setBoolean(registry, frozen);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Field findBooleanField(Class<?> cls, String name) {
        for (Class<?> current = cls; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                if (field.getType() == boolean.class) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void registerLevel(MinecraftServer server, ServerLevel level) throws IllegalAccessException {
        Map<ResourceKey<Level>, ServerLevel> levels = getLevelsMap(server);
        levels.put(level.dimension(), level);
    }

    @SuppressWarnings("unchecked")
    private void unregisterLevel(MinecraftServer server, ResourceKey<Level> key) throws IllegalAccessException {
        Map<ResourceKey<Level>, ServerLevel> levels = getLevelsMap(server);
        levels.remove(key);
    }

    @SuppressWarnings("unchecked")
    private Map<ResourceKey<Level>, ServerLevel> getLevelsMap(MinecraftServer server) throws IllegalAccessException {
        for (Class<?> cls = server.getClass(); cls != null; cls = cls.getSuperclass()) {
            try {
                var field = cls.getDeclaredField("levels");
                field.setAccessible(true);
                Object value = field.get(server);
                if (value instanceof Map<?, ?> map) {
                    return (Map<ResourceKey<Level>, ServerLevel>) map;
                }
            } catch (NoSuchFieldException ignored) {
            }
            try {
                var field = cls.getDeclaredField("P");
                field.setAccessible(true);
                Object value = field.get(server);
                if (value instanceof Map<?, ?> map) {
                    return (Map<ResourceKey<Level>, ServerLevel>) map;
                }
            } catch (NoSuchFieldException ignored) {
            }
            for (var field : cls.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(server);
                if (!(value instanceof Map<?, ?> map)) {
                    continue;
                }
                if (map.containsValue(server.overworld())) {
                    return (Map<ResourceKey<Level>, ServerLevel>) map;
                }
                boolean matchesTypes = false;
                for (var entry : map.entrySet()) {
                    if (entry.getKey() instanceof ResourceKey<?> && entry.getValue() instanceof ServerLevel) {
                        matchesTypes = true;
                        break;
                    }
                }
                if (matchesTypes) {
                    return (Map<ResourceKey<Level>, ServerLevel>) map;
                }
            }
        }
        throw new IllegalStateException("InstanceWorldManager: unable to locate levels map");
    }
}
