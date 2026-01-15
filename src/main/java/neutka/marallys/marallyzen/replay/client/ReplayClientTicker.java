package neutka.marallys.marallyzen.replay.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ReplayClientTicker {
    private ReplayClientTicker() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ReplayClientRecorder.getInstance().tick();
    }
}
