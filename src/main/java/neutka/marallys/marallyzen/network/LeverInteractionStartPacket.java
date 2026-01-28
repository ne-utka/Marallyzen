package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: Starts the lever interaction sequence on the client.
 */
public record LeverInteractionStartPacket(Vec3 targetPos, float targetYaw, float targetPitch,
                                          int grabTicks, int shakeTicks, int downTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeverInteractionStartPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("lever_interaction_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverInteractionStartPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.VEC3,
            LeverInteractionStartPacket::targetPos,
            NetworkCodecs.FLOAT,
            LeverInteractionStartPacket::targetYaw,
            NetworkCodecs.FLOAT,
            LeverInteractionStartPacket::targetPitch,
            NetworkCodecs.INT,
            LeverInteractionStartPacket::grabTicks,
            NetworkCodecs.INT,
            LeverInteractionStartPacket::shakeTicks,
            NetworkCodecs.INT,
            LeverInteractionStartPacket::downTicks,
            LeverInteractionStartPacket::new
        );

    @Override
    public CustomPacketPayload.Type<LeverInteractionStartPacket> type() {
        return TYPE;
    }

    public static void handle(LeverInteractionStartPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.client.lever.LeverInteractionClient.start(
                    packet.targetPos(),
                    packet.targetYaw(),
                    packet.targetPitch(),
                    packet.grabTicks(),
                    packet.shakeTicks(),
                    packet.downTicks()
                );
            }
        });
    }
}
