package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ValveQteStartPacket(BlockPos valvePos, int sequenceLength, int stepIndex, boolean expectRight,
                                  int windowTicks, int shakeLoopTicks, int grabTicks)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ValveQteStartPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("valve_qte_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ValveQteStartPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                NetworkCodecs.BLOCK_POS.encode(buf, packet.valvePos());
                NetworkCodecs.INT.encode(buf, packet.sequenceLength());
                NetworkCodecs.INT.encode(buf, packet.stepIndex());
                NetworkCodecs.BOOLEAN.encode(buf, packet.expectRight());
                NetworkCodecs.INT.encode(buf, packet.windowTicks());
                NetworkCodecs.INT.encode(buf, packet.shakeLoopTicks());
                NetworkCodecs.INT.encode(buf, packet.grabTicks());
            },
            buf -> new ValveQteStartPacket(
                NetworkCodecs.BLOCK_POS.decode(buf),
                NetworkCodecs.INT.decode(buf),
                NetworkCodecs.INT.decode(buf),
                NetworkCodecs.BOOLEAN.decode(buf),
                NetworkCodecs.INT.decode(buf),
                NetworkCodecs.INT.decode(buf),
                NetworkCodecs.INT.decode(buf)
            )
        );

    @Override
    public CustomPacketPayload.Type<ValveQteStartPacket> type() {
        return TYPE;
    }

    public static void handle(ValveQteStartPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                neutka.marallys.marallyzen.client.valve.ValveQteClient.start(
                    packet.valvePos(),
                    packet.sequenceLength(),
                    packet.stepIndex(),
                    packet.expectRight(),
                    packet.windowTicks(),
                    packet.shakeLoopTicks(),
                    packet.grabTicks()
                );
            }
        });
    }
}


