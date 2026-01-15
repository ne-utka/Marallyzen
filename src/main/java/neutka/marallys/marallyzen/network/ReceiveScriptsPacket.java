package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

/**
 * S2C packet: Sends additional scripts to the client for client-side execution.
 * Similar to Clientizen's ReceiveScriptsPacketIn.
 */
public record ReceiveScriptsPacket(Map<String, String> scripts) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReceiveScriptsPacket> TYPE = new CustomPacketPayload.Type<>(MarallyzenNetwork.id("receive_scripts"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, ReceiveScriptsPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(
            NetworkCodecs.STRING_MAP,
            ReceiveScriptsPacket::scripts,
            ReceiveScriptsPacket::new
    );

    @Override
    public CustomPacketPayload.Type<ReceiveScriptsPacket> type() {
        return TYPE;
    }

    public static void handle(ReceiveScriptsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                // TODO: Load scripts into client-side DenizenCore (if we add client-side scripting)
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("ReceiveScriptsPacket received: {} scripts", packet.scripts.size());
            }
        });
    }
}

