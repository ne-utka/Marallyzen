package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import com.mojang.blaze3d.vertex.PoseStack;
import neutka.marallys.marallyzen.client.lever.LeverInteractionClient;
import neutka.marallys.marallyzen.client.valve.ValveInteractionClient;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class FpvEventHandler {

    private static boolean lastInterpreterActive = false;

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        forceFirstPersonIfActive();
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        forceFirstPersonIfActive();
        var cameraController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
        boolean appliedPreview = false;
        if (cameraController.isActive() || appliedPreview) {
            event.setYaw(cameraController.getYaw());
            event.setPitch(cameraController.getPitch());
            event.setRoll(0.0f);
            return;
        }
        
        boolean shouldApply = MarallyzenFpvController.shouldApply(mc.player);
        if (!shouldApply) {
            EmoteFpvInterpreter.getInstance().forceInactive();
            if (lastInterpreterActive) {
                lastInterpreterActive = false;
            }
            return;
        }

        EmoteFpvInterpreter interpreter = EmoteFpvInterpreter.getInstance();
        interpreter.update(mc.player);

        if (interpreter.isActive()) {
            if (!lastInterpreterActive) {
                // Marallyzen.LOGGER.info("[FPV] FpvEventHandler.onComputeCameraAngles: FPV enabled, interpreter active");
                lastInterpreterActive = true;
            }
            
            double partialTick = event.getPartialTick();
            float alpha = Mth.clamp((float) partialTick, 0f, 1f);

            // Sample head offsets (radians) interpolated per-frame
            float headPitch = interpreter.getHeadPitch(alpha);
            float headYaw = interpreter.getHeadYaw(alpha);
            float headRoll = interpreter.getHeadRoll(alpha);

            // Interactive chain removed.

            // Base (vanilla) look angles
            float baseYaw = mc.player.getViewYRot((float) partialTick);
            float basePitch = mc.player.getViewXRot((float) partialTick);

            // Check if head movement is enabled
            boolean headMovementEnabled = MarallyzenRenderContext.isHeadMovementEnabled();
            
            if (headMovementEnabled) {
                // Scale emote influence based on current pitch to prevent excessive movement when looking up/down
                // When looking straight (pitch ~0), full influence. When looking up/down (pitch ~±90), reduce influence
                float pitchRad = (float) Math.toRadians(basePitch);
                float pitchScale = 1.0f - Math.abs(pitchRad) * 0.5f; // Reduce by up to 50% at extreme angles
                pitchScale = Mth.clamp(pitchScale, 0.3f, 1.0f); // Never go below 30% influence
                
                // Apply scaled emote offsets
                float yawDeg = baseYaw + (float) Math.toDegrees(headYaw);
                float pitchDeg = basePitch + (float) Math.toDegrees(headPitch) * pitchScale;
                float rollDeg = (float) Math.toDegrees(headRoll); // roll doesn't need scaling
                
                event.setYaw(yawDeg);
                event.setPitch(pitchDeg);
                event.setRoll(rollDeg);
            } else {
                // Head movement disabled - use vanilla angles only
                event.setYaw(baseYaw);
                event.setPitch(basePitch);
                event.setRoll(0.0f); // No roll when head movement is disabled
            }
        } else {
            if (lastInterpreterActive) {
                // Marallyzen.LOGGER.info("[FPV] FpvEventHandler.onComputeCameraAngles: Interpreter became inactive");
                lastInterpreterActive = false;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !MarallyzenFpvController.shouldApply(mc.player)) {
            return;
        }
        var ctxId = MarallyzenRenderContext.getCurrentEmoteId();
        boolean valveDone = ctxId != null && "valve_done".equalsIgnoreCase(ctxId.getPath());
        EmoteFpvInterpreter interpreter = EmoteFpvInterpreter.getInstance();
        if (interpreter.isActive() || valveDone) {
            // Cancel vanilla hand rendering when emote is active
            // The arms will be rendered by the body render
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterEntities event) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || !mc.options.getCameraType().isFirstPerson()) return;
        if (!MarallyzenFpvController.shouldApply(player)) return;

        EmoteFpvInterpreter interpreter = EmoteFpvInterpreter.getInstance();
        var ctxId = MarallyzenRenderContext.getCurrentEmoteId();
        boolean valveDone = ctxId != null && "valve_done".equalsIgnoreCase(ctxId.getPath());
        if (!interpreter.isActive()) {
            if (!valveDone) {
                return;
            }
        }

        // Custom FPV body rendering is currently disabled for 1.21.11 render pipeline changes.
    }
    
    // Model Part Visibility Management
    
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (EmoteFpvInterpreter.getInstance().isRenderingFpvBody()) {
            var model = event.getRenderer().getModel();
            if (model instanceof PlayerModel playerModel) {
                playerModel.head.visible = false;
                playerModel.hat.visible = false;
            }
        }
    }
    
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (EmoteFpvInterpreter.getInstance().isRenderingFpvBody()) {
            var model = event.getRenderer().getModel();
            if (model instanceof PlayerModel playerModel) {
                playerModel.head.visible = true;
                playerModel.hat.visible = true;
            }
        }
    }

    private static void forceFirstPersonIfActive() {
        if (!LeverInteractionClient.isBlockingInput() && !ValveInteractionClient.isBlockingInput()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }
}
