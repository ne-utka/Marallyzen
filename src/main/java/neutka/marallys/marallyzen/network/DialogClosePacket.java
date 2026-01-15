package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * C2S packet: Sent when player closes dialog (e.g., via ESC key).
 */
public record DialogClosePacket(UUID npcEntityUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DialogClosePacket> TYPE = 
            new CustomPacketPayload.Type<>(MarallyzenNetwork.id("dialog_close"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogClosePacket> STREAM_CODEC = 
            StreamCodec.composite(
                    OpenDialogPacket.UUID_CODEC,
                    DialogClosePacket::npcEntityUuid,
                    DialogClosePacket::new
            );

    @Override
    public CustomPacketPayload.Type<DialogClosePacket> type() {
        return TYPE;
    }

    public static void handle(DialogClosePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            
            // Close dialog on server side
            neutka.marallys.marallyzen.npc.NpcProximityHandler.setPlayerDialogClosed(player.getUUID());
            neutka.marallys.marallyzen.ai.NpcAiManager.onDialogClosed(player, packet.npcEntityUuid());
            var registry = neutka.marallys.marallyzen.npc.NpcClickHandler.getRegistry();
            var npcEntity = registry.getNpcByUuid(packet.npcEntityUuid());
            if (npcEntity != null) {
                var npcId = registry.getNpcId(npcEntity);
                var npcData = npcId != null ? registry.getNpcData(npcId) : null;
                neutka.marallys.marallyzen.npc.NpcExpressionManager.applyDefaultExpression(npcEntity, npcData);
            }
            
            // Send state change to client to close dialog
            neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                    player, 
                    new DialogStateChangedPacket(neutka.marallys.marallyzen.client.gui.DialogState.CLOSED)
            );
        });
    }
}


































