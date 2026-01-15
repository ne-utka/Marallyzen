package neutka.marallys.marallyzen.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.OpenDialogPacket;
import neutka.marallys.marallyzen.npc.NpcExpressionManager;
import neutka.marallys.marallyzen.audio.AudioMetadata;
import neutka.marallys.marallyzen.audio.MarallyzenAudioService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * Handles player interactions with NPCs.
 */
@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class NpcClickHandler {

    private static final NpcRegistry registry = new NpcRegistry();
    private static final Map<UUID, Long> lastClickTick = new HashMap<>();
    private static final Map<UUID, UUID> lastClickNpc = new HashMap<>();

    public static NpcRegistry getRegistry() {
        return registry;
    }

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = event.getTarget();
        String npcId = registry.getNpcId(target);
        
        if (npcId == null) {
            return; // Not an NPC
        }

        long gameTime = player.level().getGameTime();
        UUID playerId = player.getUUID();
        UUID npcEntityUuid = target.getUUID();
        Long lastTick = lastClickTick.get(playerId);
        UUID lastNpc = lastClickNpc.get(playerId);
        if (lastTick != null && lastNpc != null && lastNpc.equals(npcEntityUuid) && (gameTime - lastTick) <= 2) {
            return;
        }
        if (NpcNarrateHandler.isPlayerReceivingNarrate(playerId)
                && lastNpc != null
                && lastNpc.equals(npcEntityUuid)) {
            return;
        }
        lastClickTick.put(playerId, gameTime);
        lastClickNpc.put(playerId, npcEntityUuid);

        event.setCanceled(true); // Prevent default interaction

        NpcData npcData = registry.getNpcData(npcId);
        if (npcData == null) {
            return;
        }

        neutka.marallys.marallyzen.quest.QuestManager.getInstance().onNpcDialog(player, npcId);
        NpcExpressionManager.applyTalkExpression(target, npcData);

        // AI dialog path
        if (neutka.marallys.marallyzen.ai.NpcAiManager.isAiEnabled(npcData)) {
            UUID openDialogNpcUuid = neutka.marallys.marallyzen.npc.NpcProximityHandler.getPlayerOpenDialogNpc(playerId);

            if (openDialogNpcUuid != null && openDialogNpcUuid.equals(npcEntityUuid)) {
                Marallyzen.LOGGER.info("NpcClickHandler: Player {} already has an open AI dialog with NPC {}, ignoring click", 
                        player.getName().getString(), npcId);
                return;
            }

            Marallyzen.LOGGER.info("NpcClickHandler: Player {} clicked on AI NPC {} - opening dialog", 
                    player.getName().getString(), npcId);
            neutka.marallys.marallyzen.ai.NpcAiManager.openInitialDialog(player, target, npcData);
            return;
        }

        // If NPC has a dialog script, open dialog HUD
        if (npcData.getDialogScript() != null && !npcData.getDialogScript().isEmpty()) {
            // Check if player already has an open dialog with this NPC
            UUID openDialogNpcUuid = neutka.marallys.marallyzen.npc.NpcProximityHandler.getPlayerOpenDialogNpc(playerId);
            
            if (openDialogNpcUuid != null && openDialogNpcUuid.equals(npcEntityUuid)) {
                // Player already has an open dialog with this NPC - don't reopen
                Marallyzen.LOGGER.info("NpcClickHandler: Player {} already has an open dialog with NPC {}, ignoring click (current step: {})", 
                        player.getName().getString(), npcId, 
                        neutka.marallys.marallyzen.npc.NpcProximityHandler.getPlayerDialogStep(playerId));
                return;
            }
            
            Marallyzen.LOGGER.info("NpcClickHandler: Player {} clicked on NPC {} - opening dialog (no existing dialog found)", 
                    player.getName().getString(), npcId);
            
            // Load dialog options from Denizen script (always start with step 1)
            DialogScriptLoader.DialogOptions options = DialogScriptLoader.loadDialogOptions(npcData.getDialogScript(), 1);
            Map<String, String> buttons = options != null ? options.texts() : null;
            
            if (buttons == null || buttons.isEmpty()) {
                Marallyzen.LOGGER.warn("Failed to load dialog options for script '{}' (NPC: {})", 
                        npcData.getDialogScript(), npcId);
                return;
            }
            
            // Check if step 1 has narrate messages (initial narration when dialog opens)
            // These are messages from the click trigger block in step 1, before the choose statement
            // We need to parse them from the script file directly
            java.util.Map.Entry<java.util.List<String>, Integer> initialNarrateData = 
                    neutka.marallys.marallyzen.npc.DialogScriptLoader.parseInitialNarrateMessages(npcData.getDialogScript(), 1);
            java.util.List<String> initialNarrateMessages = initialNarrateData.getKey();
            int initialDuration = initialNarrateData.getValue();
            List<neutka.marallys.marallyzen.npc.DialogScriptLoader.AudioData> initialAudioList =
                    neutka.marallys.marallyzen.npc.DialogScriptLoader.parseInitialAudioCommands(npcData.getDialogScript(), 1);
            
            // Check if step 1 has screen fade (after initial narration)
            var initialScreenFade = neutka.marallys.marallyzen.npc.DialogScriptLoader.parseInitialScreenFade(npcData.getDialogScript(), 1);
            
            if (initialNarrateMessages != null && !initialNarrateMessages.isEmpty()) {
                // Send initial narration messages before opening dialog
                String npcName = npcData.getName() != null ? npcData.getName() : npcData.getId();
                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) target.level();
                
                Marallyzen.LOGGER.info("NpcClickHandler: Found {} initial narrate messages for step 1, sending narration before opening dialog", 
                        initialNarrateMessages.size());
                
                // Create final callback to open dialog
                Runnable finalCallback = () -> {
                    // After narration/screen fade completes, open dialog
                    NetworkHelper.sendToPlayer(player, new OpenDialogPacket(
                            npcId,
                            npcName,
                            buttons,
                            target.getUUID()
                    ));
                    
                    // Mark that dialog is open for this player (step 1)
                    neutka.marallys.marallyzen.npc.NpcProximityHandler.setPlayerDialogOpen(player.getUUID(), target.getUUID(), 1);
                    
                    // Send proximity (navigation message) when dialog opens
                    neutka.marallys.marallyzen.npc.NpcProximityHandler.sendProximityForOpenDialog(player, target, npcData, serverLevel);
                    
                    Marallyzen.LOGGER.info("NpcClickHandler: Opened dialog after initial narration/screen fade completed");
                };
                
                // If there's a screen fade, chain it: narration → screen fade → open dialog
                // Otherwise, just open dialog after narration
                Runnable onNarrationComplete;
                if (initialScreenFade != null) {
                    // Chain: narration → screen fade → open dialog
                    onNarrationComplete = () -> {
                        Marallyzen.LOGGER.info("NpcClickHandler: Narration complete, starting screen fade before opening dialog");
                        neutka.marallys.marallyzen.npc.NpcNarrateHandler.scheduleScreenFadeAfterNarration(player, initialScreenFade, serverLevel, finalCallback);
                    };
                } else {
                    // No screen fade - open dialog directly after narration
                    onNarrationComplete = finalCallback;
                }

                if (initialAudioList != null && !initialAudioList.isEmpty()) {
                    long totalAudioDurationMs = calculateTotalAudioDurationMs(initialAudioList);
                    if (totalAudioDurationMs > 0) {
                        initialDuration = (int) (totalAudioDurationMs / 50);
                    }
                    playInitialAudioSequentially(serverLevel, player, target, initialAudioList);
                }
                
                neutka.marallys.marallyzen.npc.NpcNarrateHandler.scheduleNarrateMessages(
                        player, initialNarrateMessages, initialDuration, npcName, target.getUUID(), serverLevel, onNarrationComplete);
            } else {
                // No initial narration - open dialog immediately
                NetworkHelper.sendToPlayer(player, new OpenDialogPacket(
                        npcId,
                        npcData.getName() != null ? npcData.getName() : "NPC",
                        buttons,
                        target.getUUID()
                ));
                
                // Mark that dialog is open for this player (step 1)
                neutka.marallys.marallyzen.npc.NpcProximityHandler.setPlayerDialogOpen(player.getUUID(), target.getUUID(), 1);
                
                // Send proximity (navigation message) when dialog opens
                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) target.level();
                neutka.marallys.marallyzen.npc.NpcProximityHandler.sendProximityForOpenDialog(player, target, npcData, serverLevel);
            }
        }

        // TODO: Trigger Denizen script if dialogScript is set
        // This will be implemented when Denizen event system is ready
    }

    private static long calculateTotalAudioDurationMs(List<neutka.marallys.marallyzen.npc.DialogScriptLoader.AudioData> audioList) {
        long totalMs = 0;
        for (var audio : audioList) {
            int metadataTicks = AudioMetadata.getDurationTicks(audio.filePath());
            if (metadataTicks > 0) {
                totalMs += metadataTicks * 50L;
            } else {
                totalMs += 3000L;
            }
        }
        return totalMs;
    }

    private static void playInitialAudioSequentially(net.minecraft.server.level.ServerLevel level, ServerPlayer player, Entity npcEntity,
                                                     List<neutka.marallys.marallyzen.npc.DialogScriptLoader.AudioData> audioList) {
        if (level == null || player == null || audioList == null || audioList.isEmpty()) {
            return;
        }
        final List<neutka.marallys.marallyzen.npc.DialogScriptLoader.AudioData> finalAudioList = new java.util.ArrayList<>(audioList);
        java.util.function.Consumer<Integer> playNextAudio = new java.util.function.Consumer<Integer>() {
            @Override
            public void accept(Integer index) {
                if (index >= finalAudioList.size()) {
                    return;
                }
                var audio = finalAudioList.get(index);
                Vec3 audioPosition = null;
                if ("npc".equals(audio.source()) && npcEntity != null) {
                    audioPosition = npcEntity.position();
                } else if ("player".equals(audio.source())) {
                    audioPosition = player.position();
                } else if (audio.source() != null && audio.source().startsWith("position:")) {
                    String posStr = audio.source().substring("position:".length());
                    String[] coords = posStr.replaceAll("[\\[\\]]", "").split(",");
                    if (coords.length == 3) {
                        try {
                            double x = Double.parseDouble(coords[0].trim());
                            double y = Double.parseDouble(coords[1].trim());
                            double z = Double.parseDouble(coords[2].trim());
                            audioPosition = new Vec3(x, y, z);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                long audioDurationMs;
                if (audio.positional() && audioPosition != null) {
                    audioDurationMs = MarallyzenAudioService.playNpcAudio(
                            level,
                            npcEntity != null ? npcEntity : player,
                            audio.filePath(),
                            audio.radius(),
                            java.util.Collections.singletonList(player)
                    );
                } else {
                    audioDurationMs = MarallyzenAudioService.playGlobalAudio(
                            level,
                            audio.filePath(),
                            java.util.Collections.singletonList(player)
                    );
                }

                long delayMs = audioDurationMs;
                if (delayMs <= 0) {
                    int metadataTicks = AudioMetadata.getDurationTicks(audio.filePath());
                    delayMs = metadataTicks > 0 ? metadataTicks * 50L : 3000L;
                }
                if (index + 1 < finalAudioList.size()) {
                    java.util.Timer timer = new java.util.Timer();
                    long finalDelayMs = delayMs;
                    timer.schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            level.getServer().execute(() -> accept(index + 1));
                        }
                    }, finalDelayMs + 50);
                }
            }
        };
        playNextAudio.accept(0);
    }
}
