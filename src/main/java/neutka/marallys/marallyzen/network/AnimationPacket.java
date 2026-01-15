package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * S2C: Instructs clients to play an emote on an entity (NPC or player).
 * Uses Emotecraft API for local emote playback.
 */
public record AnimationPacket(UUID entityUuid, String animationName, int radius) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AnimationPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT_CODEC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeInt,
            RegistryFriendlyByteBuf::readInt
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AnimationPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUID_CODEC,
                    AnimationPacket::entityUuid,
                    NetworkCodecs.STRING,
                    AnimationPacket::animationName,
                    INT_CODEC,
                    AnimationPacket::radius,
                    AnimationPacket::new
            );

    @Override
    public CustomPacketPayload.Type<AnimationPacket> type() {
        return TYPE;
    }

    public static void handle(AnimationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("AnimationPacket: Received emote '{}' for entity UUID {}", 
                        packet.animationName(), packet.entityUuid());
                neutka.marallys.marallyzen.client.emote.ClientEmoteHandler.handle(
                        packet.entityUuid(), 
                        packet.animationName()
                );
            }
        });
    }
}

