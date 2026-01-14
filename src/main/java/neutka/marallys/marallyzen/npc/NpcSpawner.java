package neutka.marallys.marallyzen.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.Set;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class NpcSpawner {
    private NpcSpawner() {
    }

    public static void bootstrap(ServerLevel level, NpcRegistry registry) {
        if (level == null || registry == null) {
            return;
        }
        NpcSavedData data = NpcSavedData.get(level);
        syncSavedDataWithConfigs(data, registry, level);
        data.rebuildIndex();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        spawnForChunk(level, NpcClickHandler.getRegistry(), event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        despawnForChunk(level, NpcClickHandler.getRegistry(), event.getChunk().getPos());
    }

    private static void spawnForChunk(ServerLevel level, NpcRegistry registry, ChunkPos chunkPos) {
        if (level == null || registry == null || chunkPos == null) {
            return;
        }
        registerExistingEntities(level, registry, chunkPos);
        NpcSavedData data = NpcSavedData.get(level);
        Set<String> ids = data.getNpcIdsForChunk(level.dimension(), chunkPos.toLong());
        if (ids.isEmpty()) {
            return;
        }
        for (String npcId : ids) {
            if (registry.getNpc(npcId) != null) {
                continue;
            }
            NpcState state = data.getState(npcId);
            if (state == null) {
                continue;
            }
            if (!level.dimension().equals(state.dimension())) {
                continue;
            }
            registry.spawnNpcFromState(npcId, level, state);
        }
    }

    private static void registerExistingEntities(ServerLevel level, NpcRegistry registry, ChunkPos chunkPos) {
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX() + 1;
        int maxZ = chunkPos.getMaxBlockZ() + 1;
        AABB box = new AABB(minX, level.getMinBuildHeight(), minZ, maxX, level.getMaxBuildHeight(), maxZ);
        for (NpcEntity entity : level.getEntitiesOfClass(NpcEntity.class, box)) {
            registry.registerExistingNpcEntity(entity);
        }
    }

    private static void despawnForChunk(ServerLevel level, NpcRegistry registry, ChunkPos chunkPos) {
        if (level == null || registry == null || chunkPos == null) {
            return;
        }
        NpcSavedData data = NpcSavedData.get(level);
        Set<String> ids = data.getNpcIdsForChunk(level.dimension(), chunkPos.toLong());
        if (ids.isEmpty()) {
            return;
        }
        for (String npcId : ids) {
            registry.despawnNpc(npcId);
        }
    }

    private static void syncSavedDataWithConfigs(NpcSavedData data, NpcRegistry registry, ServerLevel level) {
        for (NpcData npcData : registry.getAllNpcData()) {
            if (data.getState(npcData.getId()) != null) {
                continue;
            }
            BlockPos spawnPos = npcData.getSpawnPos();
            if (spawnPos == null) {
                continue;
            }
            String appearanceId = npcData.getId();
            NpcState state = new NpcState(level.dimension(), spawnPos, 0.0f, appearanceId, null, null);
            data.putState(npcData.getId(), state);
        }
    }
}
