package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import neutka.marallys.marallyzen.client.gui.TalkIconRenderer;

import java.util.UUID;

/**
 * S2C РїР°РєРµС‚: РїРѕРєР°Р·С‹РІР°РµС‚/СЃРєСЂС‹РІР°РµС‚ Р·РЅР°С‡РѕРє СЂР°Р·РіРѕРІРѕСЂР° РЅР°Рґ NPC.
 */
public record NpcTalkIconPacket(UUID npcEntityUuid, int argbColor, boolean visible) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NpcTalkIconPacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("npc_talk_icon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcTalkIconPacket> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC,
            NpcTalkIconPacket::npcEntityUuid,
            net.minecraft.network.codec.ByteBufCodecs.INT,
            NpcTalkIconPacket::argbColor,
            net.minecraft.network.codec.ByteBufCodecs.BOOL,
            NpcTalkIconPacket::visible,
            NpcTalkIconPacket::new
    );

    @Override
    public CustomPacketPayload.Type<NpcTalkIconPacket> type() {
        return TYPE;
    }

    public static void handle(NpcTalkIconPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ClientOnly.isClient(context)) {
                TalkIconRenderer.updateIcon(
                        packet.npcEntityUuid(),
                        packet.argbColor(),
                        packet.visible()
                );
            }
        });
    }
}


