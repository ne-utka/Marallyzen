package neutka.marallys.marallyzen.client.cutscene;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Blocks player movement and camera rotation during cutscenes if blockPlayerInput is enabled.
 * Supports both ScreenFade and EyesClose cutscenes.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class PlayerInputBlocker {
    
    /**
     * Blocks player movement input during cutscenes if blockPlayerInput is true.
     */
    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        boolean shouldBlock = false;
        
        // Check screen fade
        ScreenFadeManager screenFadeManager = ScreenFadeManager.getInstance();
        if (screenFadeManager.shouldBlockPlayerInput()) {
            shouldBlock = true;
        }
        
        // Check eyes close cutscene
        EyesCloseManager eyesCloseManager = EyesCloseManager.getInstance();
        if (eyesCloseManager.shouldBlockPlayerInput()) {
            shouldBlock = true;
        }
        
        // Check cutscene editor recording/preview
        // This is a simple check - in a real implementation, you'd want a proper manager
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof neutka.marallys.marallyzen.client.cutscene.editor.CutsceneEditorScreen editorScreen) {
            if (editorScreen.shouldBlockPlayerInput()) {
                shouldBlock = true;
            }
        }
        
        if (shouldBlock) {
            // Block all movement input
            event.getInput().forwardImpulse = 0.0f;
            event.getInput().leftImpulse = 0.0f;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
            event.getInput().up = false;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
        }
    }
    
    /**
     * Note: Camera rotation blocking is handled by Minecraft's input system.
     * We can't directly block mouse input here, but movement blocking should be sufficient
     * for most use cases. If full input blocking is needed, it would require mixins
     * or other advanced techniques.
     */
}
