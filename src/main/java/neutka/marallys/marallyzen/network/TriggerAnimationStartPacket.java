package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.trigger.client.TriggerAnimationClient;

import java.util.ArrayList;
import java.util.List;

public record TriggerAnimationStartPacket(
        String instanceId,
        String dimensionId,
        BlockPos origin,
        BlockPos spawnOffset,
        int durationTicks,
        String animationType,
        String easing,
        List<BlockVisual> blocks
) implements CustomPacketPayload {
    public record BlockVisual(BlockPos localPos, String state) {
    }

    public static final CustomPacketPayload.Type<TriggerAnimationStartPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("trigger_animation_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerAnimationStartPacket> STREAM_CODEC =
            StreamCodec.of(TriggerAnimationStartPacket::write, TriggerAnimationStartPacket::read);

    @Override
    public Type<TriggerAnimationStartPacket> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, TriggerAnimationStartPacket packet) {
        buf.writeUtf(packet.instanceId);
        buf.writeUtf(packet.dimensionId);
        buf.writeBlockPos(packet.origin);
        buf.writeBlockPos(packet.spawnOffset);
        buf.writeInt(packet.durationTicks);
        buf.writeUtf(packet.animationType);
        buf.writeUtf(packet.easing);
        buf.writeInt(packet.blocks.size());
        for (BlockVisual block : packet.blocks) {
            buf.writeBlockPos(block.localPos());
            buf.writeUtf(block.state());
        }
    }

    private static TriggerAnimationStartPacket read(RegistryFriendlyByteBuf buf) {
        String instanceId = buf.readUtf();
        String dimensionId = buf.readUtf();
        BlockPos origin = buf.readBlockPos();
        BlockPos spawnOffset = buf.readBlockPos();
        int durationTicks = buf.readInt();
        String animationType = buf.readUtf();
        String easing = buf.readUtf();
        int size = Math.max(0, buf.readInt());
        List<BlockVisual> blocks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            blocks.add(new BlockVisual(buf.readBlockPos(), buf.readUtf()));
        }
        return new TriggerAnimationStartPacket(instanceId, dimensionId, origin, spawnOffset, durationTicks, animationType, easing, blocks);
    }

    public static void handle(TriggerAnimationStartPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> TriggerAnimationClient.getInstance().start(packet));
    }
}
