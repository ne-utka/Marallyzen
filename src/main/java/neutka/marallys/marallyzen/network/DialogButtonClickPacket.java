package neutka.marallys.marallyzen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * C2S packet: Sent when a player clicks a button in a dialog GUI.
 */
public record DialogButtonClickPacket(String dialogId, String buttonId, UUID npcEntityUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DialogButtonClickPacket> TYPE = new CustomPacketPayload.Type<>(MarallyzenNetwork.id("dialog_button_click"));
    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, DialogButtonClickPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(
            NetworkCodecs.STRING,
            DialogButtonClickPacket::dialogId,
            NetworkCodecs.STRING,
            DialogButtonClickPacket::buttonId,
            OpenDialogPacket.UUID_CODEC,
            DialogButtonClickPacket::npcEntityUuid,
            DialogButtonClickPacket::new
    );

    @Override
    public CustomPacketPayload.Type<DialogButtonClickPacket> type() {
        return TYPE;
    }

    public static void handle(DialogButtonClickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            
            // Play poke animation for the player and nearby players when selecting a dialog option
            neutka.marallys.marallyzen.npc.NpcAnimationHandler.sendAnimationToNearbyPlayers(
                    player,
                    "SPE_Poke",
                    32 // 32 block radius
            );
            
            neutka.marallys.marallyzen.npc.NpcRegistry registry = neutka.marallys.marallyzen.npc.NpcClickHandler.getRegistry();
            net.minecraft.world.entity.Entity npcEntity = registry.getNpcByUuid(packet.npcEntityUuid());
            if (npcEntity == null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("Dialog click: NPC not found for UUID {}", packet.npcEntityUuid());
                return;
            }
            String npcId = registry.getNpcId(npcEntity);
            if (npcId == null) {
                return;
            }
            neutka.marallys.marallyzen.npc.NpcData npcData = registry.getNpcData(npcId);
            if (npcData == null || npcData.getDialogScript() == null || npcData.getDialogScript().isEmpty()) {
                return;
            }

            // Get current step for this player BEFORE closing dialog (otherwise step will be lost)
            int currentStep = neutka.marallys.marallyzen.npc.NpcProximityHandler.getPlayerDialogStep(player.getUUID());
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Button '{}' clicked, current step: {} (player: {})", 
                    packet.buttonId(), currentStep, player.getName().getString());
            
            // Load dialog options to resolve emote binding and narrate messages
            var dialogOptions = neutka.marallys.marallyzen.npc.DialogScriptLoader.loadDialogOptions(npcData.getDialogScript(), currentStep);
            if (dialogOptions == null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("DialogButtonClickPacket: Failed to load dialog options for script '{}' at step {}", npcData.getDialogScript(), currentStep);
                return;
            }
            
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Button '{}' clicked, nextSteps map: {}", packet.buttonId(), dialogOptions.nextSteps());
            
            // Send state change to EXECUTING (dialog window should hide, narration/animation will start)
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Sending EXECUTING state to player {}", player.getName().getString());
            neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                    player, 
                    new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                            neutka.marallys.marallyzen.client.gui.DialogState.EXECUTING
                    )
            );
            
            // Mark that dialog is closed for this player (button was clicked)
            // But keep the step for now, we'll update it when opening next dialog
            neutka.marallys.marallyzen.npc.NpcProximityHandler.setPlayerDialogClosed(player.getUUID());
            
            // Send narrate messages if available
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Looking for narrate messages for buttonId='{}', available keys: {}", 
                    packet.buttonId(), dialogOptions.narrateMessages().keySet());
            var narrateMessages = dialogOptions.narrateMessages().get(packet.buttonId());
            if (narrateMessages != null && !narrateMessages.isEmpty()) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Found {} narrate messages for button '{}'", 
                        narrateMessages.size(), packet.buttonId());
                var narrateExpressions = dialogOptions.expressions().get(packet.buttonId());
                // Use case-specific duration if available, otherwise use default duration from script
                int duration = dialogOptions.narrateDurations().getOrDefault(packet.buttonId(), dialogOptions.defaultDuration());
                String npcName = npcData.getName() != null ? npcData.getName() : npcData.getId();
                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) npcEntity.level();

                neutka.marallys.marallyzen.npc.NpcExpressionManager.applyTalkExpression(npcEntity, npcData);
                
                // Check if there's a nested dialog or next step to open after narrate completes
                var nestedDialog = dialogOptions.nestedDialogs() != null ? dialogOptions.nestedDialogs().get(packet.buttonId()) : null;
                Integer nextStep = dialogOptions.nextSteps().get(packet.buttonId());
                var screenFade = dialogOptions.screenFades() != null ? dialogOptions.screenFades().get(packet.buttonId()) : null;
                var eyesClose = dialogOptions.eyesCloses() != null ? dialogOptions.eyesCloses().get(packet.buttonId()) : null;
                var itemEquip = dialogOptions.itemEquips() != null ? dialogOptions.itemEquips().get(packet.buttonId()) : null;
                var audioData = dialogOptions.audioData() != null ? dialogOptions.audioData().get(packet.buttonId()) : null;
                var audioDataList = dialogOptions.audioDataList() != null ? dialogOptions.audioDataList().get(packet.buttonId()) : null;
                
                // Use audioDataList if available (supports multiple audio files), otherwise fall back to single audioData
                if (audioDataList == null && audioData != null) {
                    audioDataList = java.util.Collections.singletonList(audioData);
                }
                
                // If audio is present AND narration is present, use audio duration for narration
                // This ensures narration syncs with audio playback, ignoring script duration
                if (audioDataList != null && !audioDataList.isEmpty() && narrateMessages != null && !narrateMessages.isEmpty()) {
                    // Calculate total duration from all audio files
                    long totalAudioDurationMs = 0;
                    for (var audio : audioDataList) {
                        int metadataDurationTicks = neutka.marallys.marallyzen.audio.AudioMetadata.getDurationTicks(audio.filePath());
                        if (metadataDurationTicks > 0) {
                            totalAudioDurationMs += metadataDurationTicks * 50L; // Convert ticks to ms
                        }
                    }
                    if (totalAudioDurationMs > 0) {
                        duration = (int) (totalAudioDurationMs / 50); // Convert ms to ticks
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Using total audio duration {} ticks for narration (from {} audio file(s)), ignoring script duration {}", 
                                duration, audioDataList.size(), dialogOptions.narrateDurations().getOrDefault(packet.buttonId(), dialogOptions.defaultDuration()));
                    }
                }
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Button '{}' clicked, hasNested={}, nextStep: {}, hasScreenFade: {}, hasEyesClose: {}, hasItemEquip: {}, hasAudio: {} ({} file(s))", 
                        packet.buttonId(), nestedDialog != null, nextStep, screenFade != null, eyesClose != null, itemEquip != null, 
                        audioDataList != null && !audioDataList.isEmpty(), audioDataList != null ? audioDataList.size() : 0);
                
                // Execute item equip command if present
                if (itemEquip != null && npcEntity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                    try {
                        net.minecraft.resources.ResourceLocation itemLocation = net.minecraft.resources.ResourceLocation.parse(itemEquip.itemId());
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(itemLocation).ifPresentOrElse(
                                item -> {
                                    net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(item);
                                    
                                    // Use setItemSlot for all LivingEntity types
                                    net.minecraft.world.entity.EquipmentSlot slot = itemEquip.hand().equals("mainhand") 
                                            ? net.minecraft.world.entity.EquipmentSlot.MAINHAND 
                                            : net.minecraft.world.entity.EquipmentSlot.OFFHAND;
                                    livingEntity.setItemSlot(slot, itemStack);
                                    
                                    // For ServerPlayer, send equipment update packet to nearby players
                                    if (livingEntity instanceof net.minecraft.server.level.ServerPlayer fakePlayer) {
                                        java.util.List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> equipment = new java.util.ArrayList<>();
                                        equipment.add(com.mojang.datafixers.util.Pair.of(slot, itemStack));
                                        net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket equipmentPacket = 
                                                new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(fakePlayer.getId(), equipment);
                                        
                                        // Send to all nearby players (use existing serverLevel variable)
                                        for (net.minecraft.server.level.ServerPlayer nearbyPlayer : serverLevel.players()) {
                                            if (nearbyPlayer != fakePlayer && nearbyPlayer.distanceToSqr(fakePlayer) < 64 * 64) {
                                                nearbyPlayer.connection.send(equipmentPacket);
                                            }
                                        }
                                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Equipped item '{}' to NPC {} (ServerPlayer) in {} hand and synced to nearby players", 
                                                itemEquip.itemId(), npcId, itemEquip.hand());
                                    } else {
                                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Equipped item '{}' to NPC {} (LivingEntity) in {} hand", 
                                                itemEquip.itemId(), npcId, itemEquip.hand());
                                    }
                                },
                                () -> neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("DialogButtonClickPacket: Item '{}' not found in registry", itemEquip.itemId())
                        );
                    } catch (Exception e) {
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.error("DialogButtonClickPacket: Failed to equip item '{}' to NPC", itemEquip.itemId(), e);
                    }
                }
                
                // Execute audio commands if present (support multiple audio files)
                long totalAudioDurationMs = 0;
                if (audioDataList != null && !audioDataList.isEmpty()) {
                    // Calculate total duration from all audio files for narration sync
                    for (var audio : audioDataList) {
                        int metadataDurationTicks = neutka.marallys.marallyzen.audio.AudioMetadata.getDurationTicks(audio.filePath());
                        if (metadataDurationTicks > 0) {
                            totalAudioDurationMs += metadataDurationTicks * 50L; // Convert ticks to ms
                        } else {
                            // Fallback: assume 3 seconds if metadata not available
                            totalAudioDurationMs += 3000;
                        }
                    }
                    
                    // Play all audio files sequentially using recursive callback approach
                    final java.util.List<neutka.marallys.marallyzen.npc.DialogScriptLoader.AudioData> finalAudioList = new java.util.ArrayList<>(audioDataList);
                    final int[] currentIndex = {0};
                    
                    java.util.function.Consumer<Integer> playNextAudio = new java.util.function.Consumer<Integer>() {
                        @Override
                        public void accept(Integer index) {
                            if (index >= finalAudioList.size()) {
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Finished playing all {} audio file(s) for button '{}'", 
                                        finalAudioList.size(), packet.buttonId());
                                return;
                            }
                            
                            var audio = finalAudioList.get(index);
                            try {
                                net.minecraft.world.phys.Vec3 audioPosition = null;
                                if (audio.source().equals("npc") && npcEntity != null) {
                                    audioPosition = npcEntity.position();
                                } else if (audio.source().equals("player")) {
                                    audioPosition = player.position();
                                } else if (audio.source().startsWith("position:")) {
                                    // Parse position from source string (format: "position:[x,y,z]")
                                    String posStr = audio.source().substring("position:".length());
                                    String[] coords = posStr.replaceAll("[\\[\\]]", "").split(",");
                                    if (coords.length == 3) {
                                        try {
                                            double x = Double.parseDouble(coords[0].trim());
                                            double y = Double.parseDouble(coords[1].trim());
                                            double z = Double.parseDouble(coords[2].trim());
                                            audioPosition = new net.minecraft.world.phys.Vec3(x, y, z);
                                        } catch (NumberFormatException e) {
                                            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("DialogButtonClickPacket: Invalid position format in audio source: {}", audio.source());
                                        }
                                    }
                                }
                                
                                // Play audio
                                long audioDurationMs = -1;
                                if (audio.positional() && audioPosition != null) {
                                    audioDurationMs = neutka.marallys.marallyzen.audio.MarallyzenAudioService.playNpcAudio(
                                            serverLevel,
                                            npcEntity != null ? npcEntity : player,
                                            audio.filePath(),
                                            audio.radius(),
                                            java.util.Collections.singletonList(player)
                                    );
                                } else {
                                    audioDurationMs = neutka.marallys.marallyzen.audio.MarallyzenAudioService.playGlobalAudio(
                                            serverLevel,
                                            audio.filePath(),
                                            java.util.Collections.singletonList(player)
                                    );
                                }
                                
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Playing audio {}/{} '{}' for button '{}', duration={}ms, blocking={}", 
                                        index + 1, finalAudioList.size(), audio.filePath(), packet.buttonId(), audioDurationMs, audio.blocking());
                                
                                // Schedule next audio after current one finishes
                                final long finalAudioDurationMs = audioDurationMs; // Make final for lambda
                                if (finalAudioDurationMs > 0 && index + 1 < finalAudioList.size()) {
                                    // Schedule next audio after current audio duration
                                    int delayTicks = (int) (finalAudioDurationMs / 50);
                                    serverLevel.getServer().execute(() -> {
                                        // Wait for audio to finish, then play next
                                        java.util.Timer timer = new java.util.Timer();
                                        timer.schedule(new java.util.TimerTask() {
                                            @Override
                                            public void run() {
                                                serverLevel.getServer().execute(() -> accept(index + 1));
                                            }
                                        }, finalAudioDurationMs + 50); // Small delay to ensure previous audio source is removed
                                    });
                                } else if (index + 1 < finalAudioList.size()) {
                                    // If duration unknown, use metadata or fallback
                                    int metadataDurationTicks = neutka.marallys.marallyzen.audio.AudioMetadata.getDurationTicks(audio.filePath());
                                    long delayMs = metadataDurationTicks > 0 ? metadataDurationTicks * 50L : 3000;
                                    java.util.Timer timer = new java.util.Timer();
                                    timer.schedule(new java.util.TimerTask() {
                                        @Override
                                        public void run() {
                                            serverLevel.getServer().execute(() -> accept(index + 1));
                                        }
                                    }, delayMs + 50);
                                }
                            } catch (Exception e) {
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.error("DialogButtonClickPacket: Failed to play audio '{}'", audio.filePath(), e);
                                // Continue with next audio even if current one failed
                                if (index + 1 < finalAudioList.size()) {
                                    serverLevel.getServer().execute(() -> accept(index + 1));
                                }
                            }
                        }
                    };
                    
                    // Start playing first audio
                    playNextAudio.accept(0);
                    
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Started sequential playback of {} audio file(s) for button '{}', total estimated duration={}ms", 
                            audioDataList.size(), packet.buttonId(), totalAudioDurationMs);
                    
                    // If audio duration was obtained and we have narration from script, update narration duration
                    if (totalAudioDurationMs > 0 && narrateMessages != null && !narrateMessages.isEmpty()) {
                        int playbackDurationTicks = (int) (totalAudioDurationMs / 50); // Convert ms to ticks
                        duration = playbackDurationTicks; // Use total audio duration for narration from script
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Using total audio duration {} ticks for narration from script (from {} audio file(s))", duration, audioDataList.size());
                    }
                }
                
                // If we have both audio and narration, ensure they start simultaneously
                // Narration should start immediately (delay=0) when audio starts
                final long finalAudioDurationMs = totalAudioDurationMs;
                final boolean hasAudio = audioDataList != null && !audioDataList.isEmpty() && finalAudioDurationMs > 0;
                if (hasAudio && narrateMessages != null && !narrateMessages.isEmpty()) {
                    // Update duration to match audio if not already updated
                    if (duration == dialogOptions.narrateDurations().getOrDefault(packet.buttonId(), dialogOptions.defaultDuration())) {
                        int audioDurationTicks = (int) (finalAudioDurationMs / 50);
                        if (audioDurationTicks > 0) {
                            duration = audioDurationTicks;
                            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Synchronizing narration with audio - using audio duration {} ticks", duration);
                        }
                    }
                }
                
                Runnable onComplete = null;
                
                // Create the final callback (after screen fade, if any)
                final Runnable[] finalCallbackRef = new Runnable[1];
                
                // If audio is blocking, create a callback that waits for audio to finish
                // finalAudioDurationMs is already defined above for narration synchronization
                final boolean audioBlocking = audioData != null && audioData.blocking();
                
                // Priority: nested dialog > next step
                if (nestedDialog != null && !nestedDialog.texts().isEmpty()) {
                    // Show nested dialog options after narration/screen fade
                    finalCallbackRef[0] = () -> {
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Opening nested dialog for button '{}' with {} options", 
                                packet.buttonId(), nestedDialog.texts().size());
                        neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(player, new neutka.marallys.marallyzen.network.OpenDialogPacket(
                                npcId,
                                npcName,
                                nestedDialog.texts(),
                                npcEntity.getUUID()
                        ));
                        // Keep current step for nested dialogs (they're part of the same step)
                        neutka.marallys.marallyzen.npc.NpcProximityHandler.sendProximityForOpenDialog(player, npcEntity, npcData, serverLevel);
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Sent OpenDialogPacket for nested dialog to player {}", player.getName().getString());
                    };
                } else if (nextStep != null && nextStep > 0) {
                    // Create callback to open next step dialog after narrate/screen fade completes
                    final int finalNextStep = nextStep;
                    finalCallbackRef[0] = () -> {
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Opening next step {} dialog for player {} (immediately after narration)", finalNextStep, player.getName().getString());
                        
                        // Check if next step has initial narration
                        java.util.Map.Entry<java.util.List<String>, Integer> initialNarrateData = 
                                neutka.marallys.marallyzen.npc.DialogScriptLoader.parseInitialNarrateMessages(npcData.getDialogScript(), finalNextStep);
                        java.util.List<String> initialNarrateMessages = initialNarrateData.getKey();
                        int initialDuration = initialNarrateData.getValue();
                        
                        if (initialNarrateMessages != null && !initialNarrateMessages.isEmpty()) {
                            // Show initial narration before opening dialog
                            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Found {} initial narrate messages for step {}, showing before dialog", 
                                    initialNarrateMessages.size(), finalNextStep);
                            
                            Runnable onInitialNarrationComplete = () -> {
                                // After initial narration completes, open dialog
                                var nextStepOptions = neutka.marallys.marallyzen.npc.DialogScriptLoader.loadDialogOptions(npcData.getDialogScript(), finalNextStep);
                                if (nextStepOptions != null && !nextStepOptions.texts().isEmpty()) {
                                    neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Loaded {} options for step {}", nextStepOptions.texts().size(), finalNextStep);
                                    neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(player, new neutka.marallys.marallyzen.network.OpenDialogPacket(
                                            npcId,
                                            npcName,
                                            nextStepOptions.texts(),
                                            npcEntity.getUUID()
                                    ));
                                    neutka.marallys.marallyzen.npc.NpcProximityHandler.setPlayerDialogOpen(player.getUUID(), npcEntity.getUUID(), finalNextStep);
                                    neutka.marallys.marallyzen.npc.NpcProximityHandler.sendProximityForOpenDialog(player, npcEntity, npcData, serverLevel);
                                    neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Sent OpenDialogPacket and proximity for step {} to player {}", finalNextStep, player.getName().getString());
                                } else {
                                    neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("DialogButtonClickPacket: Failed to load options for step {} or options are empty, closing dialog", finalNextStep);
                                    neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                                            player,
                                            new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                                    neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                                            )
                                    );
                                }
                            };
                            
                            neutka.marallys.marallyzen.npc.NpcNarrateHandler.scheduleNarrateMessages(
                                    player, initialNarrateMessages, initialDuration, npcName, npcEntity.getUUID(), serverLevel, onInitialNarrationComplete);
                        } else {
                            // No initial narration - open dialog immediately
                            var nextStepOptions = neutka.marallys.marallyzen.npc.DialogScriptLoader.loadDialogOptions(npcData.getDialogScript(), finalNextStep);
                            if (nextStepOptions != null && !nextStepOptions.texts().isEmpty()) {
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Loaded {} options for step {}", nextStepOptions.texts().size(), finalNextStep);
                                neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(player, new neutka.marallys.marallyzen.network.OpenDialogPacket(
                                        npcId,
                                        npcName,
                                        nextStepOptions.texts(),
                                        npcEntity.getUUID()
                                ));
                                neutka.marallys.marallyzen.npc.NpcProximityHandler.setPlayerDialogOpen(player.getUUID(), npcEntity.getUUID(), finalNextStep);
                                neutka.marallys.marallyzen.npc.NpcProximityHandler.sendProximityForOpenDialog(player, npcEntity, npcData, serverLevel);
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Sent OpenDialogPacket and proximity for step {} to player {}", finalNextStep, player.getName().getString());
                            } else {
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("DialogButtonClickPacket: Failed to load options for step {} or options are empty, closing dialog", finalNextStep);
                                neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                                        player,
                                        new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                                neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                                        )
                                );
                            }
                        }
                    };
                } else {
                    // No next step - close dialog after narration/screen fade
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: No next step for button '{}', will close dialog after narration/screen fade", packet.buttonId());
                    finalCallbackRef[0] = () -> {
                        neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                                player,
                                new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                        neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                                )
                        );
                    };
                }

                final Runnable originalFinalCallback = finalCallbackRef[0];
                finalCallbackRef[0] = () -> {
                    neutka.marallys.marallyzen.npc.NpcExpressionManager.applyDefaultExpression(npcEntity, npcData);
                    if (originalFinalCallback != null) {
                        originalFinalCallback.run();
                    }
                };
                
                // If audio is blocking, wrap final callback to wait for audio to finish
                if (audioBlocking && finalAudioDurationMs > 0) {
                    final Runnable originalCallback = finalCallbackRef[0];
                    finalCallbackRef[0] = () -> {
                        // Schedule callback after audio duration using server tick scheduler
                        int audioTicks = (int) (finalAudioDurationMs / 50); // Convert ms to ticks
                        if (audioTicks <= 0) {
                            audioTicks = 20; // Default to 1 second if duration unknown
                        }
                        // Schedule callback to run after audio duration
                        serverLevel.getServer().execute(() -> {
                            // Use a simple delay mechanism - schedule callback after audio duration
                            // Note: This is a simplified approach; for precise timing, consider using a tick-based scheduler
                            new java.util.Timer().schedule(new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    serverLevel.getServer().execute(originalCallback);
                                }
                            }, finalAudioDurationMs);
                        });
                    };
                }
                
                // If there's a cutscene (screen fade or eyes close), chain it: narration → cutscene → final callback
                // Otherwise, just use final callback directly
                if (eyesClose != null) {
                    // Chain: narration → eyes close → final callback
                    final Runnable finalCallback = finalCallbackRef[0];
                    Runnable narrationCompleteCallback = () -> {
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Narration complete, starting eyes close cutscene for button '{}'", packet.buttonId());
                        neutka.marallys.marallyzen.npc.NpcNarrateHandler.scheduleEyesCloseAfterNarration(player, eyesClose, serverLevel, finalCallback);
                    };
                    onComplete = narrationCompleteCallback;
                } else if (screenFade != null) {
                    // Chain: narration → screen fade → final callback
                    final Runnable finalCallback = finalCallbackRef[0];
                    Runnable narrationCompleteCallback = () -> {
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Narration complete, starting screen fade for button '{}'", packet.buttonId());
                        neutka.marallys.marallyzen.npc.NpcNarrateHandler.scheduleScreenFadeAfterNarration(player, screenFade, serverLevel, finalCallback);
                    };
                    onComplete = narrationCompleteCallback;
                } else {
                    // No cutscene - use final callback directly
                    onComplete = finalCallbackRef[0];
                }
                
                neutka.marallys.marallyzen.npc.NpcNarrateHandler.scheduleNarrateMessages(
                        player, narrateMessages, narrateExpressions, duration, npcName, npcEntity.getUUID(), serverLevel, onComplete);
            } else {
                // No narrate messages - check if there's a nested dialog or next step
                var nestedDialog = dialogOptions.nestedDialogs() != null ? dialogOptions.nestedDialogs().get(packet.buttonId()) : null;
                Integer nextStep = dialogOptions.nextSteps().get(packet.buttonId());
                
                // Priority: nested dialog > next step
                if (nestedDialog != null && !nestedDialog.texts().isEmpty()) {
                    // Show nested dialog immediately
                    net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) npcEntity.level();
                    String npcName = npcData.getName() != null ? npcData.getName() : npcData.getId();
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: No narrate messages, opening nested dialog immediately for button '{}' with {} options", 
                            packet.buttonId(), nestedDialog.texts().size());
                    neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(player, new neutka.marallys.marallyzen.network.OpenDialogPacket(
                            npcId,
                            npcName,
                            nestedDialog.texts(),
                            npcEntity.getUUID()
                    ));
                    neutka.marallys.marallyzen.npc.NpcProximityHandler.sendProximityForOpenDialog(player, npcEntity, npcData, serverLevel);
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Sent OpenDialogPacket for nested dialog to player {}", player.getName().getString());
                } else if (nextStep != null && nextStep > 0) {
                    // Open next step - check for initial narration first
                    final int finalNextStep = nextStep;
                    net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) npcEntity.level();
                    String npcName = npcData.getName() != null ? npcData.getName() : npcData.getId();
                    
                    // Check if next step has initial narration (including step 1)
                    java.util.Map.Entry<java.util.List<String>, Integer> initialNarrateData = 
                            neutka.marallys.marallyzen.npc.DialogScriptLoader.parseInitialNarrateMessages(npcData.getDialogScript(), finalNextStep);
                    java.util.List<String> initialNarrateMessages = initialNarrateData.getKey();
                    int initialDuration = initialNarrateData.getValue();
                    
                    if (initialNarrateMessages != null && !initialNarrateMessages.isEmpty()) {
                        // Show initial narration before opening dialog (works for step 1 and any other step)
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Found {} initial narrate messages for step {}, showing before dialog", 
                                initialNarrateMessages.size(), finalNextStep);
                        
                        Runnable onInitialNarrationComplete = () -> {
                            var nextStepOptions = neutka.marallys.marallyzen.npc.DialogScriptLoader.loadDialogOptions(npcData.getDialogScript(), finalNextStep);
                            if (nextStepOptions != null && !nextStepOptions.texts().isEmpty()) {
                                neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(player, new neutka.marallys.marallyzen.network.OpenDialogPacket(
                                        npcId,
                                        npcName,
                                        nextStepOptions.texts(),
                                        npcEntity.getUUID()
                                ));
                                neutka.marallys.marallyzen.npc.NpcProximityHandler.setPlayerDialogOpen(player.getUUID(), npcEntity.getUUID(), finalNextStep);
                                neutka.marallys.marallyzen.npc.NpcProximityHandler.sendProximityForOpenDialog(player, npcEntity, npcData, serverLevel);
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Sent OpenDialogPacket for step {} to player {}", finalNextStep, player.getName().getString());
                            } else {
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("DialogButtonClickPacket: Failed to load options for step {} or options are empty, closing dialog", finalNextStep);
                                neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                                        player,
                                        new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                                neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                                        )
                                );
                            }
                        };
                        
                        neutka.marallys.marallyzen.npc.NpcNarrateHandler.scheduleNarrateMessages(
                                player, initialNarrateMessages, initialDuration, npcName, npcEntity.getUUID(), serverLevel, onInitialNarrationComplete);
                    } else {
                        // No initial narration - open dialog immediately
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: No narrate messages, opening next step {} dialog immediately for player {}", finalNextStep, player.getName().getString());
                        var nextStepOptions = neutka.marallys.marallyzen.npc.DialogScriptLoader.loadDialogOptions(npcData.getDialogScript(), finalNextStep);
                        if (nextStepOptions != null && !nextStepOptions.texts().isEmpty()) {
                            serverLevel.getServer().execute(() -> {
                                neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(player, new neutka.marallys.marallyzen.network.OpenDialogPacket(
                                        npcId,
                                        npcName,
                                        nextStepOptions.texts(),
                                        npcEntity.getUUID()
                                ));
                                neutka.marallys.marallyzen.npc.NpcProximityHandler.setPlayerDialogOpen(player.getUUID(), npcEntity.getUUID(), finalNextStep);
                                neutka.marallys.marallyzen.npc.NpcProximityHandler.sendProximityForOpenDialog(player, npcEntity, npcData, serverLevel);
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Sent OpenDialogPacket for step {} to player {}", finalNextStep, player.getName().getString());
                            });
                        } else {
                            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("DialogButtonClickPacket: Failed to load options for next step {} in script '{}' (no narrate messages), closing dialog", finalNextStep, npcData.getDialogScript());
                            neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                                    player,
                                    new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                            neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                                    )
                            );
                        }
                    }
                } else {
                    // No next step and no narrate - close dialog
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: No narrate messages and no next step for button '{}', closing dialog", packet.buttonId());
                    neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                            player,
                            new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                    neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                            )
                    );
                }
            }
            
            String emoteId = dialogOptions.emotes().get(packet.buttonId());
            if (emoteId == null || emoteId.isEmpty()) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("DialogButtonClickPacket: No emote bound to button '{}'", packet.buttonId());
                // Continue even if no emote - narrate messages are more important
            } else {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("DialogButtonClickPacket: Button '{}' clicked, emoteId: '{}', NPC: {}", 
                        packet.buttonId(), emoteId, npcEntity.getName().getString());

                // Mark that NPC is playing a non-default animation
                neutka.marallys.marallyzen.npc.NpcDefaultAnimationHandler.markNpcPlayingOtherAnimation(npcEntity);
                
                // Send animation to nearby players using new animation system
                // Default radius: 32 blocks (can be made configurable)
                neutka.marallys.marallyzen.npc.NpcAnimationHandler.sendAnimationToNearbyPlayers(
                        npcEntity, 
                        emoteId, 
                        32
                );
            }
        });
    }
    
}
