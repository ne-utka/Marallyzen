package neutka.marallys.marallyzen.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.MOD)
@SuppressWarnings("removal")
public class MarallyzenNetwork {

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, path);
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar(Marallyzen.MODID);

        // Register S2C (server-to-client) packets
        registrar.playToClient(
                OpenDialogPacket.TYPE,
                OpenDialogPacket.STREAM_CODEC,
                (packet, ctx) -> {
                    Marallyzen.LOGGER.info("[NetDebug] CLIENT recv OpenDialogPacket");
                    OpenDialogPacket.handle(packet, ctx);
                }
        );

        registrar.playToClient(
                PlayScenePacket.TYPE,
                PlayScenePacket.STREAM_CODEC,
                PlayScenePacket::handle
        );

        registrar.playToClient(
                ReloadScenesPacket.TYPE,
                ReloadScenesPacket.STREAM_CODEC,
                ReloadScenesPacket::handle
        );

        registrar.playToClient(
                ReceiveScriptsPacket.TYPE,
                ReceiveScriptsPacket.STREAM_CODEC,
                ReceiveScriptsPacket::handle
        );

        registrar.playToClient(
                PlayNpcEmotePacket.TYPE,
                PlayNpcEmotePacket.STREAM_CODEC,
                PlayNpcEmotePacket::handle
        );

        registrar.playToClient(
                NpcTalkIconPacket.TYPE,
                NpcTalkIconPacket.STREAM_CODEC,
                NpcTalkIconPacket::handle
        );

        registrar.playToClient(
                AnimationPacket.TYPE,
                AnimationPacket.STREAM_CODEC,
                AnimationPacket::handle
        );

        registrar.playToClient(
                DialogStateChangedPacket.TYPE,
                DialogStateChangedPacket.STREAM_CODEC,
                DialogStateChangedPacket::handle
        );

        registrar.playToClient(
                NarratePacket.TYPE,
                NarratePacket.STREAM_CODEC,
                (packet, ctx) -> {
                    Marallyzen.LOGGER.info("[NetDebug] CLIENT recv NarratePacket");
                    NarratePacket.handle(packet, ctx);
                }
        );

        registrar.playToClient(
                ClearNarrationPacket.TYPE,
                ClearNarrationPacket.STREAM_CODEC,
                ClearNarrationPacket::handle
        );

        registrar.playToClient(
                ProximityPacket.TYPE,
                ProximityPacket.STREAM_CODEC,
                ProximityPacket::handle
        );

        registrar.playToClient(
                ClearProximityPacket.TYPE,
                ClearProximityPacket.STREAM_CODEC,
                ClearProximityPacket::handle
        );

        registrar.playToClient(
                ScreenFadePacket.TYPE,
                ScreenFadePacket.STREAM_CODEC,
                (packet, ctx) -> {
                    Marallyzen.LOGGER.info("[NetDebug] CLIENT recv ScreenFadePacket");
                    ScreenFadePacket.handle(packet, ctx);
                }
        );

        registrar.playToClient(
                EyesClosePacket.TYPE,
                EyesClosePacket.STREAM_CODEC,
                EyesClosePacket::handle
        );

        registrar.playToClient(
                FlashlightStatePacket.TYPE,
                FlashlightStatePacket.STREAM_CODEC,
                FlashlightStatePacket::handle
        );

        registrar.playToClient(
                LeverShakePacket.TYPE,
                LeverShakePacket.STREAM_CODEC,
                LeverShakePacket::handle
        );

        registrar.playToClient(
                LeverInteractionStartPacket.TYPE,
                LeverInteractionStartPacket.STREAM_CODEC,
                LeverInteractionStartPacket::handle
        );

        registrar.playToClient(
                LeverInteractionMovePacket.TYPE,
                LeverInteractionMovePacket.STREAM_CODEC,
                LeverInteractionMovePacket::handle
        );

        registrar.playToClient(
                InteractiveChainJumpPacket.TYPE,
                InteractiveChainJumpPacket.STREAM_CODEC,
                InteractiveChainJumpPacket::handle
        );

        registrar.playToClient(
                InteractiveChainHangPacket.TYPE,
                InteractiveChainHangPacket.STREAM_CODEC,
                InteractiveChainHangPacket::handle
        );

        registrar.playToClient(
                InteractiveChainAttachPacket.TYPE,
                InteractiveChainAttachPacket.STREAM_CODEC,
                InteractiveChainAttachPacket::handle
        );

        registrar.playToClient(
                InteractiveChainSwingStatePacket.TYPE,
                InteractiveChainSwingStatePacket.STREAM_CODEC,
                InteractiveChainSwingStatePacket::handle
        );

        registrar.playToClient(
                QuestSyncPacket.TYPE,
                QuestSyncPacket.STREAM_CODEC,
                QuestSyncPacket::handle
        );

        registrar.playToClient(
                CutsceneWorldTrackPacket.TYPE,
                CutsceneWorldTrackPacket.STREAM_CODEC,
                CutsceneWorldTrackPacket::handle
        );

        registrar.playToClient(
                QuestAudioPacket.TYPE,
                QuestAudioPacket.STREAM_CODEC,
                QuestAudioPacket::handle
        );

        registrar.playToClient(
                QuestNarratePacket.TYPE,
                QuestNarratePacket.STREAM_CODEC,
                QuestNarratePacket::handle
        );

        registrar.playToClient(
                InstanceStatusPacket.TYPE,
                InstanceStatusPacket.STREAM_CODEC,
                (packet, ctx) -> {
                    Marallyzen.LOGGER.info("[NetDebug] CLIENT recv InstanceStatusPacket");
                    InstanceStatusPacket.handle(packet, ctx);
                }
        );

        registrar.playToClient(
                InstanceRegistryPacket.TYPE,
                InstanceRegistryPacket.STREAM_CODEC,
                (packet, ctx) -> {
                    Marallyzen.LOGGER.info("[NetDebug] CLIENT recv InstanceRegistryPacket");
                    InstanceRegistryPacket.handle(packet, ctx);
                }
        );
        
        registrar.playToClient(
                OldTvBindModePacket.TYPE,
                OldTvBindModePacket.STREAM_CODEC,
                OldTvBindModePacket::handle
        );

        // Register C2S (client-to-server) packets
        registrar.playToServer(
                DialogButtonClickPacket.TYPE,
                DialogButtonClickPacket.STREAM_CODEC,
                DialogButtonClickPacket::handle
        );

        registrar.playToServer(
                FireEventPacket.TYPE,
                FireEventPacket.STREAM_CODEC,
                FireEventPacket::handle
        );

        registrar.playToServer(
                SendConfirmationPacket.TYPE,
                SendConfirmationPacket.STREAM_CODEC,
                SendConfirmationPacket::handle
        );

        registrar.playToServer(
                DialogClosePacket.TYPE,
                DialogClosePacket.STREAM_CODEC,
                DialogClosePacket::handle
        );

        registrar.playToServer(
                NarrationCompletePacket.TYPE,
                NarrationCompletePacket.STREAM_CODEC,
                NarrationCompletePacket::handle
        );

        registrar.playToServer(
                ScreenFadeCompletePacket.TYPE,
                ScreenFadeCompletePacket.STREAM_CODEC,
                ScreenFadeCompletePacket::handle
        );

        registrar.playToServer(
                EyesCloseCompletePacket.TYPE,
                EyesCloseCompletePacket.STREAM_CODEC,
                EyesCloseCompletePacket::handle
        );

        registrar.playToServer(
                PosterInteractPacket.TYPE,
                PosterInteractPacket.STREAM_CODEC,
                PosterInteractPacket::handle
        );

        registrar.playToServer(
                PosterBookBindPacket.TYPE,
                PosterBookBindPacket.STREAM_CODEC,
                PosterBookBindPacket::handle
        );

        registrar.playToServer(
                ReplayRecordPacket.TYPE,
                ReplayRecordPacket.STREAM_CODEC,
                ReplayRecordPacket::handle
        );

        registrar.playToServer(
                InteractiveChainInteractPacket.TYPE,
                InteractiveChainInteractPacket.STREAM_CODEC,
                InteractiveChainInteractPacket::handle
        );

        registrar.playToServer(
                InteractiveChainSwingPacket.TYPE,
                InteractiveChainSwingPacket.STREAM_CODEC,
                InteractiveChainSwingPacket::handle
        );

        registrar.playToServer(
                QuestSelectPacket.TYPE,
                QuestSelectPacket.STREAM_CODEC,
                QuestSelectPacket::handle
        );

        registrar.playToServer(
                CutsceneWorldRecordPacket.TYPE,
                CutsceneWorldRecordPacket.STREAM_CODEC,
                CutsceneWorldRecordPacket::handle
        );

        registrar.playToServer(
                OldTvBookBindPacket.TYPE,
                OldTvBookBindPacket.STREAM_CODEC,
                OldTvBookBindPacket::handle
        );

        registrar.playToServer(
                RadioInteractPacket.TYPE,
                RadioInteractPacket.STREAM_CODEC,
                RadioInteractPacket::handle
        );

        registrar.playToServer(
                InstanceLeaveRequestPacket.TYPE,
                InstanceLeaveRequestPacket.STREAM_CODEC,
                InstanceLeaveRequestPacket::handle
        );

        registrar.playToServer(
                QuestZoneTeleportRequestPacket.TYPE,
                QuestZoneTeleportRequestPacket.STREAM_CODEC,
                QuestZoneTeleportRequestPacket::handle
        );

        registrar.playToServer(
                DecoratedPotCarryActionPacket.TYPE,
                DecoratedPotCarryActionPacket.STREAM_CODEC,
                DecoratedPotCarryActionPacket::handle
        );

        Marallyzen.LOGGER.info("Marallyzen network packets registered");
    }
}
