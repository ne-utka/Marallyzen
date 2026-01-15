package neutka.marallys.marallyzen.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NpcAppearancePacket(
        String npcId,
        String skinTexture,
        String skinSignature,
        String skinModel
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NpcAppearancePacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("npc_appearance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcAppearancePacket> STREAM_CODEC =
            StreamCodec.composite(
                    NetworkCodecs.STRING,
                    NpcAppearancePacket::npcId,
                    NetworkCodecs.STRING,
                    NpcAppearancePacket::skinTexture,
                    NetworkCodecs.STRING,
                    NpcAppearancePacket::skinSignature,
                    NetworkCodecs.STRING,
                    NpcAppearancePacket::skinModel,
                    NpcAppearancePacket::new
            );

    @Override
    public CustomPacketPayload.Type<NpcAppearancePacket> type() {
        return TYPE;
    }

    public static void handle(NpcAppearancePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            neutka.marallys.marallyzen.client.NpcSkinCache.getInstance().updateAppearance(
                    packet.npcId(),
                    packet.skinTexture(),
                    packet.skinSignature(),
                    packet.skinModel()
            );
        });
    }
}
