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
 * S2C packet: Sends proximity text to be displayed in custom HUD overlay.
 * Sent from server when player is near an NPC.
 */
public record ProximityPacket(Component text, UUID npcUuid, float alpha) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ProximityPacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("proximity"));
    
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
    
    public static final StreamCodec<RegistryFriendlyByteBuf, Float> FLOAT_CODEC = StreamCodec.of(
            RegistryFriendlyByteBuf::writeFloat,
            RegistryFriendlyByteBuf::readFloat
    );
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ProximityPacket> STREAM_CODEC = StreamCodec.composite(
            COMPONENT_CODEC,
            ProximityPacket::text,
            UUID_CODEC,
            ProximityPacket::npcUuid,
            FLOAT_CODEC,
            ProximityPacket::alpha,
            ProximityPacket::new
    );

    @Override
    public CustomPacketPayload.Type<ProximityPacket> type() {
        return TYPE;
    }

    public static void handle(ProximityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                neutka.marallys.marallyzen.client.narration.NarrationManager manager = 
                        neutka.marallys.marallyzen.client.narration.NarrationManager.getInstance();
                
                // Check if narration is active - don't show proximity if narration is visible
                neutka.marallys.marallyzen.client.narration.NarrationOverlay narration = manager.getActive();
                if (narration != null && narration.isVisible()) {
                    return;
                }
                
                // Update proximity overlay on client
                manager.updateProximity(
                        packet.text(),
                        packet.npcUuid(),
                        packet.alpha()
                );
            }
        });
    }
}

