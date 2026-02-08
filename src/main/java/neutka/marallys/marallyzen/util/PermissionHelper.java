package neutka.marallys.marallyzen.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public final class PermissionHelper {
    private PermissionHelper() {
    }

    public static boolean hasPermission(ServerPlayer player, String permission) {
        if (player == null || permission == null || permission.isEmpty()) {
            return false;
        }
        return isOp(player);
    }

    public static boolean isOp(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        var server = player.level().getServer();
        if (server == null) {
            return false;
        }
        return server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));
    }
}
