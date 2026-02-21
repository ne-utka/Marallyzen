package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.lever.LeverInteractionClient;
import neutka.marallys.marallyzen.client.valve.ValveInteractionClient;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class FpvEventHandler {

    private static boolean lastInterpreterActive;
    private static final FpvQteController QTE = new FpvQteController();

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        forceFirstPersonIfActive();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            QTE.tick(null, false);
            return;
        }
        boolean active = MarallyzenFpvController.shouldApply(mc.player);
        QTE.tick(MarallyzenRenderContext.getCurrentEmoteId(), active);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        forceFirstPersonIfActive();

        var cameraController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
        if (cameraController.isActive()) {
            event.setYaw(cameraController.getYaw());
            event.setPitch(cameraController.getPitch());
            event.setRoll(0.0f);
            return;
        }

        if (!MarallyzenFpvController.shouldApply(mc.player)) {
            EmoteFpvInterpreter.getInstance().forceInactive();
            lastInterpreterActive = false;
            return;
        }

        float baseYaw = mc.player.getViewYRot((float) event.getPartialTick());
        float basePitch = mc.player.getViewXRot((float) event.getPartialTick());
        EmoteFpvInterpreter interpreter = EmoteFpvInterpreter.getInstance();
        interpreter.update(mc.player);
        boolean interpreterActive = interpreter.isActive();
        FpvQteState.Snapshot qte = FpvQteState.snapshot();
        boolean qteActive = qte.active() || qte.finishing();
        if (!interpreterActive && !qteActive) {
            lastInterpreterActive = false;
            return;
        }

        if (!interpreterActive || !MarallyzenRenderContext.isHeadMovementEnabled()) {
            event.setYaw(baseYaw);
            event.setPitch(basePitch);
            event.setRoll(0.0f);
            lastInterpreterActive = false;
            return;
        }

        lastInterpreterActive = true;
        float alpha = Mth.clamp((float) event.getPartialTick(), 0f, 1f);
        float headPitch = interpreter.getHeadPitch(alpha);
        float headYaw = interpreter.getHeadYaw(alpha);
        float headRoll = interpreter.getHeadRoll(alpha);

        float pitchRad = (float) Math.toRadians(basePitch);
        float pitchScale = 1.0f - Math.abs(pitchRad) * 0.5f;
        pitchScale = Mth.clamp(pitchScale, 0.3f, 1.0f);

        event.setYaw(baseYaw + (float) Math.toDegrees(headYaw));
        event.setPitch(basePitch + (float) Math.toDegrees(headPitch) * pitchScale);
        event.setRoll((float) Math.toDegrees(headRoll));
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (!MarallyzenFpvController.shouldApply(mc.player)) {
            return;
        }

        EmoteFpvInterpreter interpreter = EmoteFpvInterpreter.getInstance();
        var ctxId = MarallyzenRenderContext.getCurrentEmoteId();
        boolean valveDone = ctxId != null && "valve_done".equalsIgnoreCase(ctxId.getPath());
        if (!shouldRenderFpv(interpreter.isActive(), valveDone)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterEntities event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (!MarallyzenFpvController.shouldApply(mc.player)) {
            return;
        }
        EmoteFpvInterpreter interpreter = EmoteFpvInterpreter.getInstance();
        var ctxId = MarallyzenRenderContext.getCurrentEmoteId();
        boolean valveDone = ctxId != null && "valve_done".equalsIgnoreCase(ctxId.getPath());
        if (!shouldRenderFpv(interpreter.isActive(), valveDone)) {
            return;
        }

        interpreter.setRenderingFpvBody(true);
        try {
            float t = (float) (net.minecraft.util.Util.getMillis() * 0.001);
            FpvPhaseController.Sample sample = QTE.sample(t);
            FpvArmsRenderer.renderWorld(event, mc.player, interpreter, sample);
        } finally {
            interpreter.setRenderingFpvBody(false);
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

    private static boolean shouldRenderFpv(boolean interpreterActive, boolean valveDone) {
        if (interpreterActive || valveDone) {
            return true;
        }
        FpvQteState.Snapshot qte = FpvQteState.snapshot();
        return qte.active() || qte.finishing();
    }
}
