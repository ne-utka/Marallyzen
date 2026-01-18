package neutka.marallys.marallyzen.radio;

import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.audio.MarallyzenAudioService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RadioTrackRegistry {
    private static final String DEFAULT_STATION = "Свободная волна";
    private static final List<Station> cachedStations = new ArrayList<>();
    private static boolean loaded = false;

    private RadioTrackRegistry() {}

    public record Station(String name, List<String> tracks) {}

    public static synchronized List<Station> getStations() {
        if (!loaded) {
            loadTracks();
        }
        return java.util.Collections.unmodifiableList(cachedStations);
    }

    private static void loadTracks() {
        loaded = true;
        cachedStations.clear();

        Path radioDir = MarallyzenAudioService.getAudioBaseDir().resolve("radio");
        List<Station> stations = listStationsFromDisk(radioDir);
        if (stations.isEmpty()) {
            Marallyzen.LOGGER.warn("RadioTrackRegistry: no radio stations found under {}", radioDir);
            return;
        }

        cachedStations.addAll(stations);
    }

    private static List<Station> listStationsFromDisk(Path radioDir) {
        if (radioDir == null || !Files.isDirectory(radioDir)) {
            return List.of();
        }
        java.util.Map<String, List<String>> tracksByStation = new java.util.LinkedHashMap<>();

        try (var rootStream = Files.list(radioDir)) {
            rootStream.filter(path -> Files.isRegularFile(path)
                    && isAudioFile(path.getFileName().toString()))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String relative = "radio/" + DEFAULT_STATION + "/" + fileName;
                    tracksByStation.computeIfAbsent(DEFAULT_STATION, key -> new ArrayList<>())
                        .add(relative);
                });
        } catch (IOException e) {
            Marallyzen.LOGGER.warn("RadioTrackRegistry: failed to list radio root {}", radioDir, e);
        }

        try (var stationStream = Files.list(radioDir)) {
            stationStream.filter(Files::isDirectory)
                .forEach(stationDir -> {
                    String stationName = stationDir.getFileName().toString();
                    List<String> tracks = tracksByStation.computeIfAbsent(stationName, key -> new ArrayList<>());
                    try (var trackStream = Files.list(stationDir)) {
                        trackStream.filter(path -> Files.isRegularFile(path)
                                && isAudioFile(path.getFileName().toString()))
                            .forEach(path -> {
                                String fileName = path.getFileName().toString();
                                tracks.add("radio/" + stationName + "/" + fileName);
                            });
                    } catch (IOException e) {
                        Marallyzen.LOGGER.warn("RadioTrackRegistry: failed to list station {}", stationName, e);
                    }
                });
        } catch (IOException e) {
            Marallyzen.LOGGER.warn("RadioTrackRegistry: failed to list station dirs {}", radioDir, e);
        }

        List<String> orderedNames = new ArrayList<>(tracksByStation.keySet());
        orderedNames.sort((a, b) -> {
            if (DEFAULT_STATION.equals(a)) {
                return -1;
            }
            if (DEFAULT_STATION.equals(b)) {
                return 1;
            }
            return a.compareToIgnoreCase(b);
        });

        List<Station> stations = new ArrayList<>();
        for (String name : orderedNames) {
            List<String> tracks = tracksByStation.getOrDefault(name, List.of());
            if (!tracks.isEmpty()) {
                stations.add(new Station(name, List.copyOf(tracks)));
            }
        }
        return stations;
    }

    private static boolean isAudioFile(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".ogg") || lower.endsWith(".wav") || lower.endsWith(".mp3");
    }
}
