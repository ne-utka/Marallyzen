package neutka.marallys.marallyzen.client.quest;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.QuestZoneTeleportRequestPacket;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class QuestZonePromptInputHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.screen != null) {
            return;
        }
        if (event.getButton() != 1 || event.getAction() != 1) {
            return;
        }
        String zoneId = QuestZonePromptHud.getInstance().activeZoneId();
        if (zoneId == null) {
            return;
        }
        NetworkHelper.sendToServer(new QuestZoneTeleportRequestPacket(zoneId));
        event.setCanceled(true);
    }
}
