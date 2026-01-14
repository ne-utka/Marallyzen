package neutka.marallys.marallyzen.util;

import net.minecraft.server.level.ServerPlayer;

public final class PermissionHelper {
    private PermissionHelper() {
    }

    public static boolean hasPermission(ServerPlayer player, String permission) {
        if (player == null || permission == null || permission.isEmpty()) {
            return false;
        }
        return player.hasPermissions(2);
    }
}
