package neutka.marallys.marallyzen.trigger.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class TriggerAnimationRenderer {
    private TriggerAnimationRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        TriggerAnimationClient.getInstance().tick();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterEntities event) {
        float partialTick = net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        TriggerAnimationClient.getInstance().render(event.getPoseStack(), partialTick);
    }
}
