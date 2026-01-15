package neutka.marallys.marallyzen.client.cutscene;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Represents a screen fade overlay with state machine for fade-out, black screen, and fade-in animations.
 * Bedrock-style cinematic screen transition with text overlay.
 */
public class ScreenFadeOverlay {
    
    /**
     * State of the screen fade overlay.
     */
    public enum State {
        FADE_OUT,      // Screen is fading to black
        BLACK_SCREEN,  // Screen is fully black, showing text
        FADE_IN,       // Screen is fading back to normal
        HIDDEN         // Overlay is hidden
    }
    
    private State state = State.HIDDEN;
    private float overlayAlpha = 0.0f; // Alpha of the black overlay (0.0 = transparent, 1.0 = fully black)
    
    // Previous alpha for smooth interpolation (60fps)
    private float previousOverlayAlpha = 0.0f;
    
    // Text display
    private Component titleText;
    private Component subtitleText;
    private String soundId; // ResourceLocation ID of sound to play when text appears
    private float titleAlpha = 0.0f; // Alpha of the title text (0.0 = invisible, 1.0 = fully visible)
    private float subtitleAlpha = 0.0f; // Alpha of the subtitle text (0.0 = invisible, 1.0 = fully visible)
    private float previousTitleAlpha = 0.0f;
    private float previousSubtitleAlpha = 0.0f;
    private boolean soundPlayed = false; // Track if sound has been played
    
    // Timing configuration
    private int fadeOutTicks;
    private int blackScreenTicks;
    private int fadeInTicks;
    private int textFadeOutTicks = 10; // 0.5 seconds at 20 TPS for fade-out
    private int titleSubtitleDelay = 5; // Delay between title and subtitle fade-out (0.25 seconds)
    
    // Current progress
    private int fadeOutProgress = 0;
    private int blackScreenProgress = 0;
    private int fadeInProgress = 0;
    private int titleFadeOutProgress = 0;
    private int subtitleFadeOutProgress = 0;
    
    // Flags
    private boolean textVisible = false;
    private boolean titleFadingOut = false;
    private boolean subtitleFadingOut = false;
    
    /**
     * Starts a new screen fade with specified timing.
     * 
     * @param fadeOutTicks Number of ticks for fade-out animation
     * @param blackScreenTicks Number of ticks to stay fully black
     * @param fadeInTicks Number of ticks for fade-in animation
     * @param titleText Main text to display during black screen
     * @param subtitleText Subtitle text to display below main text
     * @param soundId ResourceLocation ID of sound to play when text appears (can be null)
     */
    public void start(int fadeOutTicks, int blackScreenTicks, int fadeInTicks, Component titleText, Component subtitleText, String soundId) {
        this.fadeOutTicks = Math.max(1, fadeOutTicks);
        this.blackScreenTicks = Math.max(0, blackScreenTicks);
        this.fadeInTicks = Math.max(1, fadeInTicks);
        this.titleText = titleText;
        this.subtitleText = subtitleText;
        this.soundId = soundId;
        
        this.state = State.FADE_OUT;
        this.overlayAlpha = 0.0f;
        this.titleAlpha = 0.0f;
        this.subtitleAlpha = 0.0f;
        this.fadeOutProgress = 0;
        this.blackScreenProgress = 0;
        this.fadeInProgress = 0;
        this.titleFadeOutProgress = 0;
        this.subtitleFadeOutProgress = 0;
        this.textVisible = false;
        this.titleFadingOut = false;
        this.subtitleFadingOut = false;
        this.soundPlayed = false;
    }
    
    /**
     * Updates the overlay state machine. Should be called every client tick.
     * Saves previous alpha for smooth interpolation at 60fps.
     */
    public void tick() {
        // Save previous alpha for interpolation
        previousOverlayAlpha = overlayAlpha;
        previousTitleAlpha = titleAlpha;
        previousSubtitleAlpha = subtitleAlpha;
        
        switch (state) {
            case FADE_OUT:
                fadeOutProgress++;
                if (fadeOutProgress >= fadeOutTicks) {
                    // Fade-out complete, transition to BLACK_SCREEN
                    overlayAlpha = 1.0f;
                    state = State.BLACK_SCREEN;
                    blackScreenProgress = 0;
                    // Make text visible instantly
                    textVisible = true;
                    titleAlpha = 1.0f;
                    subtitleAlpha = 1.0f;
                    // Play sound when text appears (at start of BLACK_SCREEN)
                    if (soundId != null && !soundId.isEmpty() && !soundPlayed) {
                        playSound(soundId);
                        soundPlayed = true;
                    }
                } else {
                    // Interpolate alpha from 0 to 1 with ease_in_out
                    float t = (float) fadeOutProgress / (float) fadeOutTicks;
                    overlayAlpha = easeInOut(t);
                }
                break;
                
            case BLACK_SCREEN:
                blackScreenProgress++;
                
                // Text is already fully visible (appeared instantly when BLACK_SCREEN started)
                // Now handle fade-out at the end of black screen phase
                
                // Calculate when to start fade-out (before fade-in phase begins)
                int fadeOutStartTick = blackScreenTicks - textFadeOutTicks - titleSubtitleDelay;
                
                // Start title fade-out first
                if (blackScreenProgress >= fadeOutStartTick && !titleFadingOut) {
                    titleFadingOut = true;
                    titleFadeOutProgress = 0;
                }
                
                // Handle title fade-out
                if (titleFadingOut) {
                    if (titleFadeOutProgress < textFadeOutTicks) {
                        titleFadeOutProgress++;
                        float t = (float) titleFadeOutProgress / (float) textFadeOutTicks;
                        titleAlpha = 1.0f - t; // Linear fade-out for title
                    } else {
                        titleAlpha = 0.0f; // Fully invisible
                    }
                }
                
                // Start subtitle fade-out after title starts fading (with delay)
                int subtitleFadeOutStartTick = fadeOutStartTick + titleSubtitleDelay;
                if (blackScreenProgress >= subtitleFadeOutStartTick && !subtitleFadingOut) {
                    subtitleFadingOut = true;
                    subtitleFadeOutProgress = 0;
                }
                
                // Handle subtitle fade-out
                if (subtitleFadingOut) {
                    if (subtitleFadeOutProgress < textFadeOutTicks) {
                        subtitleFadeOutProgress++;
                        float t = (float) subtitleFadeOutProgress / (float) textFadeOutTicks;
                        subtitleAlpha = 1.0f - t; // Linear fade-out for subtitle
                    } else {
                        subtitleAlpha = 0.0f; // Fully invisible
                    }
                }
                
                // Check if black screen period is complete
                if (blackScreenProgress >= blackScreenTicks) {
                    // Transition to FADE_IN
                    state = State.FADE_IN;
                    fadeInProgress = 0;
                }
                break;
                
            case FADE_IN:
                fadeInProgress++;
                if (fadeInProgress >= fadeInTicks) {
                    // Fade-in complete, transition to HIDDEN
                    overlayAlpha = 0.0f;
                    titleAlpha = 0.0f;
                    subtitleAlpha = 0.0f;
                    state = State.HIDDEN;
                } else {
                    // Interpolate alpha from 1 to 0 with ease_in_out
                    float t = (float) fadeInProgress / (float) fadeInTicks;
                    overlayAlpha = 1.0f - easeInOut(t);
                }
                break;
                
            case HIDDEN:
                // Do nothing, already hidden
                overlayAlpha = 0.0f;
                titleAlpha = 0.0f;
                subtitleAlpha = 0.0f;
                break;
        }
    }
    
    /**
     * Easing function: ease_in_out (Bedrock-style).
     * Smooth acceleration and deceleration.
     */
    private float easeInOut(float t) {
        if (t < 0.5f) {
            return 2.0f * t * t;
        } else {
            float t2 = -2.0f * t + 2.0f;
            return 1.0f - (t2 * t2) / 2.0f;
        }
    }
    
    /**
     * Gets the previous overlay alpha value for smooth interpolation.
     * 
     * @return Previous overlay alpha value
     */
    public float getPreviousOverlayAlpha() {
        return previousOverlayAlpha;
    }
    
    /**
     * Gets the previous title alpha value for smooth interpolation.
     * 
     * @return Previous title alpha value
     */
    public float getPreviousTitleAlpha() {
        return previousTitleAlpha;
    }
    
    /**
     * Gets the previous subtitle alpha value for smooth interpolation.
     * 
     * @return Previous subtitle alpha value
     */
    public float getPreviousSubtitleAlpha() {
        return previousSubtitleAlpha;
    }
    
    /**
     * Checks if the overlay should be rendered.
     * 
     * @return true if overlay is visible (not HIDDEN)
     */
    public boolean isVisible() {
        return state != State.HIDDEN && overlayAlpha > 0.0f;
    }
    
    /**
     * Gets the current overlay alpha value for rendering (0.0 to 1.0).
     * 
     * @return Current overlay alpha value
     */
    public float getOverlayAlpha() {
        return Mth.clamp(overlayAlpha, 0.0f, 1.0f);
    }
    
    /**
     * Gets the current title alpha value for rendering (0.0 to 1.0).
     * 
     * @return Current title alpha value
     */
    public float getTitleAlpha() {
        return Mth.clamp(titleAlpha, 0.0f, 1.0f);
    }
    
    /**
     * Gets the current subtitle alpha value for rendering (0.0 to 1.0).
     * 
     * @return Current subtitle alpha value
     */
    public float getSubtitleAlpha() {
        return Mth.clamp(subtitleAlpha, 0.0f, 1.0f);
    }
    
    /**
     * Gets the title text to display.
     * 
     * @return The title text, or null if not set
     */
    public Component getTitleText() {
        return titleText;
    }
    
    /**
     * Gets the subtitle text to display.
     * 
     * @return The subtitle text, or null if not set
     */
    public Component getSubtitleText() {
        return subtitleText;
    }
    
    /**
     * Gets the current state.
     * 
     * @return Current state
     */
    public State getState() {
        return state;
    }
    
    /**
     * Checks if text should be visible (during black screen phase).
     * 
     * @return true if text should be rendered
     */
    public boolean isTextVisible() {
        return textVisible && (state == State.BLACK_SCREEN || state == State.FADE_IN);
    }
    
    /**
     * Forcefully clears the overlay (immediately hides it).
     */
    public void clear() {
        this.state = State.HIDDEN;
        this.overlayAlpha = 0.0f;
        this.titleAlpha = 0.0f;
        this.subtitleAlpha = 0.0f;
        this.titleText = null;
        this.subtitleText = null;
        this.soundId = null;
        this.textVisible = false;
        this.titleFadingOut = false;
        this.subtitleFadingOut = false;
        this.soundPlayed = false;
    }
    
    /**
     * Plays a sound by ResourceLocation ID.
     * 
     * @param soundId ResourceLocation ID of the sound (e.g., "minecraft:block.anvil.land")
     */
    private void playSound(String soundId) {
        try {
            net.minecraft.resources.ResourceLocation soundLocation = net.minecraft.resources.ResourceLocation.parse(soundId);
            net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(soundLocation).ifPresentOrElse(
                soundEvent -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.player != null && mc.level != null) {
                        // Play sound at player position
                        mc.level.playSound(
                            mc.player,
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            soundEvent,
                            net.minecraft.sounds.SoundSource.MASTER,
                            1.0f, // volume
                            1.0f  // pitch
                        );
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[ScreenFadeOverlay] Playing sound: {}", soundId);
                    }
                },
                () -> {
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("[ScreenFadeOverlay] Sound not found in registry: {}", soundId);
                }
            );
        } catch (Exception e) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.error("[ScreenFadeOverlay] Failed to play sound: {}", soundId, e);
        }
    }
}

