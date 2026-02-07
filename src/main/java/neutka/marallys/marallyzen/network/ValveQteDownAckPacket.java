package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ValveQteDownAckPacket(int dummy) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ValveQteDownAckPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("valve_qte_down_ack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ValveQteDownAckPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.INT,
            ValveQteDownAckPacket::dummy,
            ValveQteDownAckPacket::new
        );

    @Override
    public CustomPacketPayload.Type<ValveQteDownAckPacket> type() {
        return TYPE;
    }

    public static void handle(ValveQteDownAckPacket packet, IPayloadContext context) {
        var player = context.player();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            context.enqueueWork(() -> {
                neutka.marallys.marallyzen.blocks.ValveInteractionHandler.handleQteDownAck(serverPlayer);
            });
        }
    }
}


