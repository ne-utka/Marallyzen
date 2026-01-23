package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.audio.MarallyzenAudioService;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class QuestAudioPlayer {
    private QuestAudioPlayer() {
    }

    public static void play(String filePath, float volume) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        Path resolved = MarallyzenAudioService.getAudioBaseDir().resolve(filePath);
        if (!Files.exists(resolved)) {
            Path wavFallback = tryWavFallback(filePath);
            if (wavFallback != null) {
                resolved = wavFallback;
            }
        }
        if (!Files.exists(resolved)) {
            Marallyzen.LOGGER.warn("QuestAudioPlayer: file not found {}", resolved);
            return;
        }
        Path audioFile = resolved;
        Thread thread = new Thread(() -> playBlocking(audioFile, volume), "QuestAudioPlayer");
        thread.setDaemon(true);
        thread.start();
    }

    private static void playBlocking(Path audioFile, float volume) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(audioFile)))) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            applyVolume(clip, volume * getVoiceVolume());
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("QuestAudioPlayer: failed to play {}", audioFile, e);
        }
    }

    private static void applyVolume(Clip clip, float volume) {
        if (clip == null) {
            return;
        }
        float clamped = Math.max(0.0f, Math.min(1.0f, volume));
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float min = control.getMinimum();
        float max = control.getMaximum();
        if (clamped <= 0.0f) {
            control.setValue(min);
            return;
        }
        float db = (float) (20.0 * Math.log10(clamped));
        if (db < min) {
            db = min;
        } else if (db > max) {
            db = max;
        }
        control.setValue(db);
    }

    private static float getVoiceVolume() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return 1.0f;
        }
        return mc.options.getSoundSourceVolume(SoundSource.VOICE);
    }

    private static Path tryWavFallback(String filePath) {
        String normalized = filePath.trim();
        if (normalized.toLowerCase(java.util.Locale.ROOT).endsWith(".wav")) {
            return null;
        }
        String base = normalized.replaceAll("\\.[^.]+$", "");
        Path wavPath = MarallyzenAudioService.getAudioBaseDir().resolve(base + ".wav");
        return Files.exists(wavPath) ? wavPath : null;
    }
}
