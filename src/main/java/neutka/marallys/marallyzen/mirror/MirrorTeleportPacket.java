package neutka.marallys.marallyzen.mirror;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.network.MarallyzenNetwork;
import neutka.marallys.marallyzen.network.NetworkCodecs;

public record MirrorTeleportPacket(
        BlockPos targetPos,
        int fadeInTimeMs,
        int fadeOutTimeMs,
        int negativeTimeMs
) implements CustomPacketPayload {

    public static final Type<MirrorTeleportPacket> TYPE =
            new Type<>(MarallyzenNetwork.id("mirror_teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MirrorTeleportPacket> STREAM_CODEC =
            StreamCodec.composite(
                    NetworkCodecs.BLOCK_POS,
                    MirrorTeleportPacket::targetPos,
                    NetworkCodecs.INT,
                    MirrorTeleportPacket::fadeInTimeMs,
                    NetworkCodecs.INT,
                    MirrorTeleportPacket::fadeOutTimeMs,
                    NetworkCodecs.INT,
                    MirrorTeleportPacket::negativeTimeMs,
                    MirrorTeleportPacket::new
            );

    @Override
    public Type<MirrorTeleportPacket> type() {
        return TYPE;
    }

    public static void handle(MirrorTeleportPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MirrorTeleportClient.start(packet);
        });
    }
}
