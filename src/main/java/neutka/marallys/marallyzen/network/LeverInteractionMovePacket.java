package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: Starts the client-side approach movement toward the lever.
 */
public record LeverInteractionMovePacket(Vec3 targetPos, float targetYaw, float targetPitch, int moveTicks)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LeverInteractionMovePacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("lever_interaction_move"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverInteractionMovePacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.VEC3,
            LeverInteractionMovePacket::targetPos,
            NetworkCodecs.FLOAT,
            LeverInteractionMovePacket::targetYaw,
            NetworkCodecs.FLOAT,
            LeverInteractionMovePacket::targetPitch,
            NetworkCodecs.INT,
            LeverInteractionMovePacket::moveTicks,
            LeverInteractionMovePacket::new
        );

    @Override
    public CustomPacketPayload.Type<LeverInteractionMovePacket> type() {
        return TYPE;
    }

    public static void handle(LeverInteractionMovePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.client.lever.LeverInteractionClient.startMove(
                    packet.targetPos(),
                    packet.targetYaw(),
                    packet.targetPitch(),
                    packet.moveTicks()
                );
            }
        });
    }
}
