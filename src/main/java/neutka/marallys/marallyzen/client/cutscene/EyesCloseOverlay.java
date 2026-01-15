package neutka.marallys.marallyzen.client.cutscene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Represents an "eyes close" cutscene overlay with state machine for close/black/open animations.
 * Bedrock-style cinematic effect where eyelids close from top and bottom.
 */
public class EyesCloseOverlay {
    
    /**
     * State of the eyes close overlay.
     */
    public enum State {
        EYES_CLOSING,  // Eyelids moving toward center
        EYES_CLOSED,   // Screen fully black
        EYES_OPENING,  // Eyelids moving away from center
        HIDDEN         // Overlay is hidden
    }
    
    private State state = State.HIDDEN;
    
    // Eyelid position: 0.0 = open (at edges), 1.0 = closed (at center)
    private float eyelidPosition = 0.0f;
    private float previousEyelidPosition = 0.0f;
    
    // Timing configuration (in ticks, 20 ticks = 1 second)
    private int closeDurationTicks;
    private int blackDurationTicks;
    private int openDurationTicks;
    
    // Current progress
    private int closeProgress = 0;
    private int blackProgress = 0;
    private int openProgress = 0;
    
    // Sound configuration
    private static final String CLOSE_SOUND = "minecraft:block.deepslate.hit";
    private static final float CLOSE_SOUND_VOLUME = 0.7f;
    private static final float CLOSE_SOUND_PITCH = 0.8f;
    private static final float OPEN_SOUND_VOLUME = 0.5f;
    private static final float OPEN_SOUND_PITCH = 1.1f;
    
    // Flags
    private boolean closeSoundPlayed = false;
    private boolean openSoundPlayed = false;
    
    // Animation speed multiplier (3x faster for snappy, organic blink feel)
    private static final float SPEED_MULTIPLIER = 3.0f;
    
    /**
     * Starts a new eyes close cutscene with specified timing.
     * 
     * @param closeDurationTicks Number of ticks for eyes closing animation
     * @param blackDurationTicks Number of ticks to stay fully black
     * @param openDurationTicks Number of ticks for eyes opening animation
     */
    public void start(int closeDurationTicks, int blackDurationTicks, int openDurationTicks) {
        this.closeDurationTicks = Math.max(1, closeDurationTicks);
        this.blackDurationTicks = Math.max(0, blackDurationTicks);
        this.openDurationTicks = Math.max(1, openDurationTicks);
        
        this.state = State.EYES_CLOSING;
        this.eyelidPosition = 0.0f;
        this.previousEyelidPosition = 0.0f;
        this.closeProgress = 0;
        this.blackProgress = 0;
        this.openProgress = 0;
        this.closeSoundPlayed = false;
        this.openSoundPlayed = false;
        
        Marallyzen.LOGGER.info("[EyesCloseOverlay] Started - close={}t, black={}t, open={}t", 
                closeDurationTicks, blackDurationTicks, openDurationTicks);
    }
    
    /**
     * Clears the overlay immediately.
     */
    public void clear() {
        this.state = State.HIDDEN;
        this.eyelidPosition = 0.0f;
        this.previousEyelidPosition = 0.0f;
        this.closeProgress = 0;
        this.blackProgress = 0;
        this.openProgress = 0;
    }
    
    /**
     * Updates the overlay state machine. Should be called every client tick.
     * Saves previous position for smooth interpolation at 60fps.
     */
    public void tick() {
        // Save previous position for interpolation
        previousEyelidPosition = eyelidPosition;
        
        switch (state) {
            case EYES_CLOSING:
                // Play close sound at start
                if (!closeSoundPlayed) {
                    playSound(CLOSE_SOUND, CLOSE_SOUND_VOLUME, CLOSE_SOUND_PITCH);
                    closeSoundPlayed = true;
                }
                
                closeProgress++;
                // Apply speed multiplier for faster animation
                float effectiveCloseProgress = closeProgress * SPEED_MULTIPLIER;
                if (effectiveCloseProgress >= closeDurationTicks) {
                    eyelidPosition = 1.0f;
                    state = State.EYES_CLOSED;
                    blackProgress = 0;
                    Marallyzen.LOGGER.info("[EyesCloseOverlay] Eyes closed, starting black screen");
                } else {
                    // Ease-out cubic: fast start, slow end (cinematic closing)
                    float t = effectiveCloseProgress / (float) closeDurationTicks;
                    eyelidPosition = easeOutCubic(t);
                }
                break;
                
            case EYES_CLOSED:
                blackProgress++;
                eyelidPosition = 1.0f; // Fully closed
                
                if (blackProgress >= blackDurationTicks) {
                    state = State.EYES_OPENING;
                    openProgress = 0;
                    Marallyzen.LOGGER.info("[EyesCloseOverlay] Black screen complete, opening eyes");
                }
                break;
                
            case EYES_OPENING:
                // Play open sound at start
                if (!openSoundPlayed) {
                    playSound(CLOSE_SOUND, OPEN_SOUND_VOLUME, OPEN_SOUND_PITCH);
                    openSoundPlayed = true;
                }
                
                openProgress++;
                // Apply speed multiplier for faster animation
                float effectiveOpenProgress = openProgress * SPEED_MULTIPLIER;
                if (effectiveOpenProgress >= openDurationTicks) {
                    eyelidPosition = 0.0f;
                    state = State.HIDDEN;
                    Marallyzen.LOGGER.info("[EyesCloseOverlay] Eyes opened, cutscene complete");
                } else {
                    // Ease-in cubic for opening: gentle start, fast end (eyes snap open)
                    float t = effectiveOpenProgress / (float) openDurationTicks;
                    eyelidPosition = 1.0f - easeInCubic(t);
                }
                break;
                
            case HIDDEN:
                eyelidPosition = 0.0f;
                break;
        }
    }
    
    /**
     * Plays a sound with specified volume and pitch.
     */
    private void playSound(String soundId, float volume, float pitch) {
        try {
            ResourceLocation soundLocation = ResourceLocation.parse(soundId);
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundLocation);
            
            if (soundEvent != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.getSoundManager().play(new SimpleSoundInstance(
                            soundEvent,
                            SoundSource.MASTER,
                            volume,
                            pitch,
                            RandomSource.create(),
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ()
                    ));
                    Marallyzen.LOGGER.info("[EyesCloseOverlay] Playing sound: {} (vol={}, pitch={})", soundId, volume, pitch);
                }
            } else {
                Marallyzen.LOGGER.warn("[EyesCloseOverlay] Sound not found: {}", soundId);
            }
        } catch (Exception e) {
            Marallyzen.LOGGER.error("[EyesCloseOverlay] Failed to play sound: {}", soundId, e);
        }
    }
    
    /**
     * Ease-in function (quadratic): slow start, fast end.
     * t goes from 0 to 1, output goes from 0 to 1.
     */
    private float easeIn(float t) {
        return t * t;
    }
    
    /**
     * Ease-out function (quadratic): fast start, slow end.
     * t goes from 0 to 1, output goes from 0 to 1.
     */
    private float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }
    
    /**
     * Ease-out cubic function: fast start, gentle slow end.
     * More dramatic deceleration than quadratic - perfect for cinematic eyelid closing.
     * t goes from 0 to 1, output goes from 0 to 1.
     */
    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3);
    }
    
    /**
     * Ease-in cubic function: gentle start, fast end.
     * Perfect for eyes opening — slow wake then snap open.
     * t goes from 0 to 1, output goes from 0 to 1.
     */
    private float easeInCubic(float t) {
        return t * t * t;
    }
    
    // Getters
    
    public State getState() {
        return state;
    }
    
    public boolean isVisible() {
        return state != State.HIDDEN;
    }
    
    public float getEyelidPosition() {
        return Mth.clamp(eyelidPosition, 0.0f, 1.0f);
    }
    
    public float getPreviousEyelidPosition() {
        return Mth.clamp(previousEyelidPosition, 0.0f, 1.0f);
    }
    
    /**
     * Checks if eyes are fully closed (for hiding HUD).
     */
    public boolean isFullyClosed() {
        return state == State.EYES_CLOSED || eyelidPosition >= 0.99f;
    }
}

