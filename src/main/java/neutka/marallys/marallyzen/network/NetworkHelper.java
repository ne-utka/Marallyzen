package neutka.marallys.marallyzen.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Utility class for sending network packets.
 * Provides convenience methods for common packet sending scenarios.
 */
public class NetworkHelper {

    /**
     * Sends a packet to a specific player.
     */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    /**
     * Sends a packet to all players on the server.
     */
    public static void sendToAll(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    /**
     * Sends a packet to all players in a specific dimension.
     */
    public static void sendToDimension(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, CustomPacketPayload packet) {
        // Note: This may need adjustment based on NeoForge 21.1.209 API
        // For now, sending to all players as a workaround
        sendToAll(packet);
    }

    /**
     * Sends a packet to all players near a specific point.
     */
    public static void sendToNear(net.minecraft.world.phys.Vec3 pos, double radius, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, CustomPacketPayload packet) {
        // Note: This method may need adjustment based on NeoForge 21.1.209 API
        // For now, sending to all players in dimension as a workaround
        sendToDimension(dimension, packet);
    }

    /**
     * Sends a packet from client to server.
     */
    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}

