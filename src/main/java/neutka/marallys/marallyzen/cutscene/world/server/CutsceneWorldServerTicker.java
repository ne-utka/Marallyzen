package neutka.marallys.marallyzen.cutscene.world.server;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CutsceneWorldServerTicker {
    private CutsceneWorldServerTicker() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() == null) {
            return;
        }
        event.getServer().getAllLevels().forEach(CutsceneWorldRecorder::tick);
    }
}
