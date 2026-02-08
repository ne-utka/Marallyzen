package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeverQteInputPacket(boolean rightClick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeverQteInputPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("lever_qte_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverQteInputPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.BOOLEAN,
            LeverQteInputPacket::rightClick,
            LeverQteInputPacket::new
        );

    @Override
    public CustomPacketPayload.Type<LeverQteInputPacket> type() {
        return TYPE;
    }

    public static void handle(LeverQteInputPacket packet, IPayloadContext context) {
        var player = context.player();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            context.enqueueWork(() -> {
                neutka.marallys.marallyzen.blocks.LeverInteractionHandler.handleQteInput(
                    serverPlayer,
                    packet.rightClick()
                );
            });
        }
    }
}

