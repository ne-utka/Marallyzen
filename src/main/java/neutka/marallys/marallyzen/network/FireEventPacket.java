package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

/**
 * C2S packet: Sends a client-side event to the server.
 * Used for client-side script events that need server processing.
 */
public record FireEventPacket(String eventId, Map<String, String> data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FireEventPacket> TYPE = new CustomPacketPayload.Type<>(MarallyzenNetwork.id("fire_event"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, FireEventPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(
            NetworkCodecs.STRING,
            FireEventPacket::eventId,
            NetworkCodecs.STRING_MAP,
            FireEventPacket::data,
            FireEventPacket::new
    );

    @Override
    public CustomPacketPayload.Type<FireEventPacket> type() {
        return TYPE;
    }

    public static void handle(FireEventPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            neutka.marallys.marallyzen.quest.QuestManager.getInstance()
                    .fireCustomEvent(player, packet.eventId(), packet.data());
        });
    }
}
