package neutka.marallys.marallyzen.audio;

import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for reading audio file metadata.
 * Used to get duration and other information about audio files for synchronization.
 */
public final class AudioMetadata {
    
    /**
     * Gets the duration of an audio file in milliseconds.
     * 
     * @param filePath Path to audio file (relative to config/marallyzen/audio/)
     * @return Duration in milliseconds, or -1 if unable to determine
     */
    public static long getDurationMs(String filePath) {
        try {
            Path audioFile = resolveAudioPath(filePath);
            
            if (audioFile == null || !audioFile.toFile().exists()) {
                return getDurationFromResource(filePath);
            }
            
            // Try to get duration using Java Sound API
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(audioFile.toFile());
            
            if (fileFormat.properties().containsKey("duration")) {
                Long duration = (Long) fileFormat.properties().get("duration");
                if (duration != null) {
                    return duration / 1_000_000; // Convert nanoseconds to milliseconds
                }
            }
            
            // Fallback: calculate from frame length and frame rate
            try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile.toFile())) {
                long frames = audioInputStream.getFrameLength();
                float frameRate = audioInputStream.getFormat().getFrameRate();
                
                if (frames != AudioSystem.NOT_SPECIFIED && frameRate != AudioSystem.NOT_SPECIFIED) {
                    return (long) ((frames / frameRate) * 1000.0);
                }
            }
            
            return -1;
        } catch (Exception e) {
            Marallyzen.LOGGER.debug("Failed to get audio metadata for {}: {}", filePath, e.getMessage());
            return -1;
        }
    }

    private static Path resolveAudioPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        Path audioFile = MarallyzenAudioService.getAudioBaseDir().resolve(filePath);
        if (audioFile.toFile().exists()) {
            return audioFile;
        }
        String normalized = filePath.trim();
        if (!normalized.toLowerCase(java.util.Locale.ROOT).endsWith(".wav")) {
            String base = normalized.replaceAll("\\.[^.]+$", "");
            Path wavPath = MarallyzenAudioService.getAudioBaseDir().resolve(base + ".wav");
            if (wavPath.toFile().exists()) {
                return wavPath;
            }
        }
        return audioFile;
    }

    private static long getDurationFromResource(String filePath) {
        String resourcePath = "assets/" + Marallyzen.MODID + "/sounds/" + filePath;
        try (InputStream formatStream = AudioMetadata.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (formatStream == null) {
                Marallyzen.LOGGER.warn("Audio resource not found for metadata: {}", resourcePath);
                return -1;
            }
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(new BufferedInputStream(formatStream));
            if (fileFormat.properties().containsKey("duration")) {
                Long duration = (Long) fileFormat.properties().get("duration");
                if (duration != null) {
                    return duration / 1_000_000;
                }
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.debug("Failed to read audio metadata from resource {}: {}", resourcePath, e.getMessage());
        }
        try (InputStream audioStream = AudioMetadata.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (audioStream == null) {
                return -1;
            }
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(audioStream))) {
                long frames = ais.getFrameLength();
                float frameRate = ais.getFormat().getFrameRate();
                if (frames != AudioSystem.NOT_SPECIFIED && frameRate != AudioSystem.NOT_SPECIFIED) {
                    return (long) ((frames / frameRate) * 1000.0);
                }
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.debug("Failed to compute audio duration from resource {}: {}", resourcePath, e.getMessage());
        }
        return -1;
    }
    
    /**
     * Gets the duration of an audio file in ticks (20 ticks = 1 second).
     * 
     * @param filePath Path to audio file (relative to config/marallyzen/audio/)
     * @return Duration in ticks, or -1 if unable to determine
     */
    public static int getDurationTicks(String filePath) {
        long durationMs = getDurationMs(filePath);
        if (durationMs < 0) {
            return -1;
        }
        return (int) (durationMs / 50); // 50ms per tick
    }
}





























