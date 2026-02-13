package neutka.marallys.marallyzen.goals;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Hooks goal script reload into datapack reload cycle.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public final class GoalReloadEvents {
    private GoalReloadEvents() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        if (event.getPlayerList() == null) {
            return;
        }
        GoalProgressEngine.getInstance().reload(event.getPlayerList().getServer());
    }
}
