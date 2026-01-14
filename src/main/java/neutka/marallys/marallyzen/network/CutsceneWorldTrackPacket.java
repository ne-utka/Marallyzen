package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.client.cutscene.world.CutsceneWorldTrackAssembler;

public record CutsceneWorldTrackPacket(String sceneId, int partIndex, int totalParts, byte[] payload)
    implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CutsceneWorldTrackPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("cutscene_world_track"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT_CODEC = StreamCodec.of(
        RegistryFriendlyByteBuf::writeInt,
        RegistryFriendlyByteBuf::readInt
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, byte[]> BYTE_ARRAY_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeByteArray(value),
        buf -> buf.readByteArray()
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CutsceneWorldTrackPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.STRING,
            CutsceneWorldTrackPacket::sceneId,
            INT_CODEC,
            CutsceneWorldTrackPacket::partIndex,
            INT_CODEC,
            CutsceneWorldTrackPacket::totalParts,
            BYTE_ARRAY_CODEC,
            CutsceneWorldTrackPacket::payload,
            CutsceneWorldTrackPacket::new
        );

    @Override
    public CustomPacketPayload.Type<CutsceneWorldTrackPacket> type() {
        return TYPE;
    }

    public static void handle(CutsceneWorldTrackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> CutsceneWorldTrackAssembler.onPacket(packet));
    }
}
