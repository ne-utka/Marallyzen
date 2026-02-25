package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.trigger.client.TriggerAnimationClient;

public record TriggerAnimationStopPacket(String instanceId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TriggerAnimationStopPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("trigger_animation_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerAnimationStopPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeUtf(packet.instanceId),
                    buf -> new TriggerAnimationStopPacket(buf.readUtf())
            );

    @Override
    public Type<TriggerAnimationStopPacket> type() {
        return TYPE;
    }

    public static void handle(TriggerAnimationStopPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> TriggerAnimationClient.getInstance().stop(packet.instanceId));
    }
}
