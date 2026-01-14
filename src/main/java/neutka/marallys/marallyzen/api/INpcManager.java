package neutka.marallys.marallyzen.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;

/**
 * API for managing NPCs in Marallyzen.
 * Allows other mods to create, modify, and control NPCs.
 */
public interface INpcManager {

    /**
     * Create a new NPC from configuration.
     *
     * @param npcId The unique ID for this NPC
     * @param level The world level
     * @param spawnPos The spawn position
     * @return The created NPC entity, or null if creation failed
     */
    Entity createNpc(String npcId, ServerLevel level, BlockPos spawnPos);

    /**
     * Register an NPC configuration programmatically.
     *
     * @param npcId The unique ID for this NPC
     * @param name The display name
     * @param entityType The entity type resource location
     * @param spawnPos The spawn position
     * @param waypoints List of waypoint positions
     * @param metadata Additional metadata
     */
    void registerNpc(String npcId, String name, ResourceLocation entityType,
                    BlockPos spawnPos, List<BlockPos> waypoints, Map<String, String> metadata);

    /**
     * Get an NPC by its ID.
     *
     * @param npcId The NPC ID
     * @return The NPC entity, or null if not found
     */
    Entity getNpc(String npcId);

    /**
     * Check if an entity is a Marallyzen NPC.
     *
     * @param entity The entity to check
     * @return true if the entity is an NPC
     */
    boolean isNpc(Entity entity);

    /**
     * Move an NPC to a specific waypoint.
     *
     * @param npcId The NPC ID
     * @param waypointIndex The waypoint index
     */
    void moveNpcToWaypoint(String npcId, int waypointIndex);

    /**
     * Set whether an NPC's waypoints should loop.
     *
     * @param npcId The NPC ID
     * @param loop true to loop waypoints
     */
    void setNpcWaypointLoop(String npcId, boolean loop);

    /**
     * Despawn an NPC.
     *
     * @param npcId The NPC ID
     */
    void despawnNpc(String npcId);

    /**
     * Get all registered NPC IDs.
     *
     * @return List of NPC IDs
     */
    List<String> getAllNpcIds();
}



