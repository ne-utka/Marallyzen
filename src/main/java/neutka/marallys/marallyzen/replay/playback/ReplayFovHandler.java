package neutka.marallys.marallyzen.replay.playback;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ReplayFovHandler {
    private ReplayFovHandler() {
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        ReplayPlayer player = ReplayPlayer.getInstance();
        if (!player.isPlaying()) {
            return;
        }
        event.setFOV(player.getCurrentFov((float) event.getPartialTick()));
    }
}
