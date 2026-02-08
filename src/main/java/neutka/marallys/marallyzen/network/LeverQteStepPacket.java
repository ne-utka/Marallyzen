package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeverQteStepPacket(int sequenceLength, int stepIndex, boolean expectRight,
                                 int windowTicks, boolean failedReset, int failedButton)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeverQteStepPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("lever_qte_step"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverQteStepPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.INT,
            LeverQteStepPacket::sequenceLength,
            NetworkCodecs.INT,
            LeverQteStepPacket::stepIndex,
            NetworkCodecs.BOOLEAN,
            LeverQteStepPacket::expectRight,
            NetworkCodecs.INT,
            LeverQteStepPacket::windowTicks,
            NetworkCodecs.BOOLEAN,
            LeverQteStepPacket::failedReset,
            NetworkCodecs.INT,
            LeverQteStepPacket::failedButton,
            LeverQteStepPacket::new
        );

    @Override
    public CustomPacketPayload.Type<LeverQteStepPacket> type() {
        return TYPE;
    }

    public static void handle(LeverQteStepPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                neutka.marallys.marallyzen.client.lever.LeverQteClient.step(
                    packet.sequenceLength(),
                    packet.stepIndex(),
                    packet.expectRight(),
                    packet.windowTicks(),
                    packet.failedReset(),
                    packet.failedButton()
                );
            }
        });
    }
}

