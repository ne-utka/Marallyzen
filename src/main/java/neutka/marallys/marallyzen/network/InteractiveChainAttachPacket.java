package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * S2C: Notifies clients that a player attached to or detached from a chain.
 */
public record InteractiveChainAttachPacket(UUID playerId, BlockPos chainRoot, Vec3 anchor, double length, boolean attached)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InteractiveChainAttachPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("interactive_chain_attach"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractiveChainAttachPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                writeUuid(buf, packet.playerId());
                buf.writeLong(packet.chainRoot().asLong());
                writeVec3(buf, packet.anchor());
                buf.writeDouble(packet.length());
                buf.writeBoolean(packet.attached());
            },
            buf -> new InteractiveChainAttachPacket(
                    readUuid(buf),
                    BlockPos.of(buf.readLong()),
                    readVec3(buf),
                    buf.readDouble(),
                    buf.readBoolean()
            )
    );

    @Override
    public CustomPacketPayload.Type<InteractiveChainAttachPacket> type() {
        return TYPE;
    }

    public static void handle(InteractiveChainAttachPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            neutka.marallys.marallyzen.client.chain.InteractiveChainPoseManager.handleAttach(packet);
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
