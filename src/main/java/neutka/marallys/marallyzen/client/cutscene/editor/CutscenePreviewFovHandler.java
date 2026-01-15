package neutka.marallys.marallyzen.client.cutscene.editor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class CutscenePreviewFovHandler {
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (neutka.marallys.marallyzen.replay.playback.ReplayPlayer.isReplayActive()) {
            return;
        }
        if (!CutscenePreviewPlayer.isPreviewActive()) {
            return;
        }
        var cameraController = neutka.marallys.marallyzen.client.camera.CameraManager.getInstance().getCameraController();
        if (!cameraController.isActive()) {
            return;
        }
        event.setFOV(cameraController.getFov());
    }
}
