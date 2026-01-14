package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C packet: Triggers a cutscene playback on the client.
 * Sent from server when /marallyzen playscene is executed.
 */
public record PlayScenePacket(String sceneName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayScenePacket> TYPE = new CustomPacketPayload.Type<>(MarallyzenNetwork.id("play_scene"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, PlayScenePacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(
            NetworkCodecs.STRING,
            PlayScenePacket::sceneName,
            PlayScenePacket::new
    );

    @Override
    public CustomPacketPayload.Type<PlayScenePacket> type() {
        return TYPE;
    }

    public static void handle(PlayScenePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                // Start cutscene playback on client
                neutka.marallys.marallyzen.client.camera.CameraManager.getInstance()
                    .playScene(packet.sceneName);
            }
        });
    }
}

