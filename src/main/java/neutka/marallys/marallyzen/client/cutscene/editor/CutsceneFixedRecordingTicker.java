package neutka.marallys.marallyzen.client.cutscene.editor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CutsceneFixedRecordingTicker {
    private CutsceneFixedRecordingTicker() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        CutsceneEditorScreen screen = CutsceneEditorScreen.getActiveFixedRecordingScreen();
        if (screen != null) {
            screen.tickFixedRecordingBackground();
        }
    }
}
