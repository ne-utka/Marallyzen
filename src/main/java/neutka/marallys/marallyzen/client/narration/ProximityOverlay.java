package neutka.marallys.marallyzen.client.narration;

import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Represents a proximity overlay that shows NPC proximity text.
 * Simpler than NarrationOverlay - just updates text and alpha directly.
 */
public class ProximityOverlay {
    
    private Component text;
    private UUID npcUuid;
    private float alpha = 0.0f;
    private float previousAlpha = 0.0f; // Previous alpha for smooth interpolation (60fps)
    private boolean visible = false;
    
    /**
     * Updates the proximity overlay with new text and alpha.
     * 
     * @param text The text to display
     * @param npcUuid The UUID of the NPC (can be null)
     * @param alpha The alpha value (0.0 to 1.0)
     */
    public void update(Component text, UUID npcUuid, float alpha) {
        this.text = text;
        this.npcUuid = npcUuid;
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        this.visible = this.alpha > 0.0f && text != null;
    }
    
    /**
     * Ticks the proximity overlay to save previous alpha for smooth interpolation.
     * Should be called every client tick before rendering.
     */
    public void tick() {
        // Save previous alpha for interpolation (before any updates)
        this.previousAlpha = this.alpha;
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
        this.visible = false;
    }
    
    /**
     * Checks if the overlay should be rendered.
     * 
     * @return true if overlay is visible
     */
    public boolean isVisible() {
        return visible && alpha > 0.0f;
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

