package neutka.marallys.marallyzen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import neutka.marallys.marallyzen.denizen.storage.MarallyzenFlagStore;
import neutka.marallys.marallyzen.npc.NpcClickHandler;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class MarallyzenDenizenEvents {
    private static int npcStateSaveTicks;
    private static final int NPC_STATE_SAVE_INTERVAL = 200;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        DenizenService.tickServer();
        // Tick NPC AI
        NpcClickHandler.getRegistry().tickAIs();
        neutka.marallys.marallyzen.quest.QuestManager.getInstance().onServerTick();
        npcStateSaveTicks++;
        if (npcStateSaveTicks >= NPC_STATE_SAVE_INTERVAL) {
            npcStateSaveTicks = 0;
            // SavedData handles persistence; keep disabled list separate.
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MarallyzenFlagStore.saveAll();
        // SavedData handles persistence; keep disabled list separate.
        DenizenService.shutdown();
    }
}
