package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeverQteStartPacket(BlockPos leverPos, int sequenceLength, int stepIndex, boolean expectRight,
                                  int windowTicks, int shakeLoopTicks, int grabTicks)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeverQteStartPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("lever_qte_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverQteStartPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                NetworkCodecs.BLOCK_POS.encode(buf, packet.leverPos());
                NetworkCodecs.INT.encode(buf, packet.sequenceLength());
                NetworkCodecs.INT.encode(buf, packet.stepIndex());
                NetworkCodecs.BOOLEAN.encode(buf, packet.expectRight());
                NetworkCodecs.INT.encode(buf, packet.windowTicks());
                NetworkCodecs.INT.encode(buf, packet.shakeLoopTicks());
                NetworkCodecs.INT.encode(buf, packet.grabTicks());
            },
            buf -> new LeverQteStartPacket(
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
    public CustomPacketPayload.Type<LeverQteStartPacket> type() {
        return TYPE;
    }

    public static void handle(LeverQteStartPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                neutka.marallys.marallyzen.client.lever.LeverQteClient.start(
                    packet.leverPos(),
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

