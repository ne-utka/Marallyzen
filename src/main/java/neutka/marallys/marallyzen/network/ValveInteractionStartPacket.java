package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: Starts the Valve interaction sequence on the client.
 */
public record ValveInteractionStartPacket(Vec3 targetPos, float targetYaw, float targetPitch,
                                          int grabTicks, int shakeTicks, int downTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ValveInteractionStartPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("valve_interaction_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ValveInteractionStartPacket> STREAM_CODEC =
        StreamCodec.composite(
            NetworkCodecs.VEC3,
            ValveInteractionStartPacket::targetPos,
            NetworkCodecs.FLOAT,
            ValveInteractionStartPacket::targetYaw,
            NetworkCodecs.FLOAT,
            ValveInteractionStartPacket::targetPitch,
            NetworkCodecs.INT,
            ValveInteractionStartPacket::grabTicks,
            NetworkCodecs.INT,
            ValveInteractionStartPacket::shakeTicks,
            NetworkCodecs.INT,
            ValveInteractionStartPacket::downTicks,
            ValveInteractionStartPacket::new
        );

    @Override
    public CustomPacketPayload.Type<ValveInteractionStartPacket> type() {
        return TYPE;
    }

    public static void handle(ValveInteractionStartPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                neutka.marallys.marallyzen.client.valve.ValveInteractionClient.start(
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


