package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S packet: updates the active quest selection for the player.
 */
public record QuestSelectPacket(String questId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QuestSelectPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("quest_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestSelectPacket> STREAM_CODEC =
            StreamCodec.composite(
                    NetworkCodecs.STRING,
                    QuestSelectPacket::questId,
                    QuestSelectPacket::new
            );

    @Override
    public CustomPacketPayload.Type<QuestSelectPacket> type() {
        return TYPE;
    }

    public static void handle(QuestSelectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            neutka.marallys.marallyzen.quest.QuestManager.getInstance()
                    .selectActiveQuest(player, packet.questId());
        });
    }
}
