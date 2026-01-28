package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LeverShakePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LeverShakePacket> TYPE =
        new CustomPacketPayload.Type<>(MarallyzenNetwork.id("lever_shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeverShakePacket> STREAM_CODEC =
        StreamCodec.unit(new LeverShakePacket());

    @Override
    public CustomPacketPayload.Type<LeverShakePacket> type() {
        return TYPE;
    }

    public static void handle(LeverShakePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[NetDebug] CLIENT recv LeverShakePacket");
                neutka.marallys.marallyzen.client.animation.LeverShakeAnimationClient.playForLocalPlayer();
            }
        });
    }
}
