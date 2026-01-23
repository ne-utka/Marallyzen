package neutka.marallys.marallyzen.instance;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class InstanceCloneHandler {
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }
        InstanceSessionManager manager = InstanceSessionManager.getInstance();
        if (!manager.isPendingInstanceRespawn(oldPlayer)) {
            return;
        }
        manager.carryPendingInstanceRespawn(oldPlayer, newPlayer);
        Marallyzen.LOGGER.info(
                "[InstanceClone] pending respawn carried over player={}",
                newPlayer.getGameProfile().getName()
        );
    }
}
