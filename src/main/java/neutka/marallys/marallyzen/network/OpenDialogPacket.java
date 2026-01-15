package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.UUID;

/**
 * S2C packet: Opens a dialog HUD on the client.
 * Sent from server when an NPC is clicked or a script requests a dialog.
 */
public record OpenDialogPacket(String dialogId, String title, Map<String, String> buttons, UUID npcEntityUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenDialogPacket> TYPE = new CustomPacketPayload.Type<>(MarallyzenNetwork.id("open_dialog"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );
    
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialogPacket> STREAM_CODEC = StreamCodec.composite(
            NetworkCodecs.STRING,
            OpenDialogPacket::dialogId,
            NetworkCodecs.STRING,
            OpenDialogPacket::title,
            NetworkCodecs.STRING_MAP,
            OpenDialogPacket::buttons,
            UUID_CODEC,
            OpenDialogPacket::npcEntityUuid,
            OpenDialogPacket::new
    );

    @Override
    public CustomPacketPayload.Type<OpenDialogPacket> type() {
        return TYPE;
    }

    public static void handle(OpenDialogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                // Open dialog HUD on client
                neutka.marallys.marallyzen.MarallyzenClient.openDialog(
                        packet.dialogId,
                        packet.title,
                        packet.buttons,
                        packet.npcEntityUuid
                );
            }
        });
    }
}

