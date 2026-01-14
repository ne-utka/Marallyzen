package neutka.marallys.marallyzen.denizen.objects;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Collections;
import java.util.List;

final class MarallyzenServerLookup {
    private MarallyzenServerLookup() {
    }

    static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    static ServerPlayer getPlayer(String nameOrUuid) {
        MinecraftServer server = getServer();
        if (server == null) {
            return null;
        }
        ServerPlayer byUuid = null;
        try {
            byUuid = server.getPlayerList().getPlayer(java.util.UUID.fromString(nameOrUuid));
        }
        catch (IllegalArgumentException ignored) {
        }
        if (byUuid != null) {
            return byUuid;
        }
        return server.getPlayerList().getPlayerByName(nameOrUuid);
    }

    static List<ServerPlayer> getOnlinePlayers() {
        MinecraftServer server = getServer();
        if (server == null) {
            return Collections.emptyList();
        }
        return server.getPlayerList().getPlayers();
    }
}
