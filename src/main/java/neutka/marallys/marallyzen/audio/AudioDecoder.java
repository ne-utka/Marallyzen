package neutka.marallys.marallyzen.audio;

import neutka.marallys.marallyzen.Marallyzen;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * Utility class for decoding audio files to PCM samples.
 * Converts audio files (ogg, wav, mp3) to 16-bit PCM samples at 48kHz.
 */
public final class AudioDecoder {
    
    private static final int TARGET_SAMPLE_RATE = 48000;
    private static final int TARGET_CHANNELS = 1; // Mono
    private static final int TARGET_BITS_PER_SAMPLE = 16;
    
    /**
     * Decodes an audio file to PCM samples (16-bit, 48kHz, mono).
     * 
     * @param audioFile Path to audio file
     * @return Array of 16-bit PCM samples, or null if decoding failed
     */
    public static short[] decodeToPCM(Path audioFile) {
        try {
            File file = audioFile.toFile();
            if (!file.exists()) {
                Marallyzen.LOGGER.warn("Audio file not found for decoding: {}", audioFile);
                return null;
            }
            
            // Open audio file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat originalFormat = audioInputStream.getFormat();
            
            // Convert to target format (16-bit, 48kHz, mono)
            AudioFormat targetFormat = new AudioFormat(
                    TARGET_SAMPLE_RATE,
                    TARGET_BITS_PER_SAMPLE,
                    TARGET_CHANNELS,
                    true, // signed
                    false // little-endian
            );
            
            // If format doesn't match, convert it
            if (!originalFormat.matches(targetFormat)) {
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            }
            
            // Read all samples
            int bytesPerSample = TARGET_BITS_PER_SAMPLE / 8;
            int frameSize = TARGET_CHANNELS * bytesPerSample;
            byte[] buffer = new byte[4096];
            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            
            int bytesRead;
            while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            
            audioInputStream.close();
            
            // Convert bytes to short array (16-bit samples)
            byte[] audioBytes = byteArrayOutputStream.toByteArray();
            short[] samples = new short[audioBytes.length / bytesPerSample];
            ByteBuffer.wrap(audioBytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(samples);
            
            Marallyzen.LOGGER.debug("Decoded audio file {}: {} samples, {}ms duration", 
                    audioFile.getFileName(), samples.length, (samples.length * 1000) / TARGET_SAMPLE_RATE);
            
            return samples;
            
        } catch (UnsupportedAudioFileException e) {
            Marallyzen.LOGGER.error("Unsupported audio file format: {}", audioFile, e);
            return null;
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to decode audio file: {}", audioFile, e);
            return null;
        }
    }
    
    /**
     * Gets the duration of decoded PCM samples in milliseconds.
     */
    public static long getDurationMs(short[] samples) {
        if (samples == null) {
            return -1;
        }
        return (samples.length * 1000L) / TARGET_SAMPLE_RATE;
    }
}





























