package neutka.marallys.marallyzen.trigger.event;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

public final class AdminHudNotifier {
    private static final int COLOR_GREEN = 0x55FFAA;
    private static final int COLOR_PURPLE = 0xCC66FF;
    private static final int COLOR_RED = 0xFF5555;
    private static final int COLOR_BLUE = 0x55AAFF;

    private AdminHudNotifier() {
    }

    public static void firstPoint(ServerPlayer player) {
        sendActionBar(player, "Первая точка выделена", COLOR_GREEN);
    }

    public static void secondPoint(ServerPlayer player, int x, int y, int z) {
        sendActionBar(player, "Область выделена (" + x + "x" + y + "x" + z + ")", COLOR_GREEN);
    }

    public static void saved(ServerPlayer player) {
        sendActionBar(player, "Область сохранена!", COLOR_PURPLE);
    }

    public static void removed(ServerPlayer player) {
        sendActionBar(player, "Область удалена", COLOR_RED);
    }

    public static void bound(ServerPlayer player) {
        sendActionBar(player, "Триггер привязан", COLOR_BLUE);
    }

    public static void doorSaved(ServerPlayer player) {
        sendActionBar(player, "Область двери сохранена!", COLOR_GREEN);
    }

    private static void sendActionBar(ServerPlayer player, String text, int rgb) {
        if (player == null || text == null || text.isBlank()) {
            return;
        }
        Component component = Component.literal(text)
                .withStyle(style -> style.withColor(TextColor.fromRgb(rgb)));
        player.displayClientMessage(component, true);
    }
}
