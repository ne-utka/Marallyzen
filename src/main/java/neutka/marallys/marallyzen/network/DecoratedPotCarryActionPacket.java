package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.server.DecoratedPotCarryManager;

public record DecoratedPotCarryActionPacket(byte action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DecoratedPotCarryActionPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("decorated_pot_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DecoratedPotCarryActionPacket> STREAM_CODEC =
        StreamCodec.composite(
            StreamCodec.of(RegistryFriendlyByteBuf::writeByte, RegistryFriendlyByteBuf::readByte),
            DecoratedPotCarryActionPacket::action,
            DecoratedPotCarryActionPacket::new
        );

    @Override
    public CustomPacketPayload.Type<DecoratedPotCarryActionPacket> type() {
        return TYPE;
    }

    public static void handle(DecoratedPotCarryActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                DecoratedPotCarryManager.handleAction(player, packet.action());
            }
        });
    }
}

