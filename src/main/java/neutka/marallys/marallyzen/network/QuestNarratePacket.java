package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuestNarratePacket(Component text, int fadeInTicks, int stayTicks, int fadeOutTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuestNarratePacket> TYPE =
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("quest_narrate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Component> COMPONENT_CODEC = StreamCodec.of(
            (buf, component) -> {
                String json = Component.Serializer.toJson(component, buf.registryAccess());
                NetworkCodecs.STRING.encode(buf, json);
            },
            buf -> {
                String json = NetworkCodecs.STRING.decode(buf);
                return Component.Serializer.fromJson(json, buf.registryAccess());
            }
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Integer> INT_CODEC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeInt,
            RegistryFriendlyByteBuf::readInt
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestNarratePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                COMPONENT_CODEC.encode(buf, packet.text());
                INT_CODEC.encode(buf, packet.fadeInTicks());
                INT_CODEC.encode(buf, packet.stayTicks());
                INT_CODEC.encode(buf, packet.fadeOutTicks());
            },
            buf -> new QuestNarratePacket(
                    COMPONENT_CODEC.decode(buf),
                    INT_CODEC.decode(buf),
                    INT_CODEC.decode(buf),
                    INT_CODEC.decode(buf)
            )
    );

    @Override
    public CustomPacketPayload.Type<QuestNarratePacket> type() {
        return TYPE;
    }

    public static void handle(QuestNarratePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance()
                        .startNarration(packet.text(), null, packet.fadeInTicks(), packet.stayTicks(), packet.fadeOutTicks(), false);
            }
        });
    }
}
