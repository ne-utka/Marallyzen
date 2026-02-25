package neutka.marallys.marallyzen.trigger.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class TriggerScreenShakeHandler {
    private TriggerScreenShakeHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        TriggerScreenShakeManager.getInstance().tick();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        TriggerScreenShakeManager.getInstance().apply(event);
    }
}
