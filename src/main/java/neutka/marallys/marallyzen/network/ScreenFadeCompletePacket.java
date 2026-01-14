package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S packet: Sent by client when screen fade overlay has completely finished (fade-in complete, state = HIDDEN).
 * Server uses this to know when it's safe to continue script execution.
 */
public record ScreenFadeCompletePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ScreenFadeCompletePacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("screen_fade_complete"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenFadeCompletePacket> STREAM_CODEC = 
            StreamCodec.unit(new ScreenFadeCompletePacket());

    @Override
    public CustomPacketPayload.Type<ScreenFadeCompletePacket> type() {
        return TYPE;
    }

    public static void handle(ScreenFadeCompletePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[ScreenFadeCompletePacket] SERVER: Received screen fade complete from player {}", player.getName().getString());
                // Notify handler that screen fade is complete for this player
                // This will be handled by a handler similar to NpcNarrateHandler
                neutka.marallys.marallyzen.npc.NpcNarrateHandler.onScreenFadeComplete(player.getUUID());
            }
        });
    }
}

































