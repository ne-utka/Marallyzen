package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RadioInteractPacket(BlockPos blockPos, byte action, String stationName) implements CustomPacketPayload {
    public static final byte ACTION_TOGGLE = 0;
    public static final byte ACTION_SWITCH = 1;

    public static final CustomPacketPayload.Type<RadioInteractPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("radio_interact"));

    private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS_CODEC = StreamCodec.of(
        (buf, pos) -> buf.writeLong(pos.asLong()),
        buf -> BlockPos.of(buf.readLong())
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, Byte> BYTE_CODEC = StreamCodec.of(
        RegistryFriendlyByteBuf::writeByte,
        RegistryFriendlyByteBuf::readByte
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, String> STRING_CODEC = StreamCodec.of(
        RegistryFriendlyByteBuf::writeUtf,
        buf -> buf.readUtf(32767)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RadioInteractPacket> STREAM_CODEC =
        StreamCodec.composite(
            BLOCK_POS_CODEC,
            RadioInteractPacket::blockPos,
            BYTE_CODEC,
            RadioInteractPacket::action,
            STRING_CODEC,
            RadioInteractPacket::stationName,
            RadioInteractPacket::new
        );

    @Override
    public CustomPacketPayload.Type<RadioInteractPacket> type() {
        return TYPE;
    }

    public static void handle(RadioInteractPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            if (packet.action() == ACTION_SWITCH) {
                neutka.marallys.marallyzen.radio.RadioPlaybackManager.switchTrack(player, packet.blockPos(), packet.stationName());
            } else {
                neutka.marallys.marallyzen.radio.RadioPlaybackManager.toggle(player, packet.blockPos(), packet.stationName());
            }
        });
    }
}
