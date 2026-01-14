package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C packet: Clears the proximity overlay.
 * Sent from server when player leaves NPC proximity.
 */
public record ClearProximityPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClearProximityPacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("clear_proximity"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearProximityPacket> STREAM_CODEC = 
            StreamCodec.unit(new ClearProximityPacket());

    @Override
    public CustomPacketPayload.Type<ClearProximityPacket> type() {
        return TYPE;
    }

    public static void handle(ClearProximityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[ClearProximityPacket] CLIENT: Clearing proximity overlay");
                // Clear proximity overlay on client
                neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance()
                        .clearProximity();
            }
        });
    }
}

