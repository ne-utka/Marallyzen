package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuestSyncPacket(String payloadJson) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QuestSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("quest_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestSyncPacket> STREAM_CODEC = StreamCodec.composite(
            NetworkCodecs.STRING,
            QuestSyncPacket::payloadJson,
            QuestSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<QuestSyncPacket> type() {
        return TYPE;
    }

    public static void handle(QuestSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.client.quest.QuestClientState.getInstance().applySync(packet.payloadJson());
            }
        });
    }
}
