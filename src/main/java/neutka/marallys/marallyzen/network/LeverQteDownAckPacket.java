package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeverQteDownAckPacket(int dummy) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeverQteDownAckPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("lever_qte_down_ack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverQteDownAckPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.INT,
            LeverQteDownAckPacket::dummy,
            LeverQteDownAckPacket::new
        );

    @Override
    public CustomPacketPayload.Type<LeverQteDownAckPacket> type() {
        return TYPE;
    }

    public static void handle(LeverQteDownAckPacket packet, IPayloadContext context) {
        var player = context.player();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            context.enqueueWork(() -> {
                neutka.marallys.marallyzen.blocks.LeverInteractionHandler.handleQteDownAck(serverPlayer);
            });
        }
    }
}

