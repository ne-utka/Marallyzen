package neutka.marallys.marallyzen.network;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.blocks.InteractiveChainBlockEntity;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.client.chain.InteractiveChainClientHider;

/**
 * S2C: Updates swing state for rendering chain animation.
 */
public record InteractiveChainSwingStatePacket(BlockPos chainRoot, Vec3 anchor, Vec3 offset, boolean active, long tick) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InteractiveChainSwingStatePacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("interactive_chain_swing_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractiveChainSwingStatePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeLong(packet.chainRoot().asLong());
                writeVec3(buf, packet.anchor());
                writeVec3(buf, packet.offset());
                buf.writeBoolean(packet.active());
                buf.writeLong(packet.tick());
            },
            buf -> new InteractiveChainSwingStatePacket(
                    BlockPos.of(buf.readLong()),
                    readVec3(buf),
                    readVec3(buf),
                    buf.readBoolean(),
                    buf.readLong()
            )
    );

    @Override
    public CustomPacketPayload.Type<InteractiveChainSwingStatePacket> type() {
        return TYPE;
    }

    public static void handle(InteractiveChainSwingStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            if (packet.active()) {
                long clientTime = Util.getMillis();
                neutka.marallys.marallyzen.client.chain.InteractiveChainSwingVisuals.update(
                    packet.chainRoot(),
                    packet.anchor(),
                    packet.offset(),
                    packet.tick(),
                    clientTime
                );
                ensureBlockEntity(context.player(), packet.chainRoot());
                if (context.player() != null) {
                    InteractiveChainClientHider.hideChain(context.player().level(), packet.chainRoot());
                }
            } else {
                boolean restoreNow = neutka.marallys.marallyzen.client.chain.InteractiveChainSwingVisuals.beginSettle(
                    packet.chainRoot(),
                    Util.getMillis()
                );
                if (restoreNow && context.player() != null) {
                    InteractiveChainClientHider.restoreChain(context.player().level(), packet.chainRoot());
                }
            }
        });
    }

    private static void writeVec3(RegistryFriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    private static Vec3 readVec3(RegistryFriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void ensureBlockEntity(Player player, BlockPos root) {
        if (player == null || root == null) {
            return;
        }
        Level level = player.level();
        if (level == null || !level.hasChunkAt(root)) {
            return;
        }
        if (level.getBlockState(root).getBlock() != MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            return;
        }
        if (level.getBlockEntity(root) instanceof InteractiveChainBlockEntity) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(root);
        if (chunk == null) {
            return;
        }
        if (!(chunk.getBlockEntity(root, LevelChunk.EntityCreationType.IMMEDIATE)
            instanceof InteractiveChainBlockEntity)) {
            level.setBlockEntity(new InteractiveChainBlockEntity(root, level.getBlockState(root)));
        }
    }
}
