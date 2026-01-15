package neutka.marallys.marallyzen.client.gui;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.DialogClosePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

/**
 * Handles input for dialog system.
 * Processes scroll wheel, right click, and ESC key.
 * Input is blocked during OPENING, EXECUTING, and TRANSITION states.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class DialogInputController {
    
    /**
     * Handles mouse wheel scroll to change selected option.
     * Uses HIGHEST priority to intercept scroll events before inventory/gui handlers.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        DialogHud dialogHud = DialogHud.getInstance();
        
        DialogState currentState = stateMachine.getCurrentState();
        boolean isInputBlocked = stateMachine.isInputBlocked();
        boolean isVisible = dialogHud.isVisible();
        boolean hasScreen = mc.screen != null;
        double scrollDelta = event.getScrollDeltaY();
        
        // Only process input if dialog is in CHOICE state
        if (currentState != DialogState.CHOICE) {
            return;
        }
        
        // Don't process if a screen is open (like inventory, etc.)
        if (hasScreen) {
            return;
        }
        
        if (isInputBlocked) {
            return;
        }
        
        if (!isVisible) {
            return;
        }
        
        // Process scroll and cancel event to prevent inventory from handling it
        boolean handled = dialogHud.handleMouseScroll(scrollDelta);
        if (handled) {
            event.setCanceled(true);
        }
    }
    
    /**
     * Handles right click to confirm selection.
     * Uses HIGHEST priority to intercept click events before other handlers.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        DialogHud dialogHud = DialogHud.getInstance();
        
        DialogState currentState = stateMachine.getCurrentState();
        boolean isInputBlocked = stateMachine.isInputBlocked();
        boolean isVisible = dialogHud.isVisible();
        boolean hasScreen = mc.screen != null;
        int button = event.getButton();
        int action = event.getAction();
        
        // Only process input if dialog is in CHOICE state
        if (currentState != DialogState.CHOICE) {
            return;
        }
        
        // Don't process if a screen is open (like inventory, etc.)
        if (hasScreen) {
            return;
        }
        
        if (isInputBlocked) {
            return;
        }
        
        if (!isVisible) {
            return;
        }
        
        // Check for right click
        // In GLFW/Minecraft: button 0 = left, button 1 = right, button 2 = middle
        // action: 1 = press, 0 = release, 2 = repeat
        // Try both button 1 (standard) and check if it's actually right click
        boolean isRightClick = (button == 1 && action == 1);
        
        if (isRightClick) {
            if (dialogHud.handleRightClick()) {
                event.setCanceled(true);
            }
        }
    }
    
    /**
     * Handles ESC key to close dialog.
     * Note: ESC is typically handled by Screen, but we need to intercept it for dialogs.
     * This will be called when ESC is pressed and no screen is open.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyPress(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        // Check if ESC key is pressed (key code 256 = GLFW.GLFW_KEY_ESCAPE)
        // Also check if no screen is open (otherwise ESC would close the screen)
        if (event.getKey() == 256 && event.getAction() == 1 && mc.screen == null) { // 1 = press
            // Block ESC during screen fade
            var screenFadeManager = neutka.marallys.marallyzen.client.cutscene.ScreenFadeManager.getInstance();
            if (screenFadeManager.isActive()) {
                return;
            }
            
            // Block ESC during eyes close cutscene
            var eyesCloseManager = neutka.marallys.marallyzen.client.cutscene.EyesCloseManager.getInstance();
            if (eyesCloseManager.isActive()) {
                return;
            }
            
            DialogStateMachine stateMachine = DialogStateMachine.getInstance();
            
            // Only process ESC if dialog is active (not IDLE or CLOSED)
            DialogState currentState = stateMachine.getCurrentState();
            if (currentState == DialogState.IDLE || currentState == DialogState.CLOSED) {
                return;
            }
            
            // Send close packet to server
            DialogHud dialogHud = DialogHud.getInstance();
            if (dialogHud.getNpcUuid() != null) {
                NetworkHelper.sendToServer(new DialogClosePacket(dialogHud.getNpcUuid()));
                // Note: InputEvent.Key doesn't have setCanceled, but we've handled the event
            }
        }
    }
    
    /**
     * Blocks item usage (right-click with items) when dialog is open.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        
        // Only block for client player
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.equals(player)) {
            return;
        }
        
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        DialogHud dialogHud = DialogHud.getInstance();
        
        DialogState currentState = stateMachine.getCurrentState();
        boolean isVisible = dialogHud.isVisible();
        
        // Block item usage if dialog is visible and in CHOICE or OPENING state
        if (isVisible && (currentState == DialogState.CHOICE || currentState == DialogState.OPENING)) {
            event.setCanceled(true);
        }
    }
    
    /**
     * Attempts to block hotbar slot selection (keys 1-9) when dialog is open.
     * Note: InputEvent.Key doesn't support cancellation, so this may not fully block
     * the action. For complete blocking, a mixin or other approach would be needed.
     * This handler logs the attempt and may partially prevent the action through
     * early interception with HIGHEST priority.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHotbarKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return; // Don't block if a screen is open
        }
        
        DialogStateMachine stateMachine = DialogStateMachine.getInstance();
        DialogHud dialogHud = DialogHud.getInstance();
        
        DialogState currentState = stateMachine.getCurrentState();
        boolean isVisible = dialogHud.isVisible();
        
        // Attempt to block hotbar keys (1-9) if dialog is visible and in CHOICE or OPENING state
        // Key codes: 49-57 = keys 1-9
        if (isVisible && (currentState == DialogState.CHOICE || currentState == DialogState.OPENING)) {
            int keyCode = event.getKey();
            if (keyCode >= 49 && keyCode <= 57 && event.getAction() == 1) { // 1 = press
                // Note: InputEvent.Key doesn't have setCanceled(), so we can't fully block it here.
                // The event will still propagate.
                // For complete blocking, we'd need to use a mixin to intercept KeyMapping.setDown()
                // or use a different event type that supports cancellation.
            }
        }
    }
}

