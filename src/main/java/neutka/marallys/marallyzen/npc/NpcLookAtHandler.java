package neutka.marallys.marallyzen.npc;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import neutka.marallys.marallyzen.Marallyzen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles smooth head and body rotation of NPCs towards nearby players.
 * NPCs will smoothly turn their head and body to look at players within 5 blocks (without walls).
 */
@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public class NpcLookAtHandler {
    
    private static final double LOOK_AT_RANGE = 5.0; // Maximum distance for head rotation
    private static final float ROTATION_SPEED = 16.0f; // Rotation speed in degrees per tick (doubled: 8.0 * 2)
    private static final float RETURN_ROTATION_SPEED = 10.0f; // Speed for returning to default position (doubled: 5.0 * 2)
    private static final int CHECK_INTERVAL = 2; // Check every 2 ticks for optimization
    
    private static int tickCounter = 0;
    
    /**
     * Stores rotation state for each NPC-Player pair (per-player rotation).
     */
    private static class PlayerNpcRotationState {
        float currentHeadYaw;      // Current head rotation angle for this player
        float currentBodyYaw;      // Current body rotation angle for this player
        float targetHeadYaw;       // Target head rotation angle
        float targetBodyYaw;       // Target body rotation angle
        float defaultHeadYaw;      // Default angle (original head yaw)
        float defaultBodyYaw;      // Default angle (original body yaw)
        boolean isLookingAtPlayer; // Flag indicating if NPC is looking at this player
        
        PlayerNpcRotationState(float defaultHeadYaw, float defaultBodyYaw) {
            this.currentHeadYaw = defaultHeadYaw;
            this.currentBodyYaw = defaultBodyYaw;
            this.targetHeadYaw = defaultHeadYaw;
            this.targetBodyYaw = defaultBodyYaw;
            this.defaultHeadYaw = defaultHeadYaw;
            this.defaultBodyYaw = defaultBodyYaw;
            this.isLookingAtPlayer = false;
        }
    }
    
    // Track rotation state for each (NPC, Player) pair
    // Key: NPC UUID, Value: Map of (Player UUID -> RotationState)
    private static final Map<UUID, Map<UUID, PlayerNpcRotationState>> npcPlayerRotationStates = new HashMap<>();
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
        
        // Process each dimension
        for (Map.Entry<ServerLevel, java.util.List<Entity>> levelEntry : npcsByLevel.entrySet()) {
            ServerLevel serverLevel = levelEntry.getKey();
            
            for (Entity npcEntity : levelEntry.getValue()) {
                String npcId = registry.getNpcId(npcEntity);
                if (npcId == null) {
                    continue;
                }
                NpcData data = registry.getNpcData(npcId);
                if (data == null || !Boolean.TRUE.equals(data.getLookAtPlayers())) {
                    continue;
                }
                updateNpcRotationForAllPlayers(npcEntity, serverLevel);
            }
        }
        
        // Clean up states for removed NPCs
        npcPlayerRotationStates.entrySet().removeIf(entry -> {
            UUID npcId = entry.getKey();
            return registry.getNpcByUuid(npcId) == null;
        });
    }
    
    /**
     * Updates rotation for a specific NPC for all nearby players (per-player rotation).
     */
    private static void updateNpcRotationForAllPlayers(Entity npcEntity, ServerLevel level) {
        // Only process LivingEntity NPCs
        if (!(npcEntity instanceof LivingEntity livingNpc)) {
            return;
        }
        
        UUID npcId = npcEntity.getUUID();
        Map<UUID, PlayerNpcRotationState> playerStates = npcPlayerRotationStates.computeIfAbsent(npcId, k -> new HashMap<>());
        
        // Get all players in the same dimension
        java.util.List<ServerPlayer> playersInLevel = level.getServer().getPlayerList().getPlayers().stream()
                .filter(p -> p.level() == level && p.isAlive())
                .toList();
        
        // Process each player separately
        for (ServerPlayer player : playersInLevel) {
            UUID playerId = player.getUUID();
            
            // Initialize state for this player if not exists
            // Always use the actual NPC's current rotation as default (original position)
            PlayerNpcRotationState state = playerStates.computeIfAbsent(playerId, k -> {
                // Save original rotation values from NPC entity
                // These are the "rest" position that NPC should return to
                return new PlayerNpcRotationState(livingNpc.yHeadRot, livingNpc.getYRot());
            });
            
            // Update default values to match NPC's actual rotation if NPC has moved
            // This ensures we always return to the NPC's current "rest" position
            state.defaultHeadYaw = livingNpc.yHeadRot;
            state.defaultBodyYaw = livingNpc.getYRot();
            
            // Check if player is in range with line of sight
            double distance = livingNpc.distanceTo(player);
            boolean inRange = distance <= LOOK_AT_RANGE && livingNpc.hasLineOfSight(player);
            
            if (inRange) {
                // Player in range - calculate target angle and smoothly rotate
                float targetYaw = calculateLookAtYaw(livingNpc, player);
                state.targetHeadYaw = targetYaw;
                state.targetBodyYaw = targetYaw; // Body also looks at player
                state.isLookingAtPlayer = true;
                
                // Smoothly rotate head and body towards target
                smoothRotateForPlayer(livingNpc, player, state, ROTATION_SPEED);
            } else {
                // Player not in range - smoothly return to original default position
                // Always return to the original NPC rotation, not current rotation
                state.isLookingAtPlayer = false;
                state.targetHeadYaw = state.defaultHeadYaw;
                state.targetBodyYaw = state.defaultBodyYaw;
                
                // Smoothly return to default
                smoothRotateForPlayer(livingNpc, player, state, RETURN_ROTATION_SPEED);
            }
        }
        
        // Clean up states for players who left the level
        playerStates.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            return playersInLevel.stream().noneMatch(p -> p.getUUID().equals(playerId));
        });
    }
    
    /**
     * Calculates the yaw angle to look at the target player.
     */
    private static float calculateLookAtYaw(Entity npc, ServerPlayer player) {
        double dx = player.getX() - npc.getX();
        double dz = player.getZ() - npc.getZ();
        
        // Calculate yaw angle in degrees
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        
        // Normalize to [-180, 180] range
        return Mth.wrapDegrees(targetYaw);
    }
    
    /**
     * Smoothly rotates the NPC's head and body towards the target angle for a specific player.
     * Sends rotation packets only to that player (per-player rotation).
     */
    private static void smoothRotateForPlayer(LivingEntity npc, ServerPlayer player, PlayerNpcRotationState state, float rotationSpeed) {
        // Calculate head rotation
        float currentHeadYaw = state.currentHeadYaw;
        float targetHeadYaw = state.targetHeadYaw;
        float deltaHeadYaw = Mth.wrapDegrees(targetHeadYaw - currentHeadYaw);
        
        // Limit rotation speed
        float maxRotation = rotationSpeed;
        if (Math.abs(deltaHeadYaw) > maxRotation) {
            deltaHeadYaw = Math.signum(deltaHeadYaw) * maxRotation;
        }
        
        // Update current head yaw
        float newHeadYaw = Mth.wrapDegrees(currentHeadYaw + deltaHeadYaw);
        state.currentHeadYaw = newHeadYaw;
        
        // Calculate body rotation
        float currentBodyYaw = state.currentBodyYaw;
        float targetBodyYaw = state.targetBodyYaw;
        float deltaBodyYaw = Mth.wrapDegrees(targetBodyYaw - currentBodyYaw);
        
        // Body rotates slightly slower than head (but faster than before)
        float bodyMaxRotation = rotationSpeed * 0.75f;
        if (Math.abs(deltaBodyYaw) > bodyMaxRotation) {
            deltaBodyYaw = Math.signum(deltaBodyYaw) * bodyMaxRotation;
        }
        
        // Update current body yaw
        float newBodyYaw = Mth.wrapDegrees(currentBodyYaw + deltaBodyYaw);
        state.currentBodyYaw = newBodyYaw;

        if (npc instanceof ServerPlayer) {
            npc.setYRot(newBodyYaw);
            npc.yBodyRot = newBodyYaw;
            npc.yBodyRotO = newBodyYaw;
            npc.yHeadRot = newHeadYaw;
            npc.yHeadRotO = newHeadYaw;
        }

        // Send rotation packets to this specific player only
        if (player.connection != null) {
            // Send head rotation packet
            player.connection.send(new ClientboundRotateHeadPacket(npc, (byte) (newHeadYaw * 256.0F / 360.0F)));
            
            // Send body rotation packet
            player.connection.send(new ClientboundMoveEntityPacket.Rot(
                    npc.getId(),
                    (byte) (newBodyYaw * 256.0F / 360.0F),
                    (byte) (npc.getXRot() * 256.0F / 360.0F),
                    npc.onGround()
            ));
        }
    }
    
    /**
     * Handles player logout - removes rotation state for this player from all NPCs.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Remove rotation state for this player from all NPCs
        for (Map<UUID, PlayerNpcRotationState> playerStates : npcPlayerRotationStates.values()) {
            playerStates.remove(playerId);
        }
    }
}
