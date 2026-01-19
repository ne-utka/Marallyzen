package neutka.marallys.marallyzen.replay.camera;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ReplayCameraFovHandler {
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!ReplayCameraDirector.getInstance().isActive()) {
            return;
        }
        if (neutka.marallys.marallyzen.replay.playback.ReplayPlayer.isReplayActive()) {
            return;
        }
        if (neutka.marallys.marallyzen.client.cutscene.editor.CutscenePreviewPlayer.isPreviewActive()) {
            return;
        }
        var cameraController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
        if (!cameraController.isActive()) {
            return;
        }
        event.setFOV(cameraController.getFov());
    }
}
