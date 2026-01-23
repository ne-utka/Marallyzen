package neutka.marallys.marallyzen.instance;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class InstanceLoginHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InstanceSessionManager manager = InstanceSessionManager.getInstance();
        if (manager.restoreAfterLogin(player, "login_restore")) {
            return;
        }
        manager.clearRestrictions(player);
        manager.markOutside(player);
    }
}
