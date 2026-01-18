package neutka.marallys.marallyzen.client;

import net.minecraft.core.BlockPos;
import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.radio.RadioTrackRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientRadioManager {
    private static final Map<BlockPos, Integer> stationByRadio = new HashMap<>();
    private static String selectedStationName;
    private static boolean loaded;

    private ClientRadioManager() {}

    public static boolean isPlaying(BlockPos pos) {
        return pos != null && stationByRadio.containsKey(pos);
    }

    public static boolean toggle(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        if (stationByRadio.containsKey(pos)) {
            stationByRadio.remove(pos);
            return false;
        }
        int index = resolveSelectedStationIndex();
        stationByRadio.put(pos, index);
        return true;
    }

    public static void stop(BlockPos pos) {
        if (pos != null) {
            stationByRadio.remove(pos);
        }
    }

    public static void switchStation(BlockPos pos) {
        if (pos == null) {
            return;
        }
        if (!stationByRadio.containsKey(pos)) {
            return;
        }
        List<RadioTrackRegistry.Station> stations = RadioTrackRegistry.getStations();
        if (stations.isEmpty()) {
            return;
        }
        int index = stationByRadio.getOrDefault(pos, 0);
        index = (index + 1) % stations.size();
        stationByRadio.put(pos, index);
        selectedStationName = stations.get(index).name();
        saveState();
    }

    public static String getStationName(BlockPos pos) {
        List<RadioTrackRegistry.Station> stations = RadioTrackRegistry.getStations();
        if (stations.isEmpty()) {
            return "Свободная волна";
        }
        int index = stationByRadio.getOrDefault(pos, resolveSelectedStationIndex());
        index = Math.floorMod(index, stations.size());
        return stations.get(index).name();
    }

    public static String getSelectedStationName() {
        ensureLoaded();
        if (selectedStationName == null || selectedStationName.isBlank()) {
            resolveSelectedStationIndex();
        }
        return selectedStationName == null ? "Свободная волна" : selectedStationName;
    }

    public static void clearAll() {
        stationByRadio.clear();
    }

    private static int resolveSelectedStationIndex() {
        ensureLoaded();
        List<RadioTrackRegistry.Station> stations = RadioTrackRegistry.getStations();
        if (stations.isEmpty()) {
            return 0;
        }
        if (selectedStationName != null) {
            for (int i = 0; i < stations.size(); i++) {
                if (stations.get(i).name().equals(selectedStationName)) {
                    return i;
                }
            }
        }
        selectedStationName = stations.get(0).name();
        saveState();
        return 0;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path statePath = getStatePath();
        if (statePath == null || !Files.exists(statePath)) {
            return;
        }
        try {
            String value = Files.readString(statePath).trim();
            if (!value.isEmpty()) {
                selectedStationName = value;
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("ClientRadioManager: failed to read radio state", e);
        }
    }

    private static void saveState() {
        Path statePath = getStatePath();
        if (statePath == null) {
            return;
        }
        try {
            Files.createDirectories(statePath.getParent());
            Files.writeString(statePath, selectedStationName == null ? "" : selectedStationName);
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("ClientRadioManager: failed to save radio state", e);
        }
    }

    private static Path getStatePath() {
        Path base = FMLPaths.GAMEDIR.get().resolve("run").resolve("config");
        if (!Files.exists(base)) {
            base = FMLPaths.CONFIGDIR.get();
        }
        return base.resolve("marallyzen").resolve("radio_state.txt");
    }
}
