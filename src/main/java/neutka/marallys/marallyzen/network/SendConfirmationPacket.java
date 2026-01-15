package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S packet: Confirmation packet sent from client to server on connection.
 * Similar to Clientizen's SendConfirmationPacketOut.
 */
public class SendConfirmationPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SendConfirmationPacket> TYPE = new CustomPacketPayload.Type<>(MarallyzenNetwork.id("confirmation"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, SendConfirmationPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.unit(new SendConfirmationPacket());

    @Override
    public CustomPacketPayload.Type<SendConfirmationPacket> type() {
        return TYPE;
    }

    public static void handle(SendConfirmationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client confirmed connection - server can now send scripts/GUI if needed
            neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("SendConfirmationPacket received from client");
        });
    }
}

