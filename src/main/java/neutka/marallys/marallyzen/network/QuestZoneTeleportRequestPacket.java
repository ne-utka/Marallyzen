package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S packet: request instance teleport from a quest zone prompt.
 */
public record QuestZoneTeleportRequestPacket(String zoneId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QuestZoneTeleportRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("quest_zone_teleport_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestZoneTeleportRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    NetworkCodecs.STRING,
                    QuestZoneTeleportRequestPacket::zoneId,
                    QuestZoneTeleportRequestPacket::new
            );

    @Override
    public CustomPacketPayload.Type<QuestZoneTeleportRequestPacket> type() {
        return TYPE;
    }

    public static void handle(QuestZoneTeleportRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            neutka.marallys.marallyzen.quest.QuestManager.getInstance()
                    .onZoneTeleportRequest(player, packet.zoneId());
        });
    }
}
