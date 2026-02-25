package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.trigger.client.TriggerScreenShakeManager;

public record TriggerScreenShakePacket(float intensity, int durationTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TriggerScreenShakePacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("trigger_screen_shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerScreenShakePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeFloat(packet.intensity);
                        buf.writeInt(packet.durationTicks);
                    },
                    buf -> new TriggerScreenShakePacket(buf.readFloat(), buf.readInt())
            );

    @Override
    public Type<TriggerScreenShakePacket> type() {
        return TYPE;
    }

    public static void handle(TriggerScreenShakePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> TriggerScreenShakeManager.getInstance().push(packet.intensity(), packet.durationTicks()));
    }
}
