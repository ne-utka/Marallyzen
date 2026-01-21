package neutka.marallys.marallyzen.client.director;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.replay.ReplayCompat;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class DirectorOverlayEvents {
    private DirectorOverlayEvents() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ReplayCompat.isReplayActive()) {
            return;
        }
        if (!DirectorOverlayHud.isVisible()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        DirectorOverlayHud.render(event.getGuiGraphics(), 0, 0, 0.0f);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!ReplayCompat.isReplayActive()) {
            return;
        }
        if (!DirectorOverlayHud.isVisible()) {
            return;
        }
        if (event.getAction() != 1) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int mouseX = DirectorOverlayHud.getScaledMouseX(mc);
        int mouseY = DirectorOverlayHud.getScaledMouseY(mc);
        if (DirectorOverlayHud.mouseClicked(mouseX, mouseY, event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!ReplayCompat.isReplayActive()) {
            return;
        }
        if (!DirectorOverlayHud.isVisible()) {
            return;
        }
        if (DirectorOverlayHud.handleMouseScroll(event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }
}
