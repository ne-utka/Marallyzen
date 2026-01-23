package neutka.marallys.marallyzen.client.instance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.InstanceLeaveRequestPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class InstancePauseMenuButton {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!isIngameMenuScreen(screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!InstanceClientState.getInstance().isInInstance() && !isPlayerInInstanceDimension(mc)) {
            return;
        }

        int x = screen.width / 2 - BUTTON_WIDTH / 2;
        int y = findNextRowY(screen);

        Button leaveButton = Button.builder(
                Component.translatable("screen.marallyzen.instance_exit.leave"),
                btn -> {
                    NetworkHelper.sendToServer(new InstanceLeaveRequestPacket());
                    mc.setScreen(null);
                })
            .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();

        event.addListener(leaveButton);
    }

    private static int findNextRowY(Screen screen) {
        int maxY = screen.height / 4 + 96;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget) {
                maxY = Math.max(maxY, widget.getY());
            }
        }
        int next = maxY + 24;
        return Math.min(next, screen.height - 28);
    }

    private static boolean isIngameMenuScreen(Screen screen) {
        String name = screen.getClass().getName();
        return "net.minecraft.client.gui.screens.PauseScreen".equals(name)
                || "net.minecraft.client.gui.screens.IngameMenuScreen".equals(name);
    }

    private static boolean isPlayerInInstanceDimension(Minecraft mc) {
        if (mc == null || mc.level == null) {
            return false;
        }
        var key = mc.level.dimension().location();
        return Marallyzen.MODID.equals(key.getNamespace()) && key.getPath().startsWith("instance/");
    }
}
