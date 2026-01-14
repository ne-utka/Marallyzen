package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * S2C packet: Sends narration text to be displayed in custom HUD overlay.
 * Sent from server when NPC narration should be shown.
 */
public record NarratePacket(Component text, UUID npcUuid, int fadeInTicks, int stayTicks, int fadeOutTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NarratePacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("narrate"));
    
    // Nullable UUID codec (for poster blocks, npcUuid can be null)
    public static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                if (uuid == null) {
                    buf.writeBoolean(false);
                } else {
                    buf.writeBoolean(true);
                    buf.writeLong(uuid.getMostSignificantBits());
                    buf.writeLong(uuid.getLeastSignificantBits());
                }
            },
            buf -> {
                if (buf.readBoolean()) {
                    return new UUID(buf.readLong(), buf.readLong());
                } else {
                    return null;
                }
            }
    );
    
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
    
    public static final StreamCodec<RegistryFriendlyByteBuf, NarratePacket> STREAM_CODEC = StreamCodec.composite(
            COMPONENT_CODEC,
            NarratePacket::text,
            UUID_CODEC,
            NarratePacket::npcUuid,
            INT_CODEC,
            NarratePacket::fadeInTicks,
            INT_CODEC,
            NarratePacket::stayTicks,
            INT_CODEC,
            NarratePacket::fadeOutTicks,
            NarratePacket::new
    );

    @Override
    public CustomPacketPayload.Type<NarratePacket> type() {
        return TYPE;
    }

    public static void handle(NarratePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                String textStr = packet.text() != null ? packet.text().getString() : "null";
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[NarratePacket] CLIENT: Starting narration - text='{}', npcUuid={}, fadeIn={}, stay={}, fadeOut={}", 
                        textStr, packet.npcUuid(), packet.fadeInTicks(), packet.stayTicks(), packet.fadeOutTicks());
                
                // Start narration overlay on client
                neutka.marallys.marallyzen.client.narration.NarrationManager manager = 
                        neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance();
                
                // Clear proximity when narration starts to prevent flickering
                manager.clearProximity();
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("[NarratePacket] CLIENT: Cleared proximity overlay before starting narration");
                
                manager.startNarration(
                        packet.text(),
                        packet.npcUuid(),
                        packet.fadeInTicks(),
                        packet.stayTicks(),
                        packet.fadeOutTicks()
                );
            }
        });
    }
}

