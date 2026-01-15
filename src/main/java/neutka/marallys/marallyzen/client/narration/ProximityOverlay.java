package neutka.marallys.marallyzen.client.narration;

import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Represents a proximity overlay that shows NPC proximity text.
 * Simpler than NarrationOverlay - just updates text and alpha directly.
 */
public class ProximityOverlay {

    public enum State {
        FADE_IN,
        SHOW,
        FADE_OUT,
        HIDDEN
    }
    
    private Component text;
    private UUID npcUuid;
    private float alpha = 0.0f;
    private float previousAlpha = 0.0f; // Previous alpha for smooth interpolation (60fps)
    private State state = State.HIDDEN;
    private int fadeInProgress = 0;
    private int fadeOutProgress = 0;
    private int fadeInTicks = 5;
    private int fadeOutTicks = 3;

    /**
     * Updates the proximity overlay with new text and starts fade-in if needed.
     * 
     * @param text The text to display
     * @param npcUuid The UUID of the NPC (can be null)
     */
    public void update(Component text, UUID npcUuid) {
        if (text == null) {
            return;
        }
        boolean changed = this.text == null || !this.text.equals(text) || (this.npcUuid != null && !this.npcUuid.equals(npcUuid));
        this.text = text;
        this.npcUuid = npcUuid;
        if (state == State.HIDDEN || state == State.FADE_OUT || changed) {
            state = State.FADE_IN;
            fadeInProgress = 0;
        }
    }

    /**
     * Starts fade-out animation for the proximity overlay.
     */
    public void startFadeOut() {
        if (state == State.SHOW || state == State.FADE_IN) {
            state = State.FADE_OUT;
            fadeOutProgress = 0;
        }
    }
    
    /**
     * Ticks the proximity overlay to save previous alpha for smooth interpolation.
     * Should be called every client tick before rendering.
     */
    public void tick() {
        // Save previous alpha for interpolation (before any updates)
        this.previousAlpha = this.alpha;

        switch (state) {
            case FADE_IN:
                fadeInProgress++;
                if (fadeInProgress >= fadeInTicks) {
                    alpha = 1.0f;
                    state = State.SHOW;
                } else {
                    alpha = (float) fadeInProgress / (float) fadeInTicks;
                }
                break;
            case SHOW:
                alpha = 1.0f;
                break;
            case FADE_OUT:
                fadeOutProgress++;
                if (fadeOutProgress >= fadeOutTicks) {
                    alpha = 0.0f;
                    state = State.HIDDEN;
                    text = null;
                    npcUuid = null;
                } else {
                    alpha = 1.0f - ((float) fadeOutProgress / (float) fadeOutTicks);
                }
                break;
            case HIDDEN:
            default:
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
     * Clears the proximity overlay.
     */
    public void clear() {
        this.text = null;
        this.npcUuid = null;
        this.alpha = 0.0f;
        this.state = State.HIDDEN;
    }
    
    /**
     * Checks if the overlay should be rendered.
     * 
     * @return true if overlay is visible
     */
    public boolean isVisible() {
        return state != State.HIDDEN && alpha > 0.0f;
    }
    
    /**
     * Gets the current alpha value for rendering (0.0 to 1.0).
     * 
     * @return Current alpha value
     */
    public float getAlpha() {
        return alpha;
    }
    
    /**
     * Gets the text to display.
     * 
     * @return The proximity text
     */
    public Component getText() {
        return text;
    }
    
    /**
     * Gets the NPC UUID associated with this proximity overlay.
     * 
     * @return NPC UUID, or null if not associated with an NPC
     */
    public UUID getNpcUuid() {
        return npcUuid;
    }
}
