package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S packet: Sent when player right-clicks on a poster block.
 * Triggers the poke animation for the player and nearby players.
 */
public record PosterInteractPacket(BlockPos blockPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PosterInteractPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("poster_interact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS_CODEC = StreamCodec.of(
            (buf, pos) -> {
                buf.writeLong(pos.asLong());
            },
            buf -> BlockPos.of(buf.readLong())
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PosterInteractPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BLOCK_POS_CODEC,
                    PosterInteractPacket::blockPos,
                    PosterInteractPacket::new
            );

    @Override
    public CustomPacketPayload.Type<PosterInteractPacket> type() {
        return TYPE;
    }

    public static void handle(PosterInteractPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }

            // Play poke animation for the player and nearby players
            neutka.marallys.marallyzen.npc.NpcAnimationHandler.sendAnimationToNearbyPlayers(
                    player,
                    "SPE_Poke",
                    32 // 32 block radius
            );

            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("PosterInteractPacket: Player {} interacted with poster at {}, playing poke animation", 
                    player.getName().getString(), packet.blockPos());
        });
    }
}

