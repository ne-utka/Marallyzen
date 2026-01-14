package neutka.marallys.marallyzen.client.chain;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * Blocks LMB/RMB interactions while the player is hanging on a chain.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class InteractiveChainInputBlocker {
    private InteractiveChainInputBlocker() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!InteractiveChainClientAnimator.isSwinging()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.screen != null) {
            return;
        }
        if (event.getButton() == 0 || event.getButton() == 1) {
            boolean pressed = event.getAction() == 1;
            boolean left = event.getButton() == 0;
            if (left) {
                InteractiveChainClientAnimator.setMouseLeftDown(pressed);
            } else {
                InteractiveChainClientAnimator.setMouseRightDown(pressed);
            }
            event.setCanceled(true);
        }
    }
}
