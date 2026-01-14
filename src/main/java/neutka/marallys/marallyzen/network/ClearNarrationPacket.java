package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C packet: Clears the active narration overlay.
 * Sent from server when narration should be forcefully closed.
 */
public record ClearNarrationPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClearNarrationPacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("clear_narration"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearNarrationPacket> STREAM_CODEC = 
            StreamCodec.unit(new ClearNarrationPacket());

    @Override
    public CustomPacketPayload.Type<ClearNarrationPacket> type() {
        return TYPE;
    }

    public static void handle(ClearNarrationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("[ClearNarrationPacket] CLIENT: Starting fade-out for narration overlay");
                // Start fade-out animation (smooth) instead of immediately clearing
                neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance()
                        .startNarrationFadeOut();
            }
        });
    }
}

