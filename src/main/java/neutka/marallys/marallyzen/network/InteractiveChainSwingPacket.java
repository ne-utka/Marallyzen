package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.blocks.InteractiveChainJumpHandler;

/**
 * C2S: Sent while player is hanging to apply swing input.
 */
public record InteractiveChainSwingPacket(byte direction) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InteractiveChainSwingPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("interactive_chain_swing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractiveChainSwingPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeByte(packet.direction()),
            buf -> new InteractiveChainSwingPacket(buf.readByte())
    );

    @Override
    public CustomPacketPayload.Type<InteractiveChainSwingPacket> type() {
        return TYPE;
    }

    public static void handle(InteractiveChainSwingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            InteractiveChainJumpHandler.setSwingInput(player, packet.direction());
        });
    }
}
