package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ValveQteEndPacket(boolean success, int downTicks, long downStartTick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ValveQteEndPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("valve_qte_end"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ValveQteEndPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.BOOLEAN,
            ValveQteEndPacket::success,
            NetworkCodecs.INT,
            ValveQteEndPacket::downTicks,
            NetworkCodecs.LONG,
            ValveQteEndPacket::downStartTick,
            ValveQteEndPacket::new
        );

    @Override
    public CustomPacketPayload.Type<ValveQteEndPacket> type() {
        return TYPE;
    }

    public static void handle(ValveQteEndPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                if (packet.success()) {
                    neutka.marallys.marallyzen.network.NetworkHelper.sendToServer(
                        new neutka.marallys.marallyzen.network.ValveQteDownAckPacket(0)
                    );
                    neutka.marallys.marallyzen.client.valve.ValveQteClient.finishSuccess(
                        packet.downTicks(),
                        packet.downStartTick()
                    );
                } else {
                    neutka.marallys.marallyzen.client.valve.ValveQteClient.finishFail();
                }
            }
        });
    }
}


