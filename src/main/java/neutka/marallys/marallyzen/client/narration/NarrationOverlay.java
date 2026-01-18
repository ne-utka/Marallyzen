package neutka.marallys.marallyzen.client.narration;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * Represents a narration overlay with state machine for fade-in, show, and fade-out animations.
 * Bedrock-style narration display with controlled timing.
 */
public class NarrationOverlay {
    
    /**
     * State of the narration overlay.
     */
    public enum State {
        FADE_IN,
        SHOW,
        FADE_OUT,
        HIDDEN
    }
    
    private Component text;
    private UUID npcUuid;
    private State state = State.HIDDEN;
    private float alpha = 0.0f;
    
    // Previous alpha for smooth interpolation (60fps)
    private float previousAlpha = 0.0f;
    
    // Timing configuration
    private int fadeInTicks;
    private int stayTicks;
    private int fadeOutTicks;
    
    // Current progress
    private int fadeInProgress = 0;
    private int stayProgress = 0;
    private int fadeOutProgress = 0;
    
    /**
     * Starts a new narration with specified timing.
     * 
     * @param text The text to display
     * @param npcUuid The UUID of the NPC (can be null)
     * @param fadeInTicks Number of ticks for fade-in animation
     * @param stayTicks Number of ticks to stay fully visible
     * @param fadeOutTicks Number of ticks for fade-out animation
     */
    public void start(Component text, UUID npcUuid, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        this.text = text;
        this.npcUuid = npcUuid;
        this.fadeInTicks = Math.max(1, fadeInTicks);
        this.stayTicks = Math.max(0, stayTicks);
        this.fadeOutTicks = Math.max(1, fadeOutTicks);
        
        this.state = State.FADE_IN;
        this.alpha = 0.0f;
        this.fadeInProgress = 0;
        this.stayProgress = 0;
        this.fadeOutProgress = 0;
    }
    
    /**
     * Updates the overlay state machine. Should be called every client tick.
     * Saves previous alpha for smooth interpolation at 60fps.
     */
    public void tick() {
        // Save previous alpha for interpolation
        previousAlpha = alpha;
        
        switch (state) {
            case FADE_IN:
                fadeInProgress++;
                if (fadeInProgress >= fadeInTicks) {
                    // Fade-in complete, transition to SHOW
                    alpha = 1.0f;
                    state = State.SHOW;
                    stayProgress = 0;
                } else {
                    // Interpolate alpha from 0 to 1
                    alpha = (float) fadeInProgress / (float) fadeInTicks;
                }
                break;
                
            case SHOW:
                stayProgress++;
                alpha = 1.0f; // Keep fully visible
                if (stayProgress >= stayTicks) {
                    // Stay period complete, transition to FADE_OUT
                    state = State.FADE_OUT;
                    fadeOutProgress = 0;
                }
                break;
                
            case FADE_OUT:
                fadeOutProgress++;
                if (fadeOutProgress >= fadeOutTicks) {
                    // Fade-out complete, transition to HIDDEN
                    alpha = 0.0f;
                    state = State.HIDDEN;
                } else {
                    // Interpolate alpha from 1 to 0
                    alpha = 1.0f - ((float) fadeOutProgress / (float) fadeOutTicks);
                }
                break;
                
            case HIDDEN:
                // Do nothing, already hidden
                alpha = 0.0f;
                break;
        }
    }
    
    /**
     * Gets the previous alpha value for smooth interpolation.
     * 
     * @return Previous alpha value
     */
    public float getPreviousAlpha() {
        return previousAlpha;
    }
    
    /**
     * Checks if the overlay should be rendered.
     * 
     * @return true if overlay is visible (not HIDDEN)
     */
    public boolean isVisible() {
        // Use a small threshold to prevent flickering when alpha is very close to 0
        return state != State.HIDDEN && alpha >= 0.01f;
    }
    
    /**
     * Gets the current alpha value for rendering (0.0 to 1.0).
     * 
     * @return Current alpha value
     */
    public float getAlpha() {
        return Mth.clamp(alpha, 0.0f, 1.0f);
    }
    
    /**
     * Gets the text to display.
     * 
     * @return The narration text
     */
    public Component getText() {
        return text;
    }

    /**
     * Updates the displayed text without restarting the animation state.
     */
    public void updateText(Component text) {
        this.text = text;
    }
    
    /**
     * Gets the NPC UUID associated with this narration.
     * 
     * @return NPC UUID, or null if not associated with an NPC
     */
    public UUID getNpcUuid() {
        return npcUuid;
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
     * Forcefully clears the narration (immediately hides it).
     */
    public void clear() {
        this.state = State.HIDDEN;
        this.alpha = 0.0f;
        this.text = null;
        this.npcUuid = null;
    }
    
    /**
     * Starts fade-out animation if narration is currently showing.
     * If narration is in SHOW state, transitions to FADE_OUT.
     * If already fading out or hidden, does nothing.
     */
    public void startFadeOut() {
        if (this.state == State.SHOW) {
            this.state = State.FADE_OUT;
            this.fadeOutProgress = 0;
        }
    }
}
