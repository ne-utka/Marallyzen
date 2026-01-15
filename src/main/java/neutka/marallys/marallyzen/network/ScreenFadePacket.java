package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C packet: Triggers a screen fade cutscene on the client.
 * Sent from server when screen fade should be played.
 */
public record ScreenFadePacket(
        int fadeOutTicks,
        int blackScreenTicks,
        int fadeInTicks,
        Component titleText,
        Component subtitleText,
        boolean blockPlayerInput,
        String soundId  // ResourceLocation ID of sound (e.g., "minecraft:block.anvil.land")
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ScreenFadePacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("screen_fade"));
    
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
    
    public static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOLEAN_CODEC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeBoolean,
            RegistryFriendlyByteBuf::readBoolean
    );
    
    public static final StreamCodec<RegistryFriendlyByteBuf, String> NULLABLE_STRING_CODEC = NetworkCodecs.nullable(NetworkCodecs.STRING);
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenFadePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                INT_CODEC.encode(buf, packet.fadeOutTicks());
                INT_CODEC.encode(buf, packet.blackScreenTicks());
                INT_CODEC.encode(buf, packet.fadeInTicks());
                COMPONENT_CODEC.encode(buf, packet.titleText());
                COMPONENT_CODEC.encode(buf, packet.subtitleText());
                BOOLEAN_CODEC.encode(buf, packet.blockPlayerInput());
                NULLABLE_STRING_CODEC.encode(buf, packet.soundId());
            },
            buf -> new ScreenFadePacket(
                    INT_CODEC.decode(buf),
                    INT_CODEC.decode(buf),
                    INT_CODEC.decode(buf),
                    COMPONENT_CODEC.decode(buf),
                    COMPONENT_CODEC.decode(buf),
                    BOOLEAN_CODEC.decode(buf),
                    NULLABLE_STRING_CODEC.decode(buf)
            )
    );

    @Override
    public CustomPacketPayload.Type<ScreenFadePacket> type() {
        return TYPE;
    }

    public static void handle(ScreenFadePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                String titleStr = packet.titleText() != null ? packet.titleText().getString() : "null";
                String subtitleStr = packet.subtitleText() != null ? packet.subtitleText().getString() : "null";
                String soundStr = packet.soundId() != null ? packet.soundId() : "null";
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[ScreenFadePacket] CLIENT: Starting screen fade - fadeOut={}, blackScreen={}, fadeIn={}, title='{}', subtitle='{}', sound='{}', blockInput={}", 
                        packet.fadeOutTicks(), packet.blackScreenTicks(), packet.fadeInTicks(), titleStr, subtitleStr, soundStr, packet.blockPlayerInput());
                
                // Start screen fade on client
                neutka.marallys.marallyzen.client.cutscene.ScreenFadeManager manager = 
                        neutka.marallys.marallyzen.client.cutscene.ScreenFadeManager.getInstance();
                
                manager.startScreenFade(
                        packet.fadeOutTicks(),
                        packet.blackScreenTicks(),
                        packet.fadeInTicks(),
                        packet.titleText(),
                        packet.subtitleText(),
                        packet.blockPlayerInput(),
                        packet.soundId()
                );
            }
        });
    }
}

