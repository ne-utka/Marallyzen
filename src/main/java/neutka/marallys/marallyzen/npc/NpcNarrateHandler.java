package neutka.marallys.marallyzen.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NarratePacket;
import neutka.marallys.marallyzen.network.NetworkHelper;

import java.util.*;

/**
 * Handles delayed narrate message sending for NPC dialogs.
 * Messages are sent sequentially with specified delays.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public class NpcNarrateHandler {
    
    /**
     * Represents a scheduled narrate message.
     */
    private static class ScheduledMessage {
        final ServerPlayer player;
        final Component message;
        final UUID npcUuid;
        final int remainingTicks;
        final int displayDuration; // How long this message should be displayed (in ticks)
        final String expression;
        final Runnable onComplete; // Callback when all messages are sent
        
        ScheduledMessage(ServerPlayer player, Component message, UUID npcUuid, int remainingTicks) {
            this(player, message, npcUuid, remainingTicks, 0, null, null);
        }
        
        ScheduledMessage(ServerPlayer player, Component message, UUID npcUuid, int remainingTicks, Runnable onComplete) {
            this(player, message, npcUuid, remainingTicks, 0, null, onComplete);
        }
        
        ScheduledMessage(ServerPlayer player, Component message, UUID npcUuid, int remainingTicks, int displayDuration, String expression, Runnable onComplete) {
            this.player = player;
            this.message = message;
            this.npcUuid = npcUuid;
            this.remainingTicks = remainingTicks;
            this.displayDuration = displayDuration;
            this.expression = expression;
            this.onComplete = onComplete;
        }
    }
    
    // Queue of scheduled messages for each player
    private static final Map<UUID, Queue<ScheduledMessage>> playerMessageQueues = new HashMap<>();
    
    // Track pending dialog opens after narrate completes
    private static final Map<UUID, Runnable> pendingDialogOpens = new HashMap<>();
    
    // Track when narration was last sent to each player (to prevent proximity from showing immediately)
    private static final Map<UUID, Integer> playerLastNarrationTick = new HashMap<>();
    
    // Track pending onComplete callbacks that should be executed when narration completes on client
    private static final Map<UUID, Runnable> pendingNarrationCompleteCallbacks = new HashMap<>();
    
    // Track pending onComplete callbacks that should be executed when screen fade completes on client
    private static final Map<UUID, Runnable> pendingScreenFadeCompleteCallbacks = new HashMap<>();
    
    // Track pending onComplete callbacks that should be executed when eyes close cutscene completes on client
    private static final Map<UUID, Runnable> pendingEyesCloseCompleteCallbacks = new HashMap<>();
    
    /**
     * Checks if a player currently has narrate messages being sent.
     * Used by NpcProximityHandler to prevent Custom HUD conflicts.
     */
    public static boolean isPlayerReceivingNarrate(UUID playerId) {
        Queue<ScheduledMessage> queue = playerMessageQueues.get(playerId);
        if (queue != null && !queue.isEmpty()) {
            return true;
        }
        return pendingNarrationCompleteCallbacks.containsKey(playerId);
    }
    
    /**
     * Checks if narration was recently sent to a player (within last N ticks).
     * Used to prevent proximity from showing immediately after narration.
     * 
     * @param playerId The player UUID
     * @param currentTick The current server tick
     * @param delayTicks Number of ticks to wait after narration before allowing proximity
     * @return true if narration was sent recently
     */
    public static boolean wasNarrationSentRecently(UUID playerId, int currentTick, int delayTicks) {
        Integer lastNarrationTick = playerLastNarrationTick.get(playerId);
        if (lastNarrationTick == null) {
            return false;
        }
        int ticksSinceNarration = currentTick - lastNarrationTick;
        return ticksSinceNarration < delayTicks;
    }
    
    /**
     * Called when client confirms that narration has completely finished (fade-out complete).
     * Executes the pending onComplete callback if one exists.
     * 
     * @param playerId The player UUID
     */
    public static void onNarrationComplete(UUID playerId) {
        Runnable onComplete = pendingNarrationCompleteCallbacks.remove(playerId);
        if (onComplete != null) {
            Marallyzen.LOGGER.info("NpcNarrateHandler: Narration complete confirmed by client for player {}, executing onComplete callback", playerId);
            onComplete.run();
        } else {
            Marallyzen.LOGGER.debug("NpcNarrateHandler: Narration complete confirmed for player {}, but no pending callback", playerId);
        }
    }
    
    /**
     * Called when client confirms that screen fade has completely finished (fade-in complete).
     * Executes the pending onComplete callback if one exists.
     * 
     * @param playerId The player UUID
     */
    public static void onScreenFadeComplete(UUID playerId) {
        Runnable onComplete = pendingScreenFadeCompleteCallbacks.remove(playerId);
        if (onComplete != null) {
            Marallyzen.LOGGER.info("NpcNarrateHandler: Screen fade complete confirmed by client for player {}, executing onComplete callback", playerId);
            onComplete.run();
        } else {
            Marallyzen.LOGGER.debug("NpcNarrateHandler: Screen fade complete confirmed for player {}, but no pending callback", playerId);
        }
    }
    
    /**
     * Schedules a screen fade to be played after narration completes.
     * 
     * @param player The player
     * @param screenFadeData The screen fade data
     * @param serverLevel The server level (for parsing JSON components)
     * @param onComplete Callback to execute when screen fade completes (can be null)
     */
    public static void scheduleScreenFadeAfterNarration(ServerPlayer player, DialogScriptLoader.ScreenFadeData screenFadeData, ServerLevel serverLevel, Runnable onComplete) {
        if (screenFadeData == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Store callback to execute when screen fade completes
        if (onComplete != null) {
            pendingScreenFadeCompleteCallbacks.put(playerId, onComplete);
        }
        
        // Parse title and subtitle as JSON components if they look like JSON
        Component titleText = parseTextComponent(screenFadeData.titleText(), serverLevel);
        Component subtitleText = parseTextComponent(screenFadeData.subtitleText(), serverLevel);
        
        // Send screen fade packet to client
        neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(player, 
                new neutka.marallys.marallyzen.network.ScreenFadePacket(
                        screenFadeData.fadeOutTicks(),
                        screenFadeData.blackScreenTicks(),
                        screenFadeData.fadeInTicks(),
                        titleText,
                        subtitleText,
                        screenFadeData.blockPlayerInput(),
                        screenFadeData.soundId()
                ));
        
        Marallyzen.LOGGER.info("NpcNarrateHandler: Scheduled screen fade for player {} after narration (fadeOut={}t, blackScreen={}t, fadeIn={}t)", 
                player.getName().getString(), screenFadeData.fadeOutTicks(), screenFadeData.blackScreenTicks(), screenFadeData.fadeInTicks());
    }
    
    /**
     * Called when client confirms that eyes close cutscene has completely finished.
     * Executes the pending onComplete callback if one exists.
     * 
     * @param playerId The player UUID
     */
    public static void onEyesCloseComplete(UUID playerId) {
        Runnable onComplete = pendingEyesCloseCompleteCallbacks.remove(playerId);
        if (onComplete != null) {
            Marallyzen.LOGGER.info("NpcNarrateHandler: Eyes close cutscene complete confirmed by client for player {}, executing onComplete callback", playerId);
            onComplete.run();
        } else {
            Marallyzen.LOGGER.debug("NpcNarrateHandler: Eyes close cutscene complete confirmed for player {}, but no pending callback", playerId);
        }
    }
    
    /**
     * Schedules an eyes close cutscene to be played after narration completes.
     * 
     * @param player The player to send the cutscene to
     * @param eyesCloseData The eyes close cutscene parameters
     * @param serverLevel The server level
     * @param onComplete Callback to execute when eyes close cutscene completes (can be null)
     */
    public static void scheduleEyesCloseAfterNarration(ServerPlayer player, DialogScriptLoader.EyesCloseData eyesCloseData, ServerLevel serverLevel, Runnable onComplete) {
        if (eyesCloseData == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Store callback to execute when eyes close cutscene completes
        if (onComplete != null) {
            pendingEyesCloseCompleteCallbacks.put(playerId, onComplete);
        }
        
        // Send eyes close packet to client
        neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(player, 
                new neutka.marallys.marallyzen.network.EyesClosePacket(
                        eyesCloseData.closeDurationTicks(),
                        eyesCloseData.blackDurationTicks(),
                        eyesCloseData.openDurationTicks(),
                        eyesCloseData.lockPlayer()
                ));
        
        Marallyzen.LOGGER.info("NpcNarrateHandler: Scheduled eyes close cutscene for player {} after narration (close={}t, black={}t, open={}t)", 
                player.getName().getString(), eyesCloseData.closeDurationTicks(), eyesCloseData.blackDurationTicks(), eyesCloseData.openDurationTicks());
    }
    
    /**
     * Schedules narrate messages to be sent to a player sequentially with specified duration.
     * Messages will be prefixed with NPC name and ">>" (dark gray).
     * 
     * @param player The player to send messages to
     * @param messages List of messages to send
     * @param totalDurationTicks Total duration in ticks for all messages
     * @param npcName The NPC name (can be JSON formatted with color)
     * @param serverLevel The server level (for parsing JSON components)
     * @param onComplete Callback to execute when all messages are sent (can be null)
     */
    public static void scheduleNarrateMessages(ServerPlayer player, List<String> messages, int totalDurationTicks, String npcName, ServerLevel serverLevel, Runnable onComplete) {
        scheduleNarrateMessages(player, messages, null, totalDurationTicks, npcName, null, serverLevel, onComplete);
    }
    
    /**
     * Schedules narrate messages to be sent to a player sequentially with specified duration.
     * Messages will be prefixed with NPC name and ">>" (dark gray).
     * 
     * @param player The player to send messages to
     * @param messages List of messages to send
     * @param totalDurationTicks Total duration in ticks for all messages
     * @param npcName The NPC name (can be JSON formatted with color)
     * @param npcUuid The NPC UUID (can be null)
     * @param serverLevel The server level (for parsing JSON components)
     * @param onComplete Callback to execute when all messages are sent (can be null)
     */
    public static void scheduleNarrateMessages(ServerPlayer player, List<String> messages, int totalDurationTicks, String npcName, UUID npcUuid, ServerLevel serverLevel, Runnable onComplete) {
        scheduleNarrateMessages(player, messages, null, totalDurationTicks, npcName, npcUuid, serverLevel, onComplete);
    }

    public static void scheduleNarrateMessages(ServerPlayer player, List<String> messages, List<String> expressions, int totalDurationTicks, String npcName, UUID npcUuid, ServerLevel serverLevel, Runnable onComplete) {
        if (messages == null || messages.isEmpty() || totalDurationTicks <= 0) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Clear any existing messages for this player
        playerMessageQueues.remove(playerId);
        
        // Force NPC name color to 0xD48E03 regardless of scripted color
        String displayName = parseTextComponent(npcName, serverLevel).getString();
        Component npcNameComponent = Component.literal(displayName).withStyle(style ->
            style.withColor(TextColor.fromRgb(0xD48E03)));
        
        // Create "»" separator in dark gray
        final TextColor darkGrayColor = TextColor.fromRgb(0x555555);
        Component separator = Component.literal(" » ").withStyle(style -> style.withColor(darkGrayColor));
        
        // Calculate timing for messages
        // Fade timings (same as used in client)
        int fadeInTicks = 5;
        int fadeOutTicks = 3;
        
        // If we have multiple messages, distribute the total duration between them
        // This ensures synchronization with audio: all messages together take the full audio duration
        int perMessageDuration;
        int delayBetweenMessages;
        
        if (messages.size() > 1) {
            // Multiple messages: distribute total duration between them sequentially
            // Total duration = sum of all message display times
            // Each message: fadeIn + stay + fadeOut
            // We need: (fadeIn + stay + fadeOut) * messageCount <= totalDuration
            // So: stay = (totalDuration / messageCount) - fadeIn - fadeOut
            int totalAvailableForStay = totalDurationTicks - (fadeInTicks + fadeOutTicks) * messages.size();
            int stayPerMessage = Math.max(20, totalAvailableForStay / messages.size()); // At least 1 second stay per message
            perMessageDuration = fadeInTicks + stayPerMessage + fadeOutTicks;
            // Next message starts when previous one completely finishes (no overlap)
            delayBetweenMessages = perMessageDuration;
        } else {
            // Single message: use full duration
            perMessageDuration = Math.max(60, totalDurationTicks); // At least 3 seconds for single message
            delayBetweenMessages = 0; // Not used for single message
        }
        
        // Calculate total display time per message: fadeIn + stay + fadeOut
        int stayTicks = Math.max(0, perMessageDuration - fadeInTicks - fadeOutTicks);
        int totalDisplayTime = fadeInTicks + stayTicks + fadeOutTicks;
        
        Marallyzen.LOGGER.info("NpcNarrateHandler: Scheduling {} messages for player {} with totalDuration={} ticks, perMessageDuration={} ticks, fadeIn={}, stay={}, fadeOut={}, totalDisplayTime={}, delayBetweenMessages={} ticks", 
                messages.size(), player.getName().getString(), totalDurationTicks, perMessageDuration, fadeInTicks, stayTicks, fadeOutTicks, totalDisplayTime, delayBetweenMessages);
        
        // Create queue of scheduled messages with formatted prefix
        // Messages are scheduled to start simultaneously with audio (first message at delay=0)
        Queue<ScheduledMessage> queue = new ArrayDeque<>();
        int cumulativeDelay = 0; // First message starts immediately (delay=0) to sync with audio
        for (int i = 0; i < messages.size(); i++) {
            // Format message: "NPC Name » message text"
            // Text after "»" should be white
            final TextColor whiteColor = TextColor.fromRgb(0xFFFFFF);
            Component formattedMessage = npcNameComponent.copy()
                .append(separator)
                .append(Component.literal(messages.get(i)).withStyle(style -> style.withColor(whiteColor)));
            // Only the last message should have the onComplete callback
            Runnable callback = (i == messages.size() - 1) ? onComplete : null;
            
            // Use calculated per-message duration
            int messageDisplayDuration = perMessageDuration;
            String expression = resolveExpression(expressions, i);
            
            // Schedule message send
            queue.offer(new ScheduledMessage(player, formattedMessage, npcUuid, cumulativeDelay, messageDisplayDuration, expression, callback));
            Marallyzen.LOGGER.debug("NpcNarrateHandler: Scheduled message {} with delay {} ticks, displayDuration {} ticks: {}", 
                    i, cumulativeDelay, messageDisplayDuration, messages.get(i));
            
            // Calculate delay for next message
            if (i < messages.size() - 1) {
                cumulativeDelay += delayBetweenMessages;
            }
        }
        
        playerMessageQueues.put(playerId, queue);
        
        // Store onComplete callback if provided
        if (onComplete != null) {
            pendingDialogOpens.put(playerId, onComplete);
        }
    }
    
    /**
     * Parses a text component from JSON string or plain text.
     * Supports JSON format: {"text":"Name","color":"blue"}
     */
    private static Component parseTextComponent(String text, ServerLevel level) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Try to parse as JSON first
        if (text.trim().startsWith("{") || text.trim().startsWith("[")) {
            try {
                return Component.Serializer.fromJson(text, level.registryAccess());
            } catch (Exception e) {
                // If JSON parsing fails, treat as plain text
                Marallyzen.LOGGER.debug("Failed to parse text as JSON, using plain text: " + text, e);
            }
        }
        
        // Return as plain text
        return Component.literal(text);
    }
    
    /**
     * Server tick event handler.
     * Processes scheduled narrate messages.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() == null) {
            return;
        }
        
        // Process each player's message queue
        // Use iterator to safely modify map during iteration
        Iterator<Map.Entry<UUID, Queue<ScheduledMessage>>> iterator = playerMessageQueues.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, Queue<ScheduledMessage>> entry = iterator.next();
            UUID playerId = entry.getKey();
            Queue<ScheduledMessage> queue = entry.getValue();
            
            // Find the player
            ServerPlayer player = null;
            for (var level : event.getServer().getAllLevels()) {
                player = level.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    break;
                }
            }
            
            // Remove queue if player is offline
            if (player == null || !player.isAlive() || player.hasDisconnected()) {
                iterator.remove();
                continue;
            }
            
            // Process messages in queue
            // Decrease remaining ticks for ALL messages in queue each tick
            // But only send the first message that's ready (remainingTicks == 0)
            boolean messageSent = false;
            Queue<ScheduledMessage> updatedQueue = new ArrayDeque<>();
            
            // Process all messages in queue
            while (!queue.isEmpty()) {
                ScheduledMessage scheduled = queue.poll();
                
                if (scheduled.remainingTicks == 0 && !messageSent) {
                    // Time to send this message (only send one per tick)
                    if (player.isAlive() && !player.hasDisconnected() && player.connection != null) {
                        if (scheduled.expression != null && !scheduled.expression.isBlank() && scheduled.npcUuid != null) {
                            Entity npcEntity = NpcClickHandler.getRegistry().getNpcByUuid(scheduled.npcUuid);
                            NpcExpressionManager.setExpression(npcEntity, scheduled.expression);
                        }
                        // Calculate fade-in, stay, and fade-out timings
                        // fadeIn and fadeOut match dialog animation speed (5 ticks fade-in, 3 ticks fade-out)
                        // stayTicks is calculated from script duration: displayDuration - fadeIn - fadeOut
                        // IMPORTANT: Use consistent fade timings (5/3) to match client-side rendering
                        int fadeInTicks = 5; // Match dialog FADE_IN_DURATION_TICKS (0.25 seconds)
                        int fadeOutTicks = 3; // Match dialog FADE_OUT_DURATION_TICKS (0.15 seconds)
                        int stayTicks = Math.max(0, scheduled.displayDuration - fadeInTicks - fadeOutTicks);
                        
                        // Ensure minimum stay time for readability (at least 20 ticks = 1 second)
                        if (stayTicks < 20 && scheduled.displayDuration >= (fadeInTicks + fadeOutTicks + 20)) {
                            // If stay time is too short but we have enough ticks, increase stay time
                            stayTicks = Math.max(20, scheduled.displayDuration - fadeInTicks - fadeOutTicks);
                        } else if (stayTicks < 0) {
                            // If duration is too short for fade-in + fade-out, use minimum fade times
                            int availableTicks = scheduled.displayDuration;
                            fadeInTicks = Math.min(5, Math.max(2, availableTicks / 3)); // At least 2 ticks, max 5
                            fadeOutTicks = Math.min(3, Math.max(1, availableTicks / 3)); // At least 1 tick, max 3
                            stayTicks = Math.max(0, availableTicks - fadeInTicks - fadeOutTicks);
                        }
                        
                        // Send narration packet to client
                        String messageStr = scheduled.message != null ? scheduled.message.getString() : "null";
                        Marallyzen.LOGGER.info("[NpcNarrateHandler] SERVER: Sending NarratePacket to player {} - text='{}', npcUuid={}, displayDuration={} (from script), fadeIn={} (HARDCODED), stay={} (calculated), fadeOut={} (HARDCODED), remaining in queue: {}", 
                                player.getName().getString(), messageStr, scheduled.npcUuid, scheduled.displayDuration, fadeInTicks, stayTicks, fadeOutTicks, queue.size() + updatedQueue.size());
                        
                        NarratePacket packet = new NarratePacket(
                                scheduled.message,
                                scheduled.npcUuid,
                                fadeInTicks,
                                stayTicks,
                                fadeOutTicks
                        );
                        NetworkHelper.sendToPlayer(player, packet);
                        
                        // Track when narration was sent (for proximity delay)
                        if (event.getServer() != null && event.getServer().overworld() != null) {
                            int currentTick = (int) event.getServer().overworld().getGameTime();
                            playerLastNarrationTick.put(playerId, currentTick);
                        }
                        
                        // Check if this is the last message (no more messages in queue)
                        // Note: onComplete will be called when queue becomes empty, not here
                    }
                    messageSent = true;
                    // Don't add this message back to queue - it's been sent
                } else if (scheduled.remainingTicks > 0) {
                    // Decrease remaining ticks and keep in queue
                    updatedQueue.offer(new ScheduledMessage(scheduled.player, scheduled.message, scheduled.npcUuid, scheduled.remainingTicks - 1, scheduled.displayDuration, scheduled.expression, scheduled.onComplete));
                } else {
                    // Another message is ready but we already sent one this tick - keep it for next tick
                    // This should not happen if delays are calculated correctly, but handle it gracefully
                    Marallyzen.LOGGER.warn("NpcNarrateHandler: Message ready but already sent one this tick, keeping for next tick (remainingTicks: {})", 
                            scheduled.remainingTicks);
                    updatedQueue.offer(new ScheduledMessage(scheduled.player, scheduled.message, scheduled.npcUuid, scheduled.remainingTicks, scheduled.displayDuration, scheduled.expression, scheduled.onComplete));
                }
            }
            
            // Replace the queue with updated one
            if (updatedQueue.isEmpty()) {
                iterator.remove();
                // Queue is empty - all messages have been sent
                // But don't execute onComplete yet - wait for client to confirm narration is complete
                Runnable onComplete = pendingDialogOpens.remove(playerId);
                if (onComplete != null) {
                    // Store callback to execute when client confirms narration is complete
                    pendingNarrationCompleteCallbacks.put(playerId, onComplete);
                    Marallyzen.LOGGER.info("NpcNarrateHandler: All messages sent for player {}, waiting for client to confirm narration is complete before opening dialog", playerId);
                } else {
                    // No callback - narration completed, close dialog
                    Marallyzen.LOGGER.debug("NpcNarrateHandler: All messages sent, but no onComplete callback for player {}, closing dialog", playerId);
                    neutka.marallys.marallyzen.network.NetworkHelper.sendToPlayer(
                            player,
                            new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                    neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                            )
                    );
                }
            } else {
                entry.setValue(updatedQueue);
            }
        }
    }
    
    /**
     * Clears all scheduled messages for a player.
     * Useful when player disconnects.
     */
    public static void clearPlayerMessages(UUID playerId) {
        playerMessageQueues.remove(playerId);
        pendingDialogOpens.remove(playerId);
        pendingNarrationCompleteCallbacks.remove(playerId);
        pendingScreenFadeCompleteCallbacks.remove(playerId);
        pendingEyesCloseCompleteCallbacks.remove(playerId);
        // Don't remove playerLastNarrationTick - we need it for proximity delay even after queue is empty
    }
    
    /**
     * Overload without onComplete callback for backward compatibility.
     */
    public static void scheduleNarrateMessages(ServerPlayer player, List<String> messages, int totalDurationTicks, String npcName, ServerLevel serverLevel) {
        scheduleNarrateMessages(player, messages, null, totalDurationTicks, npcName, null, serverLevel, null);
    }

    private static String resolveExpression(List<String> expressions, int index) {
        if (expressions == null || expressions.isEmpty()) {
            return null;
        }
        int resolvedIndex = Math.min(index, expressions.size() - 1);
        String value = expressions.get(resolvedIndex);
        return value != null ? value.trim() : null;
    }
    
    /**
     * Clears scheduled messages when player logs out.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerMessages(player.getUUID());
        }
    }
}

