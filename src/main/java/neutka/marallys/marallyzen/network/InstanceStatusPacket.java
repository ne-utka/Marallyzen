package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.client.instance.InstanceClientState;

public record InstanceStatusPacket(boolean inInstance, String questId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<InstanceStatusPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("instance_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InstanceStatusPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.inInstance());
                NetworkCodecs.nullable(NetworkCodecs.STRING).encode(buf, packet.questId());
            },
            buf -> new InstanceStatusPacket(
                    buf.readBoolean(),
                    NetworkCodecs.nullable(NetworkCodecs.STRING).decode(buf)
            )
    );

    @Override
    public CustomPacketPayload.Type<InstanceStatusPacket> type() {
        return TYPE;
    }

    public static void handle(InstanceStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                InstanceClientState.getInstance().applyStatus(packet.inInstance(), packet.questId());
            }
        });
    }
}
