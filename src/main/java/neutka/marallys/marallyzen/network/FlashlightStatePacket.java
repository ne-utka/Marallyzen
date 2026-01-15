package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.client.FlashlightStateCache;

import java.util.UUID;

/**
 * S2C packet: Sends flashlight state (enabled/disabled, player rotation) to clients.
 * Broadcast to all players tracking the flashlight owner.
 */
public record FlashlightStatePacket(UUID playerId, boolean enabled, float yaw, float pitch) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FlashlightStatePacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("flashlight_state"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );
    
    public static final StreamCodec<RegistryFriendlyByteBuf, Float> FLOAT_CODEC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeFloat,
            RegistryFriendlyByteBuf::readFloat
    );
    
    public static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOLEAN_CODEC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeBoolean,
            RegistryFriendlyByteBuf::readBoolean
    );
    
    public static final StreamCodec<RegistryFriendlyByteBuf, FlashlightStatePacket> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC,
            FlashlightStatePacket::playerId,
            BOOLEAN_CODEC,
            FlashlightStatePacket::enabled,
            FLOAT_CODEC,
            FlashlightStatePacket::yaw,
            FLOAT_CODEC,
            FlashlightStatePacket::pitch,
            FlashlightStatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<FlashlightStatePacket> type() {
        return TYPE;
    }

    public static void handle(FlashlightStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                // Update client-side cache
                FlashlightStateCache.updateState(
                    packet.playerId(),
                    packet.enabled(),
                    packet.yaw(),
                    packet.pitch()
                );
            }
        });
    }
}

