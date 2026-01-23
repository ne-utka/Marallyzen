package neutka.marallys.marallyzen.npc;

import net.minecraft.server.level.ServerLevel;
import neutka.marallys.marallyzen.Marallyzen;

public final class NpcWorldPolicy {
    private NpcWorldPolicy() {
    }

    public static boolean isInstanceLevel(ServerLevel level) {
        if (level == null) {
            return false;
        }
        var key = level.dimension().location();
        return Marallyzen.MODID.equals(key.getNamespace()) && key.getPath().startsWith("instance/");
    }

    public static boolean isPersistentLevel(ServerLevel level) {
        return !isInstanceLevel(level);
    }
}
