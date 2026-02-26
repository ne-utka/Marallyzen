package neutka.marallys.marallyzen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import neutka.marallys.marallyzen.trigger.client.TriggerBindClientCache;

import java.util.ArrayList;
import java.util.List;

public record TriggerBindSyncPacket(List<TriggerBindEntry> entries) implements CustomPacketPayload {
    public record TriggerBindEntry(String dimensionId, BlockPos pos) {
    }

    public static final CustomPacketPayload.Type<TriggerBindSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("trigger_bind_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerBindSyncPacket> STREAM_CODEC =
            StreamCodec.of(TriggerBindSyncPacket::write, TriggerBindSyncPacket::read);

    @Override
    public Type<TriggerBindSyncPacket> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, TriggerBindSyncPacket packet) {
        List<TriggerBindEntry> entries = packet.entries() == null ? List.of() : packet.entries();
        buf.writeInt(entries.size());
        for (TriggerBindEntry entry : entries) {
            buf.writeUtf(entry.dimensionId() == null ? "minecraft:overworld" : entry.dimensionId());
            buf.writeBlockPos(entry.pos() == null ? BlockPos.ZERO : entry.pos());
        }
    }

    private static TriggerBindSyncPacket read(RegistryFriendlyByteBuf buf) {
        int size = Math.max(0, buf.readInt());
        List<TriggerBindEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String dimensionId = buf.readUtf();
            BlockPos pos = buf.readBlockPos();
            entries.add(new TriggerBindEntry(dimensionId, pos));
        }
        return new TriggerBindSyncPacket(entries);
    }

    public static void handle(TriggerBindSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!ClientOnly.isClient(context)) {
                return;
            }
            List<TriggerBindClientCache.BindEntry> list = new ArrayList<>();
            if (packet.entries() != null) {
                for (TriggerBindEntry entry : packet.entries()) {
                    if (entry == null || entry.pos() == null) {
                        continue;
                    }
                    list.add(new TriggerBindClientCache.BindEntry(entry.dimensionId(), entry.pos()));
                }
            }
            TriggerBindClientCache.replaceAll(list);
        });
    }
}
