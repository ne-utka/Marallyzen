package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OldTvBookBindPacket(BlockPos blockPos, boolean mainHand) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OldTvBookBindPacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("old_tv_book_bind"));

    private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS_CODEC = StreamCodec.of(
        (buf, pos) -> buf.writeLong(pos.asLong()),
        buf -> BlockPos.of(buf.readLong())
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOL_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeBoolean(value),
        RegistryFriendlyByteBuf::readBoolean
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OldTvBookBindPacket> STREAM_CODEC =
        StreamCodec.composite(
            BLOCK_POS_CODEC,
            OldTvBookBindPacket::blockPos,
            BOOL_CODEC,
            OldTvBookBindPacket::mainHand,
            OldTvBookBindPacket::new
        );

    @Override
    public CustomPacketPayload.Type<OldTvBookBindPacket> type() {
        return TYPE;
    }

    public static void handle(OldTvBookBindPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }

            var level = player.serverLevel();
            BlockPos pos = packet.blockPos();
            if (!level.isLoaded(pos)) {
                return;
            }

            if (!(level.getBlockState(pos).getBlock() instanceof neutka.marallys.marallyzen.blocks.OldTvBlock)) {
                return;
            }

            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 36.0) {
                return;
            }

            net.minecraft.world.InteractionHand hand = packet.mainHand()
                ? net.minecraft.world.InteractionHand.MAIN_HAND
                : net.minecraft.world.InteractionHand.OFF_HAND;
            var stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof net.minecraft.world.item.WrittenBookItem)) {
                return;
            }

            if (!player.hasPermissions(2)) {
                return;
            }

            var be = level.getBlockEntity(pos);
            if (be instanceof neutka.marallys.marallyzen.blocks.OldTvBlockEntity tvEntity) {
                tvEntity.setProtectedByOp(true);
                tvEntity.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                level.getChunkAt(pos).setUnsaved(true);
            }
        });
    }
}
