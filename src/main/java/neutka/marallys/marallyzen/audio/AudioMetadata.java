package neutka.marallys.marallyzen.audio;

import net.neoforged.fml.loading.FMLPaths;
import neutka.marallys.marallyzen.Marallyzen;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
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
            Path audioFile = MarallyzenAudioService.getAudioBaseDir().resolve(filePath);
            
            if (!audioFile.toFile().exists()) {
                Marallyzen.LOGGER.warn("Audio file not found for metadata: {}", audioFile);
                return -1;
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





























