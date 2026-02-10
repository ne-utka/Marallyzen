package neutka.marallys.marallyzen.client.lever;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.fpv.FpvQteHudRenderer;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public final class LeverQteHudRenderer {
    private LeverQteHudRenderer() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (FpvQteHudRenderer.isFpvQteActive()) {
            return;
        }
        LeverQteHud.render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
