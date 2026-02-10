package neutka.marallys.marallyzen.client.fpv;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.lever.LeverQteClient;
import neutka.marallys.marallyzen.client.valve.ValveQteClient;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class FpvInputLockController {
    private FpvInputLockController() {
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        if (!isQteInputLocked()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }

        // Force FPV while QTE is active.
        mc.options.setCameraType(CameraType.FIRST_PERSON);

        // Block inventory/chat/pause and other GUI opens.
        if (mc.screen != null) {
            mc.setScreen(null);
        }

        releaseAllMappings(mc);
    }

    private static boolean isQteInputLocked() {
        return LeverQteClient.isActive() || ValveQteClient.isActive();
    }

    private static void releaseAllMappings(Minecraft mc) {
        for (KeyMapping mapping : mc.options.keyMappings) {
            mapping.setDown(false);
            // Drain queued clicks to avoid delayed actions after QTE end.
            while (mapping.consumeClick()) {
                // no-op
            }
        }
    }
}
