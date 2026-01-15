package neutka.marallys.marallyzen.npc;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class WildfireNpcSyncHandler {
    private WildfireNpcSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        WildfireNpcIntegration.syncAllToPlayer(player, NpcClickHandler.getRegistry());
        NpcClickHandler.getRegistry().sendPlayerInfoTo(player);
    }
}
