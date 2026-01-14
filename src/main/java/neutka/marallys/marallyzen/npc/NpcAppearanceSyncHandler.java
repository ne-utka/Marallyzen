package neutka.marallys.marallyzen.npc;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.NpcAppearancePacket;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class NpcAppearanceSyncHandler {
    private NpcAppearanceSyncHandler() {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof NpcEntity npcEntity)) {
            return;
        }
        String npcId = npcEntity.getNpcId();
        if (npcId == null || npcId.isEmpty()) {
            return;
        }
        NpcData npcData = NpcClickHandler.getRegistry().getNpcData(npcId);
        if (npcData == null) {
            return;
        }
        NetworkHelper.sendToPlayer(player, new NpcAppearancePacket(
                npcId,
                safe(npcData.getSkinTexture()),
                safe(npcData.getSkinSignature()),
                safe(npcData.getSkinModel())
        ));
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
