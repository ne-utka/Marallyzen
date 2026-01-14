package neutka.marallys.marallyzen.client.cutscene;

import net.minecraft.client.Minecraft;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.EyesCloseCompletePacket;

/**
 * Singleton manager for "eyes close" cutscenes.
 * Manages the active eyes close overlay and handles completion callbacks.
 * Also handles hiding HUD and blocking player input during the cutscene.
 */
public class EyesCloseManager {
    
    private static EyesCloseManager instance;
    
    private final EyesCloseOverlay overlay;
    private boolean blockPlayerInput = false;
    
    // Track if we've already sent completion packet for current cutscene (prevent duplicate sends)
    private boolean cutsceneCompletePacketSent = false;
    
    // Track original hideGui state to restore it after cutscene
    private Boolean originalHideGui = null;
    
    private EyesCloseManager() {
        this.overlay = new EyesCloseOverlay();
    }
    
    /**
     * Gets the singleton instance of EyesCloseManager.
     * 
     * @return The singleton instance
     */
    public static EyesCloseManager getInstance() {
        if (instance == null) {
            instance = new EyesCloseManager();
        }
        return instance;
    }
    
    /**
     * Starts a new eyes close cutscene.
     * 
     * @param closeDurationTicks Number of ticks for eyes closing animation
     * @param blackDurationTicks Number of ticks to stay fully black
     * @param openDurationTicks Number of ticks for eyes opening animation
     * @param blockPlayerInput Whether to block player input during the cutscene
     */
    public void startEyesClose(int closeDurationTicks, int blackDurationTicks, int openDurationTicks, boolean blockPlayerInput) {
        Marallyzen.LOGGER.info("[EyesCloseManager] startEyesClose - close={}t, black={}t, open={}t, blockInput={}", 
                closeDurationTicks, blackDurationTicks, openDurationTicks, blockPlayerInput);
        
        // Reset completion flag when new cutscene starts
        cutsceneCompletePacketSent = false;
        this.blockPlayerInput = blockPlayerInput;
        
        // Close any open dialogs before starting cutscene
        closeDialogs();
        
        // Hide HUD and narration overlay during cutscene
        hideHudAndNarration();
        
        overlay.start(closeDurationTicks, blackDurationTicks, openDurationTicks);
    }
    
    /**
     * Closes any open dialogs before starting cutscene.
     */
    private void closeDialogs() {
        var dialogHud = neutka.marallys.marallyzen.client.gui.DialogHud.getInstance();
        if (dialogHud.isVisible()) {
            Marallyzen.LOGGER.info("[EyesCloseManager] Closing dialog before eyes close cutscene");
            var stateMachine = neutka.marallys.marallyzen.client.gui.DialogStateMachine.getInstance();
            stateMachine.transitionTo(neutka.marallys.marallyzen.client.gui.DialogState.CLOSED);
        }
    }
    
    /**
     * Hides HUD (F1 mode) and narration overlay during cutscene.
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
            Marallyzen.LOGGER.info("[EyesCloseManager] Hiding HUD (F1 mode enabled)");
        }
        
        // Clear narration overlay
        var narrationManager = neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance();
        narrationManager.clearNarration();
        narrationManager.clearProximity();
        Marallyzen.LOGGER.info("[EyesCloseManager] Cleared narration overlay");
    }
    
    /**
     * Restores HUD visibility after cutscene.
     */
    private void restoreHudAndNarration() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && originalHideGui != null) {
            // Restore original hideGui state
            mc.options.hideGui = originalHideGui;
            originalHideGui = null;
            Marallyzen.LOGGER.info("[EyesCloseManager] Restored HUD visibility (F1 mode disabled)");
        }
    }
    
    /**
     * Clears the active eyes close overlay immediately.
     */
    public void clearEyesClose() {
        Marallyzen.LOGGER.info("[EyesCloseManager] clearEyesClose");
        overlay.clear();
        blockPlayerInput = false;
        cutsceneCompletePacketSent = false;
        
        // Restore HUD visibility
        restoreHudAndNarration();
    }
    
    /**
     * Gets the active eyes close overlay.
     * 
     * @return The active overlay, or null if no cutscene is active
     */
    public EyesCloseOverlay getActive() {
        return overlay.isVisible() ? overlay : null;
    }
    
    /**
     * Checks if an eyes close cutscene is currently active.
     * 
     * @return true if cutscene is active
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
        EyesCloseOverlay.State previousState = overlay.getState();
        overlay.tick();
        EyesCloseOverlay.State currentState = overlay.getState();
        
        // Track when cutscene ends (transitions to HIDDEN)
        if (previousState != EyesCloseOverlay.State.HIDDEN && currentState == EyesCloseOverlay.State.HIDDEN) {
            // Cutscene just completed - notify server
            if (Minecraft.getInstance().level != null && !cutsceneCompletePacketSent) {
                cutsceneCompletePacketSent = true;
                blockPlayerInput = false;
                Marallyzen.LOGGER.info("[EyesCloseManager] Eyes close cutscene completed (state=HIDDEN), sending EyesCloseCompletePacket to server");
                
                // Restore HUD visibility
                restoreHudAndNarration();
                
                // Send packet to server to notify that cutscene is complete
                NetworkHelper.sendToServer(new EyesCloseCompletePacket());
            }
        }
    }
}

































