package neutka.marallys.marallyzen.npc;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Fake player connection implementation for fake player entities.
 * Based on Denizen's FakePlayerConnectionImpl but adapted for NeoForge.
 */
public class FakePlayerConnectionImpl extends ServerGamePacketListenerImpl {

    public FakePlayerConnectionImpl(MinecraftServer server, Connection networkManager, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, networkManager, player, cookie);
    }

    @Override
    public void send(Packet<?> packet) {
        // Do nothing - this is a fake connection, we don't send packets
    }
}



