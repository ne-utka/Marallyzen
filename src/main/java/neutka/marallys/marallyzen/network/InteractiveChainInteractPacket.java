package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.blocks.InteractiveChainJumpHandler;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.npc.NpcAnimationHandler;

/**
 * C2S packet: Sent when player right-clicks on an interactive chain block.
 */
public record InteractiveChainInteractPacket(BlockPos blockPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InteractiveChainInteractPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("interactive_chain_interact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS_CODEC = StreamCodec.of(
            (buf, pos) -> buf.writeLong(pos.asLong()),
            buf -> BlockPos.of(buf.readLong())
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractiveChainInteractPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BLOCK_POS_CODEC,
                    InteractiveChainInteractPacket::blockPos,
                    InteractiveChainInteractPacket::new
            );

    @Override
    public CustomPacketPayload.Type<InteractiveChainInteractPacket> type() {
        return TYPE;
    }

    public static void handle(InteractiveChainInteractPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            if (player.level().getBlockState(packet.blockPos()).getBlock() != MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
                return;
            }

            InteractiveChainJumpHandler.startJump(player, packet.blockPos());
            NpcAnimationHandler.sendAnimationToNearbyPlayers(
                    player,
                    InteractiveChainJumpHandler.CHAIN_EMOTE_ID,
                    32
            );
        });
    }
}
