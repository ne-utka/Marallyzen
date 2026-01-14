package neutka.marallys.marallyzen.client.narration;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.ClientDictaphoneManager;

import java.util.UUID;

/**
 * Singleton manager for narration and proximity overlays.
 * Manages the active narration overlay and proximity overlay.
 */
public class NarrationManager {
    
    private static NarrationManager instance;
    
    private final NarrationOverlay narrationOverlay;
    private final ProximityOverlay proximityOverlay;
    
    // Track when narration ended to prevent proximity from showing immediately
    private int lastNarrationEndTick = -1;
    private static final int PROXIMITY_DELAY_AFTER_NARRATION = 10; // 10 ticks = 0.5 seconds delay
    
    // Track if we've already sent completion packet for current narration (prevent duplicate sends)
    private boolean narrationCompletePacketSent = false;
    
    private NarrationManager() {
        this.narrationOverlay = new NarrationOverlay();
        this.proximityOverlay = new ProximityOverlay();
    }
    
    /**
     * Gets the singleton instance of NarrationManager.
     * 
     * @return The singleton instance
     */
    public static NarrationManager getInstance() {
        if (instance == null) {
            instance = new NarrationManager();
        }
        return instance;
    }
    
    /**
     * Starts a new narration overlay.
     * If a narration is already active, it will be replaced.
     * 
     * @param text The text to display
     * @param npcUuid The UUID of the NPC (can be null)
     * @param fadeInTicks Number of ticks for fade-in animation
     * @param stayTicks Number of ticks to stay fully visible
     * @param fadeOutTicks Number of ticks for fade-out animation
     */
    public void startNarration(Component text, UUID npcUuid, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        // Reset delay timer and completion flag when new narration starts
        lastNarrationEndTick = -1;
        narrationCompletePacketSent = false;
        ClientDictaphoneManager.onNarrationStart();
        
        // IMPORTANT: Clear previous narration before starting new one to prevent overlap/duplication
        // This ensures that if a previous message is still displaying, it's immediately cleared
        if (narrationOverlay.isVisible()) {
            narrationOverlay.clear();
        }
        
        narrationOverlay.start(text, npcUuid, fadeInTicks, stayTicks, fadeOutTicks);
    }
    
    /**
     * Clears the active narration overlay immediately.
     */
    public void clearNarration() {
        narrationOverlay.clear();
    }
    
    /**
     * Starts fade-out animation for the active narration overlay.
     * If narration is currently showing, it will fade out smoothly.
     */
    public void startNarrationFadeOut() {
        narrationOverlay.startFadeOut();
    }
    
    /**
     * Gets the active narration overlay.
     * 
     * @return The active overlay, or null if no narration is active
     */
    public NarrationOverlay getActive() {
        return narrationOverlay.isVisible() ? narrationOverlay : null;
    }
    
    /**
     * Updates the proximity overlay with new text and alpha.
     * Should be called every tick to update animation smoothly.
     * 
     * @param text The text to display
     * @param npcUuid The UUID of the NPC (can be null)
     * @param alpha The alpha value (0.0 to 1.0)
     */
    public void updateProximity(Component text, UUID npcUuid, float alpha) {
        proximityOverlay.update(text, npcUuid, Mth.clamp(alpha, 0.0f, 1.0f));
    }
    
    /**
     * Ticks the proximity overlay to save previous alpha for smooth interpolation.
     * Should be called every client tick for smooth animation.
     */
    public void tickProximity() {
        // Save previous alpha for interpolation (called before rendering)
        proximityOverlay.tick();
    }
    
    /**
     * Clears the proximity overlay.
     */
    public void clearProximity() {
        proximityOverlay.clear();
    }
    
    /**
     * Gets the active proximity overlay.
     * 
     * @return The proximity overlay, or null if not visible or if narration just ended
     */
    public ProximityOverlay getProximity() {
        if (!proximityOverlay.isVisible()) {
            return null;
        }
        
        // Don't show proximity if narration is active
        if (narrationOverlay.isVisible()) {
            return null;
        }
        
        // Check if proximity text is navigation message (when dialog is open)
        // Navigation messages should show immediately without delay
        Component proximityText = proximityOverlay.getText();
        boolean isNavigationMessage = proximityText != null && proximityText.getString().contains("Для навигации");
        
        // Don't show proximity immediately after narration ends (prevent flickering)
        // BUT: if it's a navigation message (dialog is open), show it immediately
        if (!isNavigationMessage && lastNarrationEndTick >= 0 && net.minecraft.client.Minecraft.getInstance().level != null) {
            int currentTick = (int) net.minecraft.client.Minecraft.getInstance().level.getGameTime();
            int ticksSinceNarrationEnd = currentTick - lastNarrationEndTick;
            if (ticksSinceNarrationEnd < PROXIMITY_DELAY_AFTER_NARRATION) {
                return null;
            }
        }
        
        return proximityOverlay;
    }
    
    /**
     * Updates the narration overlay state machine.
     * Should be called every client tick.
     */
    public void tick() {
        // Tick proximity overlay first to update previousAlpha
        tickProximity();
        
        boolean wasVisible = narrationOverlay.isVisible();
        NarrationOverlay.State previousState = narrationOverlay.getState();
        narrationOverlay.tick();
        boolean isVisible = narrationOverlay.isVisible();
        NarrationOverlay.State currentState = narrationOverlay.getState();
        
        // Track when narration ends (transitions to HIDDEN)
        if (previousState != NarrationOverlay.State.HIDDEN && currentState == NarrationOverlay.State.HIDDEN) {
            // Narration just completed (fade-out finished) - notify server
            if (net.minecraft.client.Minecraft.getInstance().level != null && !narrationCompletePacketSent) {
                lastNarrationEndTick = (int) net.minecraft.client.Minecraft.getInstance().level.getGameTime();
                narrationCompletePacketSent = true;
                
                // Send packet to server to notify that narration is complete
                neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                        new neutka.marallys.marallyzen.network.NarrationCompletePacket()
                );
                ClientDictaphoneManager.onNarrationComplete();
            }
        } else if (wasVisible && !isVisible) {
            // Fallback: also track when narration becomes invisible (for proximity delay)
            if (net.minecraft.client.Minecraft.getInstance().level != null) {
                lastNarrationEndTick = (int) net.minecraft.client.Minecraft.getInstance().level.getGameTime();
            }
        }
    }
}
