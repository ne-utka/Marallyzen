package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.client.OldTvMediaManager;

public record OldTvBindModePacket(boolean enabled, String mediaName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OldTvBindModePacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("old_tv_bind_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OldTvBindModePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.enabled());
                NetworkCodecs.nullable(NetworkCodecs.STRING).encode(buf, packet.mediaName());
            },
            buf -> new OldTvBindModePacket(
                    buf.readBoolean(),
                    NetworkCodecs.nullable(NetworkCodecs.STRING).decode(buf)
            )
    );

    @Override
    public CustomPacketPayload.Type<OldTvBindModePacket> type() {
        return TYPE;
    }

    public static void handle(OldTvBindModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            if (packet.enabled()) {
                OldTvMediaManager.enableBindMode(packet.mediaName());
            } else {
                OldTvMediaManager.disableBindMode();
            }
        });
    }
}
