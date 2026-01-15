package neutka.marallys.marallyzen.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import neutka.marallys.marallyzen.npc.NpcClickHandler;
import neutka.marallys.marallyzen.npc.NpcData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the NPC Manager API.
 */
class NpcManagerImpl implements INpcManager {

    @Override
    public Entity createNpc(String npcId, ServerLevel level, BlockPos spawnPos) {
        return NpcClickHandler.getRegistry().spawnNpc(npcId, level, spawnPos);
    }

    @Override
    public void registerNpc(String npcId, String name, ResourceLocation entityType,
                           BlockPos spawnPos, List<BlockPos> waypoints, Map<String, String> metadata) {
        NpcData data = new NpcData(npcId);
        data.setName(name);

        // Convert ResourceLocation to EntityType if needed
        // For now, we'll store it as string in metadata
        if (entityType != null) {
            metadata.put("entityType", entityType.toString());
        }

        data.setSpawnPos(spawnPos);
        data.setWaypoints(waypoints.stream()
                .map(pos -> new NpcData.Waypoint(pos, 2000, 0.3f)) // Default wait and speed
                .toList());
        data.setMetadata(metadata);

        NpcClickHandler.getRegistry().registerNpcData(data);
    }

    @Override
    public Entity getNpc(String npcId) {
        return NpcClickHandler.getRegistry().getNpc(npcId);
    }

    @Override
    public boolean isNpc(Entity entity) {
        return NpcClickHandler.getRegistry().isNpc(entity);
    }

    @Override
    public void moveNpcToWaypoint(String npcId, int waypointIndex) {
        NpcClickHandler.getRegistry().moveNpcToWaypoint(npcId, waypointIndex);
    }

    @Override
    public void setNpcWaypointLoop(String npcId, boolean loop) {
        NpcClickHandler.getRegistry().setWaypointLoop(npcId, loop);
    }

    @Override
    public void despawnNpc(String npcId) {
        NpcClickHandler.getRegistry().despawnNpc(npcId);
    }

    @Override
    public List<String> getAllNpcIds() {
        return new ArrayList<>(NpcClickHandler.getRegistry().getAllNpcData()
                .stream()
                .map(NpcData::getId)
                .toList());
    }
}



