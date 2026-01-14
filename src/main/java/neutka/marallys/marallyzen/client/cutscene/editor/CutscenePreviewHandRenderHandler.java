package neutka.marallys.marallyzen.client.cutscene.editor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CutscenePreviewHandRenderHandler {
    private CutscenePreviewHandRenderHandler() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!CutscenePreviewPlayer.isPreviewActive() || !CutscenePreviewPlayer.shouldHideHandInPreview()) {
            return;
        }
        event.setCanceled(true);
    }
}
