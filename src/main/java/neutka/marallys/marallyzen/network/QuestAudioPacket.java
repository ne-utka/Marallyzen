package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuestAudioPacket(String filePath, float volume) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuestAudioPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("quest_audio"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Float> FLOAT_CODEC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeFloat,
            RegistryFriendlyByteBuf::readFloat
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestAudioPacket> STREAM_CODEC =
            StreamCodec.composite(
                    NetworkCodecs.STRING,
                    QuestAudioPacket::filePath,
                    FLOAT_CODEC,
                    QuestAudioPacket::volume,
                    QuestAudioPacket::new
            );

    @Override
    public CustomPacketPayload.Type<QuestAudioPacket> type() {
        return TYPE;
    }

    public static void handle(QuestAudioPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                neutka.marallys.marallyzen.client.QuestAudioPlayer.play(packet.filePath(), packet.volume());
            }
        });
    }
}
