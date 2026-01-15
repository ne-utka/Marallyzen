package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * S2C: instructs clients to play an Emotecraft emote on an NPC entity.
 */
public record PlayNpcEmotePacket(UUID npcEntityUuid, String emoteId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayNpcEmotePacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("play_npc_emote"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayNpcEmotePacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_CODEC,
                    PlayNpcEmotePacket::npcEntityUuid,
                    NetworkCodecs.STRING,
                    PlayNpcEmotePacket::emoteId,
                    PlayNpcEmotePacket::new
            );

    @Override
    public CustomPacketPayload.Type<PlayNpcEmotePacket> type() {
        return TYPE;
    }

    public static void handle(PlayNpcEmotePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.client.emote.ClientEmoteHandler.handle(packet.npcEntityUuid(), packet.emoteId());
            }
        });
    }
}



