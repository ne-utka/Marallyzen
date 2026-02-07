package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ValveQteInputPacket(boolean rightClick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ValveQteInputPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("valve_qte_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ValveQteInputPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.BOOLEAN,
            ValveQteInputPacket::rightClick,
            ValveQteInputPacket::new
        );

    @Override
    public CustomPacketPayload.Type<ValveQteInputPacket> type() {
        return TYPE;
    }

    public static void handle(ValveQteInputPacket packet, IPayloadContext context) {
        var player = context.player();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            context.enqueueWork(() -> {
                neutka.marallys.marallyzen.blocks.ValveInteractionHandler.handleQteInput(
                    serverPlayer,
                    packet.rightClick()
                );
            });
        }
    }
}


