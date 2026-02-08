package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ValveQteStepPacket(int sequenceLength, int stepIndex, boolean expectRight,
                                 int windowTicks, boolean failedReset, int failedButton)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ValveQteStepPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("valve_qte_step"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ValveQteStepPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.INT,
            ValveQteStepPacket::sequenceLength,
            NetworkCodecs.INT,
            ValveQteStepPacket::stepIndex,
            NetworkCodecs.BOOLEAN,
            ValveQteStepPacket::expectRight,
            NetworkCodecs.INT,
            ValveQteStepPacket::windowTicks,
            NetworkCodecs.BOOLEAN,
            ValveQteStepPacket::failedReset,
            NetworkCodecs.INT,
            ValveQteStepPacket::failedButton,
            ValveQteStepPacket::new
        );

    @Override
    public CustomPacketPayload.Type<ValveQteStepPacket> type() {
        return TYPE;
    }

    public static void handle(ValveQteStepPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                neutka.marallys.marallyzen.client.valve.ValveQteClient.step(
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


