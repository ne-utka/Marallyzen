package neutka.marallys.marallyzen.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import neutka.marallys.marallyzen.Marallyzen;

/**
 * Handles rendering and input for the dialog HUD.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class DialogHudRenderer {
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        DialogHud.getInstance().tick();
    }
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Render after particles so prompts are late in the world render order.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            
            Camera camera = event.getCamera();
            PoseStack poseStack = event.getPoseStack();
            // Get partial tick from Minecraft timer
            float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
            
            DialogHud.getInstance().renderInWorld(poseStack, camera, partialTick);
        }
    }
    
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        // Don't handle scroll here - DialogInputController handles it with proper state checking
        // This handler was causing conflicts with DialogInputController
    }
    
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        // Don't handle mouse button here - DialogInputController handles it with proper state checking
        // This handler was causing conflicts with DialogInputController
    }
}
