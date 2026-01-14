package neutka.marallys.marallyzen.npc;

import com.google.gson.JsonParseException;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.NpcTalkIconPacket;
import neutka.marallys.marallyzen.network.ProximityPacket;
import neutka.marallys.marallyzen.network.ClearProximityPacket;
import neutka.marallys.marallyzen.npc.NpcExpressionManager;
import neutka.marallys.marallyzen.util.NarrationIcons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles proximity detection for NPCs - shows name and text when player approaches.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public class NpcProximityHandler {
    
    private static final int CHECK_INTERVAL = 2; // Check every 2 ticks (0.1 seconds) for smoother animation
    private static int tickCounter = 0;
    
    // Track which players are in proximity of which NPCs
    private static final Map<UUID, Map<String, Boolean>> playerProximityMap = new HashMap<>();
    // Track which NPC each player is currently viewing (for Custom HUD display)
    private static final Map<UUID, String> playerCurrentNpc = new HashMap<>();
    // Track animation progress for each player's proximity overlay (0.0 = hidden, 1.0 = fully visible)
    private static final Map<UUID, Float> playerProximityAnimation = new HashMap<>();
    // Track dialog close cooldown for each player (prevents proximityText from showing immediately after dialog closes)
    private static final Map<UUID, Integer> playerDialogCloseCooldown = new HashMap<>();
    // Track which NPC has an open dialog for each player (UUID of NPC entity)
    private static final Map<UUID, UUID> playerOpenDialogNpc = new HashMap<>();
    // Track current dialog step for each player (playerId -> step number)
    private static final Map<UUID, Integer> playerDialogStep = new HashMap<>();
    
    // Animation speed to match dialog fade-in (5 ticks = 0.25 seconds)
    // Each tick increases by 1/5 = 0.2, but we check every 2 ticks, so 0.2 * 2 = 0.4 per check
    // Actually, we want smooth animation: 1.0 / 5 ticks = 0.2 per tick
    // But we check every CHECK_INTERVAL (2 ticks), so per check: 0.2 * 2 = 0.4
    private static final float ANIMATION_SPEED = 0.2f; // 1.0 / 5 ticks = 0.2 per tick (matches dialog FADE_IN_DURATION_TICKS = 5)
    private static final int DIALOG_CLOSE_COOLDOWN_TICKS = 40; // 2 seconds at 20 TPS (same as client-side cooldown)

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Decrease dialog close cooldown for all players
        playerDialogCloseCooldown.replaceAll((uuid, cooldown) -> Math.max(0, cooldown - 1));
        playerDialogCloseCooldown.entrySet().removeIf(entry -> entry.getValue() <= 0);
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        NpcRegistry registry = NpcClickHandler.getRegistry();
        
        // Group NPCs by dimension for efficient processing
        Map<ServerLevel, java.util.List<Entity>> npcsByLevel = new HashMap<>();
        for (Entity npcEntity : registry.getSpawnedNpcs()) {
            if (npcEntity.level() instanceof ServerLevel serverLevel) {
                npcsByLevel.computeIfAbsent(serverLevel, k -> new java.util.ArrayList<>()).add(npcEntity);
            }
        }
        
        // Track which players are near NPCs in this tick
        java.util.Set<UUID> playersNearNpcs = new java.util.HashSet<>();
        
        // Process each dimension
        for (Map.Entry<ServerLevel, java.util.List<Entity>> levelEntry : npcsByLevel.entrySet()) {
            ServerLevel serverLevel = levelEntry.getKey();
            
            // For each player in this dimension, find the closest NPC
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                if (player.level() != serverLevel) {
                    continue;
                }
                
                Entity closestNpc = null;
                NpcData closestNpcData = null;
                double closestDistance = Double.MAX_VALUE;
                
                // Find closest NPC in range
                for (Entity npcEntity : levelEntry.getValue()) {
                    String npcId = registry.getNpcId(npcEntity);
                    if (npcId == null) {
                        continue;
                    }
                    
                    NpcData npcData = registry.getNpcData(npcId);
                    if (npcData == null || npcData.getProximityText() == null || npcData.getProximityText().isEmpty()) {
                        continue;
                    }
                    
                    double range = npcData.getProximityRange() != null ? npcData.getProximityRange() : 8.0;
                    double distance = player.distanceTo(npcEntity);
                    
                    if (distance <= range && distance < closestDistance) {
                        closestNpc = npcEntity;
                        closestNpcData = npcData;
                        closestDistance = distance;
                    }
                }
                
                // Update player's proximity overlay based on closest NPC
                UUID playerId = player.getUUID();
                Map<String, Boolean> npcProximity = playerProximityMap.computeIfAbsent(playerId, k -> new HashMap<>());
                String currentNpc = playerCurrentNpc.get(playerId);
                
                if (closestNpc != null && closestNpcData != null) {
                    String npcId = registry.getNpcId(closestNpc);
                    boolean wasInRange = npcProximity.getOrDefault(npcId, false);
                    playersNearNpcs.add(playerId);
                    
                    if (!wasInRange || !npcId.equals(currentNpc)) {
                        // If we were looking at another NPC before - hide its talk icon
                        if (currentNpc != null && !npcId.equals(currentNpc)) {
                            sendTalkIconHide(player, registry, currentNpc, serverLevel);
                        }

                        // Player entered range or switched to different NPC
                        onPlayerEnterProximity(player, closestNpc, closestNpcData);

                        // Show talk icon for NPCs with dialog
                        if (hasDialog(closestNpcData)) {
                            sendTalkIcon(player, closestNpc, closestNpcData, serverLevel, true);
                        }

                        npcProximity.put(npcId, true);
                        playerCurrentNpc.put(playerId, npcId);
                        // Reset animation when entering proximity
                        playerProximityAnimation.put(playerId, 0.0f);
                    } else {
                        // Player is still in range of the same NPC - update proximity overlay with animation (if enabled)
                        // But don't show if dialog was just closed (cooldown active) or if narrate messages are being sent
                        UUID openDialogNpcUuid = playerOpenDialogNpc.get(playerId);
                        boolean isDialogOpen = openDialogNpcUuid != null && closestNpc.getUUID().equals(openDialogNpcUuid);
                        
                        Integer cooldown = playerDialogCloseCooldown.get(playerId);
                        boolean isReceivingNarrate = neutka.marallys.marallyzen.npc.NpcNarrateHandler.isPlayerReceivingNarrate(playerId);
                        // Also check if narration was sent recently (to prevent flickering after narration ends)
                        // BUT: if dialog is open, don't apply delay - proximity should show immediately
                        int currentTick = (int) serverLevel.getGameTime();
                        boolean wasNarrationRecent = !isDialogOpen && neutka.marallys.marallyzen.npc.NpcNarrateHandler.wasNarrationSentRecently(playerId, currentTick, 40); // 2 seconds delay, but not if dialog is open
                        
                        if ((cooldown == null || cooldown <= 0) && !isReceivingNarrate && !wasNarrationRecent) {
                            boolean showInCustomHud = closestNpcData.getShowProximityTextInActionBar() != null ? closestNpcData.getShowProximityTextInActionBar() : true;
                            if (showInCustomHud) {
                                updatePlayerProximityOverlay(player, closestNpc, closestNpcData, serverLevel);
                            }
                        } else if (wasNarrationRecent) {
                            Marallyzen.LOGGER.debug("[NpcProximityHandler] Skipping proximity update for player {} - narration was sent recently (tick {}), dialogOpen={}", 
                                    player.getName().getString(), currentTick, isDialogOpen);
                        }
                    }
                } else {
                    // Player is not near any NPC
                    UUID openDialogNpcUuid = playerOpenDialogNpc.get(playerId);
                    
                    // If dialog is open, check if player is still within dialog distance
                    if (openDialogNpcUuid != null) {
                        // Find the NPC entity with open dialog
                        Entity npcEntity = null;
                        NpcData npcData = null;
                        for (Entity entity : levelEntry.getValue()) {
                            if (entity.getUUID().equals(openDialogNpcUuid)) {
                                String npcId = registry.getNpcId(entity);
                                if (npcId != null) {
                                    npcEntity = entity;
                                    npcData = registry.getNpcData(npcId);
                                    break;
                                }
                            }
                        }
                        
                        if (npcEntity != null && npcData != null) {
                            // Check distance from player to NPC with open dialog
                            double distance = player.distanceTo(npcEntity);
                            double MAX_DIALOG_DISTANCE = 8.0; // Same as client-side constant
                            
                            if (distance > MAX_DIALOG_DISTANCE) {
                                // Player moved too far away - close dialog and clear proximity
                                Marallyzen.LOGGER.info("[NpcProximityHandler] Player {} moved too far from NPC {} (distance: {}), closing dialog", 
                                        player.getName().getString(), npcEntity.getUUID(), distance);
                                NpcExpressionManager.applyDefaultExpression(npcEntity, npcData);
                                setPlayerDialogClosed(playerId);
                                // Clear proximity overlay
                                NetworkHelper.sendToPlayer(player, new ClearProximityPacket());
                                // Send state change to client to close dialog
                                NetworkHelper.sendToPlayer(player, 
                                        new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                                neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                                        ));
                            } else {
                                // Continue smooth animation for navigation message
                                boolean showInCustomHud = npcData.getShowProximityTextInActionBar() != null ? npcData.getShowProximityTextInActionBar() : true;
                                if (showInCustomHud) {
                                    updatePlayerProximityOverlay(player, npcEntity, npcData, serverLevel);
                                }
                            }
                        } else {
                            // NPC entity not found - close dialog
                            Marallyzen.LOGGER.info("[NpcProximityHandler] NPC entity {} not found for player {}, closing dialog", 
                                    openDialogNpcUuid, player.getName().getString());
                            setPlayerDialogClosed(playerId);
                            // Clear proximity overlay
                            NetworkHelper.sendToPlayer(player, new ClearProximityPacket());
                            // Send state change to client to close dialog
                            NetworkHelper.sendToPlayer(player, 
                                    new neutka.marallys.marallyzen.network.DialogStateChangedPacket(
                                            neutka.marallys.marallyzen.client.gui.DialogState.CLOSED
                                    ));
                        }
                    } else if (currentNpc != null) {
                        // Player was near an NPC before, but not anymore - animate out
                        Float animProgress = playerProximityAnimation.get(playerId);
                        if (animProgress != null && animProgress > 0.0f) {
                            // Animate out
                            animProgress = Math.max(0.0f, animProgress - ANIMATION_SPEED);
                            playerProximityAnimation.put(playerId, animProgress);
                            
                            if (animProgress > 0.0f) {
                                // Still animating out - update with fade
                                String npcId = playerCurrentNpc.get(playerId);
                                if (npcId != null) {
                                    Entity npcEntity = registry.getNpc(npcId);
                                    NpcData npcData = registry.getNpcData(npcId);
                                    if (npcEntity != null && npcData != null) {
                                        updatePlayerActionBarWithAnimation(player, npcEntity, npcData, serverLevel, animProgress);
                                    }
                                }
                            } else {
                                // Animation complete - clear
                                sendTalkIconHide(player, registry, playerCurrentNpc.get(playerId), serverLevel);
                                onPlayerLeaveProximity(player);
                                npcProximity.clear();
                                playerCurrentNpc.remove(playerId);
                                playerProximityAnimation.remove(playerId);
                            }
                        } else {
                            // No animation, just clear
                            sendTalkIconHide(player, registry, playerCurrentNpc.get(playerId), serverLevel);
                            onPlayerLeaveProximity(player);
                            npcProximity.clear();
                            playerCurrentNpc.remove(playerId);
                            playerProximityAnimation.remove(playerId);
                        }
                    }
                }
            }
        }
        
        // Check all players who were near NPCs before but aren't now
        for (UUID playerId : new java.util.HashSet<>(playerCurrentNpc.keySet())) {
            if (!playersNearNpcs.contains(playerId)) {
                // Find player and clear their proximity overlay
                for (ServerLevel level : npcsByLevel.keySet()) {
                    for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                        if (player.getUUID().equals(playerId)) {
                            sendTalkIconHide(player, registry, playerCurrentNpc.get(playerId), level);
                            onPlayerLeaveProximity(player);
                            playerProximityMap.remove(playerId);
                            playerCurrentNpc.remove(playerId);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Parses text as JSON Component if it's valid JSON, otherwise returns plain text Component.
     * Uses RegistryAccess from the server level.
     */
    private static Component parseTextComponent(String text, ServerLevel level) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Try to parse as JSON first
        text = text.trim();
        if (text.startsWith("{") || text.startsWith("[")) {
            try {
                return Component.Serializer.fromJson(text, level.registryAccess());
            } catch (JsonParseException e) {
                // If JSON parsing fails, treat as plain text
                Marallyzen.LOGGER.debug("Failed to parse text as JSON, using plain text: " + text, e);
            }
        }
        
        // Return as plain text
        return Component.literal(text);
    }
    
    /**
     * Creates a formatted message with NPC name and proximity text, supporting JSON colors.
     */
    private static Component createFormattedMessage(String npcName, String proximityText, ServerLevel level) {
        String displayName = parseTextComponent(npcName, level).getString();
        Component nameComponent = Component.literal(displayName).withStyle(style ->
            style.withColor(TextColor.fromRgb(0xD48E03)));
        Component textComponent = parseTextComponent(proximityText, level);
        
        // Create ">>" separator in dark gray
        final TextColor darkGrayColor = TextColor.fromRgb(0x555555);
        Component separator = Component.literal(" >> ").withStyle(style -> style.withColor(darkGrayColor));
        
        // Combine: "NPC Name >> Text"
        return nameComponent.copy().append(separator).append(textComponent);
    }

    private static boolean hasDialog(NpcData npcData) {
        if (npcData == null) {
            return false;
        }
        boolean hasScript = npcData.getDialogScript() != null && !npcData.getDialogScript().isEmpty();
        boolean hasAi = npcData.getAiSettings() != null && npcData.getAiSettings().isEnabled();
        return hasScript || hasAi;
    }

    private static void sendTalkIcon(ServerPlayer player, Entity npcEntity, NpcData npcData, ServerLevel level, boolean visible) {
        if (player == null || npcEntity == null || level == null || !hasDialog(npcData)) {
            return;
        }
        int argbColor = resolveNameColorArgb(npcData, level);
        NetworkHelper.sendToPlayer(player, new NpcTalkIconPacket(npcEntity.getUUID(), argbColor, visible));
    }

    private static void sendTalkIconHide(ServerPlayer player, NpcRegistry registry, String npcId, ServerLevel level) {
        if (player == null || registry == null || npcId == null) {
            return;
        }
        Entity npcEntity = registry.getNpc(npcId);
        NpcData npcData = registry.getNpcData(npcId);
        if (npcEntity != null && hasDialog(npcData)) {
            sendTalkIcon(player, npcEntity, npcData, level, false);
        }
    }

    private static int resolveNameColorArgb(NpcData npcData, ServerLevel level) {
        return 0xFFD48E03;
    }

    private static TextColor findFirstColor(Component component) {
        if (component.getStyle().getColor() != null) {
            return component.getStyle().getColor();
        }
        for (Component sibling : component.getSiblings()) {
            TextColor color = findFirstColor(sibling);
            if (color != null) {
                return color;
            }
        }
        return null;
    }
    
    private static void onPlayerEnterProximity(ServerPlayer player, Entity npcEntity, NpcData npcData) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        String npcName = npcData.getName() != null ? npcData.getName() : npcData.getId();
        String proximityText = npcData.getProximityText();
        
        // Send message to chat if enabled (default: true)
        boolean showInChat = npcData.getShowProximityTextInChat() != null ? npcData.getShowProximityTextInChat() : true;
        if (showInChat) {
            Component chatMessage = createFormattedMessage(npcName, proximityText, serverLevel);
            player.sendSystemMessage(chatMessage);
        }
        
        // Show proximity overlay if enabled (default: true)
        boolean showInCustomHud = npcData.getShowProximityTextInActionBar() != null ? npcData.getShowProximityTextInActionBar() : true;
        if (showInCustomHud) {
            // Animation starts at 0.0, will be updated in updatePlayerProximityOverlay
            playerProximityAnimation.put(player.getUUID(), 0.0f);
            updatePlayerProximityOverlay(player, npcEntity, npcData, serverLevel);
        }
    }
    
    private static void onPlayerLeaveProximity(ServerPlayer player) {
        // Clear proximity overlay when player leaves range
        Marallyzen.LOGGER.info("[NpcProximityHandler] SERVER: Sending ClearProximityPacket to player {} (player left proximity)", 
                player.getName().getString());
        NetworkHelper.sendToPlayer(player, new ClearProximityPacket());
    }
    
    private static void updatePlayerProximityOverlay(ServerPlayer player, Entity npcEntity, NpcData npcData, ServerLevel serverLevel) {
        if (player.connection == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        Float animProgress = playerProximityAnimation.get(playerId);
        
        // Update animation progress
        if (animProgress == null) {
            animProgress = 0.0f;
        }
        
        // Animate in if not fully visible
        if (animProgress < 1.0f) {
            animProgress = Math.min(1.0f, animProgress + ANIMATION_SPEED);
            playerProximityAnimation.put(playerId, animProgress);
        }
        
        // Update proximity overlay with animation
        updatePlayerActionBarWithAnimation(player, npcEntity, npcData, serverLevel, animProgress);
    }
    
    private static void updatePlayerActionBarWithAnimation(ServerPlayer player, Entity npcEntity, NpcData npcData, ServerLevel serverLevel, float animationProgress) {
        if (player.connection == null) {
            return;
        }
        
        // If animation is at 0, clear proximity overlay
        if (animationProgress <= 0.0f) {
            Marallyzen.LOGGER.info("[NpcProximityHandler] SERVER: Sending ClearProximityPacket to player {} (animationProgress={})", 
                    player.getName().getString(), animationProgress);
            NetworkHelper.sendToPlayer(player, new ClearProximityPacket());
            return;
        }
        
        String npcName = npcData.getName() != null ? npcData.getName() : npcData.getId();
        String proximityText = npcData.getProximityText();
        
        // Check if dialog is open for this player with this NPC
        UUID playerId = player.getUUID();
        UUID currentNpcUuid = npcEntity != null ? npcEntity.getUUID() : null;
        UUID openDialogNpcUuid = playerOpenDialogNpc.get(playerId);
        
        Component proximityMessage;
        boolean isNavigationMessage = false;
        if (openDialogNpcUuid != null && currentNpcUuid != null && openDialogNpcUuid.equals(currentNpcUuid)) {
            // Dialog is open - show navigation instructions
            proximityMessage = createNavigationMessage(npcName, serverLevel);
            isNavigationMessage = true;
        } else {
            // Normal proximity text
            proximityMessage = createFormattedMessage(npcName, proximityText, serverLevel);
        }
        
        // Send proximity packet with animation progress as alpha
        // animationProgress goes from 0.0 to 1.0, which is perfect for alpha
        ProximityPacket packet = new ProximityPacket(proximityMessage, currentNpcUuid, animationProgress);
        NetworkHelper.sendToPlayer(player, packet);
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearPlayerProximity(player.getUUID());
        }
    }
    
    public static void clearPlayerProximity(UUID playerId) {
        playerProximityMap.remove(playerId);
        playerCurrentNpc.remove(playerId);
        playerProximityAnimation.remove(playerId);
        playerDialogCloseCooldown.remove(playerId);
        playerOpenDialogNpc.remove(playerId);
        playerDialogStep.remove(playerId);
        neutka.marallys.marallyzen.ai.NpcAiManager.clearPlayer(playerId);
    }
    
    /**
     * Marks that a dialog is open for a player with a specific NPC.
     * @param playerId The UUID of the player
     * @param npcEntityUuid The UUID of the NPC entity
     * @param step The current dialog step (defaults to 1 if not specified)
     */
    public static void setPlayerDialogOpen(UUID playerId, UUID npcEntityUuid, int step) {
        if (npcEntityUuid != null) {
            playerOpenDialogNpc.put(playerId, npcEntityUuid);
            playerDialogStep.put(playerId, step);
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("NpcProximityHandler: Set dialog step {} for player {}", step, playerId);
        } else {
            playerOpenDialogNpc.remove(playerId);
            playerDialogStep.remove(playerId);
        }
    }
    
    /**
     * Marks that a dialog is open for a player with a specific NPC (defaults to step 1).
     */
    public static void setPlayerDialogOpen(UUID playerId, UUID npcEntityUuid) {
        setPlayerDialogOpen(playerId, npcEntityUuid, 1);
    }
    
    /**
     * Gets the UUID of the NPC that has an open dialog for a player.
     * @param playerId The UUID of the player
     * @return The UUID of the NPC, or null if no dialog is open
     */
    public static UUID getPlayerOpenDialogNpc(UUID playerId) {
        return playerOpenDialogNpc.get(playerId);
    }
    
    /**
     * Gets the current dialog step for a player.
     * @param playerId The UUID of the player
     * @return The current step number, or 1 if not set
     */
    public static int getPlayerDialogStep(UUID playerId) {
        int step = playerDialogStep.getOrDefault(playerId, 1);
        neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("NpcProximityHandler: Getting dialog step {} for player {}", step, playerId);
        return step;
    }
    
    /**
     * Sets the current dialog step for a player.
     * @param playerId The UUID of the player
     * @param step The step number
     */
    public static void setPlayerDialogStep(UUID playerId, int step) {
        if (step > 0) {
            playerDialogStep.put(playerId, step);
        } else {
            playerDialogStep.remove(playerId);
        }
    }
    
    /**
     * Marks that a dialog is closed for a player.
     * Note: We keep the step so that when a button is clicked, we know which step's options to load.
     * The step will be updated when a new dialog is opened.
     * @param playerId The UUID of the player
     */
    public static void setPlayerDialogClosed(UUID playerId) {
        Integer currentStep = playerDialogStep.get(playerId);
        playerOpenDialogNpc.remove(playerId);
        // Don't remove the step - we need it to know which step's options to load when button is clicked
        // The step will be updated when a new dialog is opened
        neutka.marallys.marallyzen.Marallyzen.LOGGER.debug("NpcProximityHandler: Dialog closed for player {}, keeping step {}", playerId, currentStep);
    }
    
    /**
     * Creates a navigation instruction message for dialog.
     */
    private static Component createNavigationMessage(String npcName, ServerLevel serverLevel) {
        return Component.translatable("narration.marallyzen.dialog_navigation", NarrationIcons.rmb());
    }
    
    /**
     * Recursively finds the first color in a Component.
     */
    private static TextColor findFirstColorInComponent(Component component) {
        if (component.getStyle().getColor() != null) {
            return component.getStyle().getColor();
        }
        for (Component sibling : component.getSiblings()) {
            TextColor color = findFirstColorInComponent(sibling);
            if (color != null) {
                return color;
            }
        }
        return null;
    }
    
    /**
     * Sets dialog close cooldown for a player to prevent proximityText from showing immediately after dialog closes.
     */
    public static void setDialogCloseCooldown(UUID playerId) {
        playerDialogCloseCooldown.put(playerId, DIALOG_CLOSE_COOLDOWN_TICKS);
    }
    
    /**
     * Starts smooth animation for proximity (navigation message) when dialog opens.
     * Animation will be updated in the main tick loop, same as regular proximity.
     * 
     * @param player The player
     * @param npcEntity The NPC entity
     * @param npcData The NPC data
     * @param serverLevel The server level
     */
    public static void sendProximityForOpenDialog(ServerPlayer player, Entity npcEntity, NpcData npcData, ServerLevel serverLevel) {
        if (player.connection == null || npcEntity == null || npcData == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        NpcRegistry registry = NpcClickHandler.getRegistry();
        String npcId = registry.getNpcId(npcEntity);
        if (npcId == null) {
            return;
        }
        
        // Start animation from 0.0 for smooth fade-in (same as regular proximity)
        playerProximityAnimation.put(playerId, 0.0f);
        
        // Ensure player is tracked in proximity map so updatePlayerProximityOverlay is called in main tick loop
        // This ensures smooth animation continues even if player is not in proximity range
        playerProximityMap.computeIfAbsent(playerId, k -> new HashMap<>()).put(npcId, true);
        playerCurrentNpc.put(playerId, npcId);
        
        // Immediately send first frame with alpha=0.0 to start animation
        // Main tick loop will continue updating it smoothly
        updatePlayerProximityOverlay(player, npcEntity, npcData, serverLevel);
    }
}

