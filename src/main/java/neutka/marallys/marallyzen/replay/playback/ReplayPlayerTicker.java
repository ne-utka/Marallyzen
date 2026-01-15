package neutka.marallys.marallyzen.replay.playback;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ReplayPlayerTicker {
    private ReplayPlayerTicker() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ReplayPlayer.getInstance().tick();
    }
}
