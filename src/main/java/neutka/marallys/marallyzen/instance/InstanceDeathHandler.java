package neutka.marallys.marallyzen.instance;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class InstanceDeathHandler {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InstanceSessionManager.getInstance().markInstanceDeath(player);
        Marallyzen.LOGGER.info(
                "[InstanceDeath] vanilla respawn suppressed flag set player={}",
                player.getGameProfile().getName()
        );
    }
}
