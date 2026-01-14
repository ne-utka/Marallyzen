package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * S2C: Smooth jump path for the local player to render at high frame rate.
 */
public record InteractiveChainJumpPacket(
        UUID playerId,
        Vec3 start,
        Vec3 control,
        Vec3 end,
        float startYaw,
        float startPitch,
        float endYaw,
        float endPitch,
        long startTick,
        int durationTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InteractiveChainJumpPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("interactive_chain_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractiveChainJumpPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                writeUuid(buf, packet.playerId());
                writeVec3(buf, packet.start());
                writeVec3(buf, packet.control());
                writeVec3(buf, packet.end());
                buf.writeFloat(packet.startYaw());
                buf.writeFloat(packet.startPitch());
                buf.writeFloat(packet.endYaw());
                buf.writeFloat(packet.endPitch());
                buf.writeLong(packet.startTick());
                buf.writeInt(packet.durationTicks());
            },
            buf -> new InteractiveChainJumpPacket(
                    readUuid(buf),
                    readVec3(buf),
                    readVec3(buf),
                    readVec3(buf),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readLong(),
                    buf.readInt()
            )
    );

    @Override
    public CustomPacketPayload.Type<InteractiveChainJumpPacket> type() {
        return TYPE;
    }

    public static void handle(InteractiveChainJumpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            neutka.marallys.marallyzen.client.chain.InteractiveChainClientAnimator.start(packet);
        });
    }

    private static void writeUuid(RegistryFriendlyByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(RegistryFriendlyByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    private static void writeVec3(RegistryFriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    private static Vec3 readVec3(RegistryFriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
