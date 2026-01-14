package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C packet: Reloads cutscene scene data on the client.
 * Sent from server when /marallyzen reload is executed.
 */
public record ReloadScenesPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReloadScenesPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("reload_scenes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadScenesPacket> STREAM_CODEC =
            StreamCodec.unit(new ReloadScenesPacket());

    @Override
    public CustomPacketPayload.Type<ReloadScenesPacket> type() {
        return TYPE;
    }

    public static void handle(ReloadScenesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[ReloadScenesPacket] CLIENT: Reloading cutscenes");
                neutka.marallys.marallyzen.client.camera.SceneLoader.loadScenes();
                int count = neutka.marallys.marallyzen.client.camera.SceneLoader.getAllScenes().size();
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[ReloadScenesPacket] CLIENT: Loaded {} cutscene(s)", count);
            }
        });
    }
}
