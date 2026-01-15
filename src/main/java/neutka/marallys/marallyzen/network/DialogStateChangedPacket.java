package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.client.gui.DialogState;

/**
 * S2C packet: Notifies client about dialog state changes.
 * Sent from server when dialog state should change (e.g., start narration, open new choice).
 */
public record DialogStateChangedPacket(DialogState state) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DialogStateChangedPacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("dialog_state_changed"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogStateChangedPacket> STREAM_CODEC = 
            StreamCodec.composite(
                    StreamCodec.of(
                            (buf, state) -> NetworkCodecs.STRING.encode(buf, state.name()),
                            buf -> DialogState.valueOf(NetworkCodecs.STRING.decode(buf))
                    ),
                    DialogStateChangedPacket::state,
                    DialogStateChangedPacket::new
            );

    @Override
    public CustomPacketPayload.Type<DialogStateChangedPacket> type() {
        return TYPE;
    }

    public static void handle(DialogStateChangedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogStateChangedPacket: Received state change to {} on client", packet.state());
                // Update client-side state machine
                neutka.marallys.marallyzen.client.gui.DialogStateMachine.getInstance()
                        .transitionTo(packet.state());
            }
        });
    }
}

