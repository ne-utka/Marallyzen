package neutka.marallys.marallyzen.client.lever;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.LeverQteInputPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class LeverQteInputHandler {
    private LeverQteInputHandler() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!LeverQteClient.isActive()) {
            return;
        }
        if (event.getAction() != 1) {
            event.setCanceled(true);
            return;
        }
        int button = event.getButton();
        if (button == 0 || button == 1) {
            NetworkHelper.sendToServer(new LeverQteInputPacket(button == 1));
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        if (LeverQteClient.isActive()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (LeverQteClient.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!LeverQteClient.isActive()) {
            return;
        }
        if (event.getNewScreen() != null) {
            event.setCanceled(true);
        }
    }
}
