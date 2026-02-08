package neutka.marallys.marallyzen.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

final class ClientOnly {
    private ClientOnly() {}

    static boolean isClient(IPayloadContext context) {
        return context != null
            && context.player() != null
            && context.player().level().isClientSide();
    }

    static boolean isClient() {
        try {
            Class.forName("net.minecraft.client.Minecraft", false, ClientOnly.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

