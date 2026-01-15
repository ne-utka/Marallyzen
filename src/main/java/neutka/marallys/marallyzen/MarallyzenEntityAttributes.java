package neutka.marallys.marallyzen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class MarallyzenEntityAttributes {
    private MarallyzenEntityAttributes() {
    }

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(Marallyzen.GECKO_NPC.get(), GeckoNpcEntity.createAttributes().build());
    }
}
