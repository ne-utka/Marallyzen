package neutka.marallys.marallyzen.client.cutscene;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.ScreenFadeCompletePacket;

/**
 * Singleton manager for screen fade cutscenes.
 * Manages the active screen fade overlay and handles completion callbacks.
 */
public class ScreenFadeManager {
    
    private static ScreenFadeManager instance;
    
    private final ScreenFadeOverlay overlay;
    private boolean blockPlayerInput = false;
    
    // Track if we've already sent completion packet for current fade (prevent duplicate sends)
    private boolean fadeCompletePacketSent = false;
    
    // Track original hideGui state to restore it after fade
    private Boolean originalHideGui = null;
    
    private ScreenFadeManager() {
        this.overlay = new ScreenFadeOverlay();
    }
    
    /**
     * Gets the singleton instance of ScreenFadeManager.
     * 
     * @return The singleton instance
     */
    public static ScreenFadeManager getInstance() {
        if (instance == null) {
            instance = new ScreenFadeManager();
        }
        return instance;
    }
    
    /**
     * Starts a new screen fade cutscene.
     * 
     * @param fadeOutTicks Number of ticks for fade-out animation
     * @param blackScreenTicks Number of ticks to stay fully black
     * @param fadeInTicks Number of ticks for fade-in animation
     * @param titleText Main text to display during black screen
     * @param subtitleText Subtitle text to display below main text
     * @param blockPlayerInput Whether to block player input during the fade
     * @param soundId ResourceLocation ID of sound to play when text appears (can be null)
     */
    public void startScreenFade(int fadeOutTicks, int blackScreenTicks, int fadeInTicks, 
                                Component titleText, Component subtitleText, boolean blockPlayerInput, String soundId) {
        String titleStr = titleText != null ? titleText.getString() : "null";
        String subtitleStr = subtitleText != null ? subtitleText.getString() : "null";
        String soundStr = soundId != null ? soundId : "null";
        Marallyzen.LOGGER.info("[ScreenFadeManager] startScreenFade - fadeOut={}, blackScreen={}, fadeIn={}, title='{}', subtitle='{}', sound='{}', blockInput={}", 
                fadeOutTicks, blackScreenTicks, fadeInTicks, titleStr, subtitleStr, soundStr, blockPlayerInput);
        
        // Reset completion flag when new fade starts
        fadeCompletePacketSent = false;
        this.blockPlayerInput = blockPlayerInput;
        
        // Close any open dialogs before starting fade
        closeDialogs();
        
        // Hide HUD (F1 mode) and narration overlay during screen fade
        hideHudAndNarration();
        
        overlay.start(fadeOutTicks, blackScreenTicks, fadeInTicks, titleText, subtitleText, soundId);
    }
    
    /**
     * Closes any open dialogs before starting screen fade.
     */
    private void closeDialogs() {
        // Close dialog if open
        var dialogHud = neutka.marallys.marallyzen.client.gui.DialogHud.getInstance();
        if (dialogHud.isVisible()) {
            Marallyzen.LOGGER.info("[ScreenFadeManager] Closing dialog before screen fade");
            var stateMachine = neutka.marallys.marallyzen.client.gui.DialogStateMachine.getInstance();
            stateMachine.transitionTo(neutka.marallys.marallyzen.client.gui.DialogState.CLOSED);
        }
    }
    
    /**
     * Clears the active screen fade overlay immediately.
     */
    public void clearScreenFade() {
        Marallyzen.LOGGER.info("[ScreenFadeManager] clearScreenFade");
        overlay.clear();
        blockPlayerInput = false;
        fadeCompletePacketSent = false;
        
        // Restore HUD visibility
        restoreHudAndNarration();
    }
    
    /**
     * Hides HUD (F1 mode) and narration overlay during screen fade.
     */
    private void hideHudAndNarration() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            // Save original hideGui state
            if (originalHideGui == null) {
                originalHideGui = mc.options.hideGui;
            }
            // Enable hideGui (F1 mode) to hide HUD elements
            mc.options.hideGui = true;
            Marallyzen.LOGGER.info("[ScreenFadeManager] Hiding HUD (F1 mode enabled)");
        }
        
        // Clear narration overlay
        var narrationManager = neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance();
        narrationManager.clearNarration();
        narrationManager.clearProximity();
        Marallyzen.LOGGER.info("[ScreenFadeManager] Cleared narration overlay");
    }
    
    /**
     * Restores HUD visibility after screen fade.
     */
    private void restoreHudAndNarration() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && originalHideGui != null) {
            // Restore original hideGui state
            mc.options.hideGui = originalHideGui;
            originalHideGui = null;
            Marallyzen.LOGGER.info("[ScreenFadeManager] Restored HUD visibility (F1 mode disabled)");
        }
    }
    
    /**
     * Gets the active screen fade overlay.
     * 
     * @return The active overlay, or null if no fade is active
     */
    public ScreenFadeOverlay getActive() {
        return overlay.isVisible() ? overlay : null;
    }
    
    /**
     * Checks if a screen fade is currently active.
     * 
     * @return true if a screen fade is active
     */
    public boolean isActive() {
        return overlay.isVisible();
    }
    
    /**
     * Checks if player input should be blocked.
     * 
     * @return true if player input is blocked
     */
    public boolean shouldBlockPlayerInput() {
        return isActive() && blockPlayerInput;
    }
    
    /**
     * Updates the overlay state machine. Should be called every client tick.
     */
    public void tick() {
        ScreenFadeOverlay.State previousState = overlay.getState();
        overlay.tick();
        ScreenFadeOverlay.State currentState = overlay.getState();
        
        // Track when fade ends (transitions to HIDDEN)
        if (previousState != ScreenFadeOverlay.State.HIDDEN && currentState == ScreenFadeOverlay.State.HIDDEN) {
            // Fade just completed - notify server
            if (Minecraft.getInstance().level != null && !fadeCompletePacketSent) {
                fadeCompletePacketSent = true;
                blockPlayerInput = false;
                Marallyzen.LOGGER.info("[ScreenFadeManager] Screen fade completed (state=HIDDEN), sending ScreenFadeCompletePacket to server");
                
                // Restore HUD visibility
                restoreHudAndNarration();
                
                // Send packet to server to notify that fade is complete
                NetworkHelper.sendToServer(new ScreenFadeCompletePacket());
            }
        }
    }
}

