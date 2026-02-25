package neutka.marallys.marallyzen.trigger;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Binds runtime trigger module instances to active servers.
 */
public final class TriggerModuleRegistry {
    private static final Map<MinecraftServer, TriggerModule> MODULES = new WeakHashMap<>();

    private TriggerModuleRegistry() {
    }

    public static synchronized void bind(MinecraftServer server, TriggerModule module) {
        if (server == null || module == null) {
            return;
        }
        MODULES.put(server, module);
    }

    public static synchronized void unbind(MinecraftServer server) {
        if (server == null) {
            return;
        }
        MODULES.remove(server);
    }

    public static synchronized TriggerModule get(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        return MODULES.get(server);
    }
}
