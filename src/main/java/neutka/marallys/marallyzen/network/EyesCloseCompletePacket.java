package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.Marallyzen;

/**
 * C2S packet: Notifies the server that an eyes close cutscene has completed on the client.
 * Sent when the cutscene finishes (eyes fully opened again).
 */
public record EyesCloseCompletePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EyesCloseCompletePacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("eyes_close_complete"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, EyesCloseCompletePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                // No data to write
            },
            buf -> new EyesCloseCompletePacket()
    );

    @Override
    public CustomPacketPayload.Type<EyesCloseCompletePacket> type() {
        return TYPE;
    }

    public static void handle(EyesCloseCompletePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) {
                Marallyzen.LOGGER.info("[EyesCloseCompletePacket] SERVER: Eyes close cutscene completed for player {}", 
                        player.getName().getString());
                
                // Notify NpcNarrateHandler to execute any pending callback
                neutka.marallys.marallyzen.npc.NpcNarrateHandler.onEyesCloseComplete(player.getUUID());
            }
        });
    }
}

