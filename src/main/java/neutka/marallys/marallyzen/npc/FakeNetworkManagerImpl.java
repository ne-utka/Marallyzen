package neutka.marallys.marallyzen.npc;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import neutka.marallys.marallyzen.Marallyzen;

import java.lang.reflect.Field;
import java.net.SocketAddress;

/**
 * Fake network manager implementation for fake player entities.
 * Based on Denizen's FakeNetworkManagerImpl but adapted for NeoForge.
 */
public class FakeNetworkManagerImpl extends Connection {

    private static Field channelField;
    private static Field addressField;

    static {
        try {
            // Use reflection to access private fields
            channelField = Connection.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            addressField = Connection.class.getDeclaredField("address");
            addressField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Marallyzen.LOGGER.error("Failed to find Connection fields for reflection", e);
        }
    }

    public FakeNetworkManagerImpl(PacketFlow direction) {
        super(direction);
        try {
            if (channelField != null) {
                channelField.set(this, new FakeChannelImpl(null));
            }
            if (addressField != null) {
                addressField.set(this, new SocketAddress() {
                    // Anonymous SocketAddress implementation
                });
            }
        } catch (IllegalAccessException e) {
            Marallyzen.LOGGER.error("Failed to set Connection fields via reflection", e);
        }
    }
}

