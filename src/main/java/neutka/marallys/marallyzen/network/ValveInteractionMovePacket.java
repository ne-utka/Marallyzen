package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: Starts the client-side approach movement toward the Valve.
 */
public record ValveInteractionMovePacket(Vec3 targetPos, float targetYaw, float targetPitch, int moveTicks)
    implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ValveInteractionMovePacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("valve_interaction_move"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ValveInteractionMovePacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.VEC3,
            ValveInteractionMovePacket::targetPos,
            NetworkCodecs.FLOAT,
            ValveInteractionMovePacket::targetYaw,
            NetworkCodecs.FLOAT,
            ValveInteractionMovePacket::targetPitch,
            NetworkCodecs.INT,
            ValveInteractionMovePacket::moveTicks,
            ValveInteractionMovePacket::new
        );

    @Override
    public CustomPacketPayload.Type<ValveInteractionMovePacket> type() {
        return TYPE;
    }

    public static void handle(ValveInteractionMovePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                // Smooth approach disabled.
            }
        });
    }
}


