package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C packet: Triggers an "eyes close" cutscene on the client.
 * Sent from server when eyes close cutscene should be played.
 */
public record EyesClosePacket(
        int closeDurationTicks,
        int blackDurationTicks,
        int openDurationTicks,
        boolean blockPlayerInput
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EyesClosePacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("eyes_close"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, EyesClosePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.closeDurationTicks());
                buf.writeInt(packet.blackDurationTicks());
                buf.writeInt(packet.openDurationTicks());
                buf.writeBoolean(packet.blockPlayerInput());
            },
            buf -> new EyesClosePacket(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public CustomPacketPayload.Type<EyesClosePacket> type() {
        return TYPE;
    }

    public static void handle(EyesClosePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[EyesClosePacket] CLIENT: Starting eyes close cutscene - close={}t, black={}t, open={}t, blockInput={}", 
                        packet.closeDurationTicks(), packet.blackDurationTicks(), packet.openDurationTicks(), packet.blockPlayerInput());
                
                // Start eyes close cutscene on client
                neutka.marallys.marallyzen.client.cutscene.EyesCloseManager manager = 
                        neutka.marallys.marallyzen.client.cutscene.EyesCloseManager.getInstance();
                
                manager.startEyesClose(
                        packet.closeDurationTicks(),
                        packet.blackDurationTicks(),
                        packet.openDurationTicks(),
                        packet.blockPlayerInput()
                );
            }
        });
    }
}

































