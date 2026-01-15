package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S packet: Sent by client when narration overlay has completely finished (fade-out complete, state = HIDDEN).
 * Server uses this to know when it's safe to open the next dialog.
 */
public record NarrationCompletePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NarrationCompletePacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("narration_complete"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, NarrationCompletePacket> STREAM_CODEC = 
            StreamCodec.unit(new NarrationCompletePacket());

    @Override
    public CustomPacketPayload.Type<NarrationCompletePacket> type() {
        return TYPE;
    }

    public static void handle(NarrationCompletePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[NarrationCompletePacket] SERVER: Received narration complete from player {}", player.getName().getString());
                // Notify NpcNarrateHandler that narration is complete for this player
                neutka.marallys.marallyzen.npc.NpcNarrateHandler.onNarrationComplete(player.getUUID());
            }
        });
    }
}

































