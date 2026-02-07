package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeverQteEndPacket(boolean success, int downTicks, long downStartTick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeverQteEndPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("lever_qte_end"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverQteEndPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.BOOLEAN,
            LeverQteEndPacket::success,
            NetworkCodecs.INT,
            LeverQteEndPacket::downTicks,
            NetworkCodecs.LONG,
            LeverQteEndPacket::downStartTick,
            LeverQteEndPacket::new
        );

    @Override
    public CustomPacketPayload.Type<LeverQteEndPacket> type() {
        return TYPE;
    }

    public static void handle(LeverQteEndPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                if (packet.success()) {
                    neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                        new neutka.marallys.marallyzen.network.LeverQteDownAckPacket(0)
                    );
                    neutka.marallys.marallyzen.client.lever.LeverQteClient.finishSuccess(
                        packet.downTicks(),
                        packet.downStartTick()
                    );
                } else {
                    neutka.marallys.marallyzen.client.lever.LeverQteClient.finishFail();
                }
            }
        });
    }
}

