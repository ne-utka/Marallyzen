package neutka.marallys.marallyzen.denizen.storage;

import com.denizenscript.denizencore.flags.SavableMapFlagTracker;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarallyzenFlagStore {
    private MarallyzenFlagStore() {
    }

    private static final Map<UUID, SavableMapFlagTracker> playerTrackers = new ConcurrentHashMap<>();
    private static final Map<UUID, SavableMapFlagTracker> entityTrackers = new ConcurrentHashMap<>();

    public static SavableMapFlagTracker getPlayerTracker(UUID uuid) {
        return playerTrackers.computeIfAbsent(uuid, MarallyzenFlagStore::loadPlayerTracker);
    }

    public static SavableMapFlagTracker getEntityTracker(UUID uuid) {
        return entityTrackers.computeIfAbsent(uuid, MarallyzenFlagStore::loadEntityTracker);
    }

    public static void saveAll() {
        for (Map.Entry<UUID, SavableMapFlagTracker> entry : playerTrackers.entrySet()) {
            saveTracker(getPlayerPath(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<UUID, SavableMapFlagTracker> entry : entityTrackers.entrySet()) {
            saveTracker(getEntityPath(entry.getKey()), entry.getValue());
        }
    }

    private static SavableMapFlagTracker loadPlayerTracker(UUID uuid) {
        return SavableMapFlagTracker.loadFlagFile(getPlayerPath(uuid).getPath(), true);
    }

    private static SavableMapFlagTracker loadEntityTracker(UUID uuid) {
        return SavableMapFlagTracker.loadFlagFile(getEntityPath(uuid).getPath(), true);
    }

    private static void saveTracker(File basePath, SavableMapFlagTracker tracker) {
        try {
            tracker.saveToFile(basePath.getPath());
        }
        catch (Exception ex) {
            Marallyzen.LOGGER.warn("Failed to save flags: {}", basePath.getPath(), ex);
        }
    }

    private static File getPlayerPath(UUID uuid) {
        File dir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("flags").resolve("players").toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            Marallyzen.LOGGER.warn("Failed to create player flags dir: {}", dir.getAbsolutePath());
        }
        return new File(dir, uuid.toString());
    }

    private static File getEntityPath(UUID uuid) {
        File dir = FMLPaths.CONFIGDIR.get().resolve(Marallyzen.MODID).resolve("flags").resolve("entities").toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            Marallyzen.LOGGER.warn("Failed to create entity flags dir: {}", dir.getAbsolutePath());
        }
        return new File(dir, uuid.toString());
    }
}
