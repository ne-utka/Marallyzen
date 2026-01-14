package neutka.marallys.marallyzen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class MarallyzenClientEvents {
    private MarallyzenClientEvents() {}

    @SubscribeEvent
    static void onClientTickPre(ClientTickEvent.Pre event) {
        neutka.marallys.marallyzen.client.ClientPosterManager.tick();
    }
}
