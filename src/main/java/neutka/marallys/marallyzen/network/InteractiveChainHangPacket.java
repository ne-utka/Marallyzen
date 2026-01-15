package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: Notifies client that the local player started or stopped hanging on a chain.
 */
public record InteractiveChainHangPacket(boolean hanging, Vec3 anchor, double length) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InteractiveChainHangPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("interactive_chain_hang"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractiveChainHangPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.hanging());
                writeVec3(buf, packet.anchor());
                buf.writeDouble(packet.length());
            },
            buf -> new InteractiveChainHangPacket(
                    buf.readBoolean(),
                    readVec3(buf),
                    buf.readDouble()
            )
    );

    @Override
    public CustomPacketPayload.Type<InteractiveChainHangPacket> type() {
        return TYPE;
    }

    public static void handle(InteractiveChainHangPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            if (packet.hanging()) {
                neutka.marallys.marallyzen.client.chain.InteractiveChainClientAnimator.startSwing(packet.anchor(), packet.length());
            } else {
                neutka.marallys.marallyzen.client.chain.InteractiveChainClientAnimator.stopSwing();
            }
        });
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
