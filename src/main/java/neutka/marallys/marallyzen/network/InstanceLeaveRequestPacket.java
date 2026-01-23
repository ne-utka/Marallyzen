package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.instance.InstanceSessionManager;

public record InstanceLeaveRequestPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<InstanceLeaveRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("instance_leave_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InstanceLeaveRequestPacket> STREAM_CODEC =
            StreamCodec.unit(new InstanceLeaveRequestPacket());

    @Override
    public CustomPacketPayload.Type<InstanceLeaveRequestPacket> type() {
        return TYPE;
    }

    public static void handle(InstanceLeaveRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                InstanceSessionManager.getInstance().requestLeave(serverPlayer);
            }
        });
    }
}
