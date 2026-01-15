package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.chain.InteractiveChainPoseManager;
import com.mojang.blaze3d.vertex.PoseStack;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class FpvEventHandler {

    private static boolean lastInterpreterActive = false;

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (neutka.marallys.marallyzen.replay.playback.ReplayPlayer.isReplayActive()) {
            return;
        }

        var cameraController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
        boolean appliedPreview = false;
        if (neutka.marallys.marallyzen.client.cutscene.editor.CutscenePreviewPlayer.isPreviewActive()) {
            float time = neutka.marallys.marallyzen.client.cutscene.editor.CutscenePreviewPlayer.getActivePreviewTime()
                + (float) event.getPartialTick();
            appliedPreview = neutka.marallys.marallyzen.client.cutscene.editor.CutscenePreviewPlayer.applyPreviewCamera(time, cameraController);
        }
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

            InteractiveChainPoseManager.ChainPose chainPose =
                    InteractiveChainPoseManager.getPose(mc.player, Util.getMillis());
            if (chainPose != null) {
                headPitch += chainPose.chainAngle() * 0.4f * chainPose.fade();
                headRoll += chainPose.chainSideAngle() * 0.3f * chainPose.fade();
            }

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
        EmoteFpvInterpreter interpreter = EmoteFpvInterpreter.getInstance();
        if (interpreter.isActive()) {
            // Cancel vanilla hand rendering when emote is active
            // The arms will be rendered by the body render
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || !mc.options.getCameraType().isFirstPerson()) return;
        if (!MarallyzenFpvController.shouldApply(player)) return;

        EmoteFpvInterpreter interpreter = EmoteFpvInterpreter.getInstance();
        if (!interpreter.isActive()) return;

        // Render the player body
        interpreter.setRenderingFpvBody(true);
        
        try {
            PoseStack poseStack = event.getPoseStack();
            Camera camera = event.getCamera();
            var deltaTracker = event.getPartialTick();
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            
            Vec3 cameraPos = camera.getPosition();
            double renderX = Mth.lerp(partialTick, player.xo, player.getX()) - cameraPos.x;
            double renderY = Mth.lerp(partialTick, player.yo, player.getY()) - cameraPos.y;
            double renderZ = Mth.lerp(partialTick, player.zo, player.getZ()) - cameraPos.z;

            poseStack.pushPose();
            // Translate to player position
            poseStack.translate(renderX, renderY, renderZ);
            
            // Render
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            
            // We need to ensure we don't crash if we are already rendering
            dispatcher.render(player, 0, 0, 0, 0, partialTick, poseStack, bufferSource, dispatcher.getPackedLightCoords(player, partialTick));
            
            // Flush buffers to ensure transparency works correctly
            bufferSource.endBatch();
            
            poseStack.popPose();
            
        } catch (Exception e) {
            Marallyzen.LOGGER.error("Failed to render FPV body", e);
        } finally {
            interpreter.setRenderingFpvBody(false);
        }
    }
    
    // Model Part Visibility Management
    
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (EmoteFpvInterpreter.getInstance().isRenderingFpvBody() && event.getEntity() == Minecraft.getInstance().player) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.head.visible = false;
            model.hat.visible = false;
            // Should we hide other parts? Usually not.
        }
    }
    
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (EmoteFpvInterpreter.getInstance().isRenderingFpvBody() && event.getEntity() == Minecraft.getInstance().player) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.head.visible = true;
            model.hat.visible = true;
        }
    }
}
