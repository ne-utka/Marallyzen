package neutka.marallys.marallyzen.replay;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = neutka.marallys.marallyzen.Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ReplayStartQueueTicker {
    private static boolean loggedPending;

    private ReplayStartQueueTicker() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (ReplayStartQueue.hasPending()) {
            if (!loggedPending) {
                loggedPending = true;
                var mc = net.minecraft.client.Minecraft.getInstance();
                String screenName = mc.screen == null ? "null" : mc.screen.getClass().getName();
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
                    "ReplayStartQueueTicker: tick while pending (screen={})",
                    screenName
                );
            }
        } else {
            loggedPending = false;
        }
        ReplayStartQueue.tick();
    }
}
