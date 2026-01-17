package neutka.marallys.marallyzen.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.Set;

@EventBusSubscriber(modid = Marallyzen.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class NpcSpawner {
    private static volatile boolean bootstrapped = false;

    private NpcSpawner() {
    }

    public static void bootstrap(ServerLevel level, NpcRegistry registry) {
        if (level == null || registry == null) {
            return;
        }
        bootstrapped = false;
        registerAllExisting(level, registry);
        NpcSavedData data = NpcSavedData.get(level);
        if (data.getNpcStates().isEmpty()) {
            syncSavedDataWithConfigs(data, registry, level);
        }
        data.rebuildIndex();
        spawnExistingFromSavedData(level, registry, data);
        bootstrapped = true;
    }

    public static void resetBootstrap() {
        bootstrapped = false;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        NpcRegistry registry = NpcClickHandler.getRegistry();
        if (!bootstrapped) {
            return;
        }
        registerExistingEntities(level, registry, event.getChunk().getPos());
        spawnForChunk(level, registry, event.getChunk().getPos());
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
        NpcSavedData data = NpcSavedData.get(level);
        Set<String> disabled = NpcStateStore.loadDisabled();
        Set<String> ids = data.getNpcIdsForChunk(level.dimension(), chunkPos.toLong());
        if (ids.isEmpty()) {
            return;
        }
        for (String npcId : ids) {
            if (disabled.contains(npcId)) {
                continue;
            }
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
            registerExistingEntity(registry, entity);
        }
        for (GeckoNpcEntity entity : level.getEntitiesOfClass(GeckoNpcEntity.class, box)) {
            registerExistingEntity(registry, entity);
        }
    }

    public static void registerAllExisting(ServerLevel level, NpcRegistry registry) {
        AABB box = new AABB(
                -3.0E7, level.getMinBuildHeight(), -3.0E7,
                3.0E7, level.getMaxBuildHeight(), 3.0E7
        );
        for (NpcEntity entity : level.getEntitiesOfClass(NpcEntity.class, box)) {
            registerExistingEntity(registry, entity);
        }
        for (GeckoNpcEntity entity : level.getEntitiesOfClass(GeckoNpcEntity.class, box)) {
            registerExistingEntity(registry, entity);
        }
    }

    private static void registerExistingEntity(NpcRegistry registry, Entity entity) {
        if (registry == null || entity == null) {
            return;
        }
        String npcId = null;
        if (entity instanceof NpcEntity npcEntity) {
            npcId = npcEntity.getNpcId();
        } else if (entity instanceof GeckoNpcEntity geckoEntity) {
            npcId = geckoEntity.getNpcId();
        }
        if (npcId == null || npcId.isEmpty()) {
            npcId = resolveNpcIdFromName(registry, entity);
            if (npcId != null) {
                if (entity instanceof NpcEntity npcEntity) {
                    npcEntity.setNpcId(npcId);
                } else if (entity instanceof GeckoNpcEntity geckoEntity) {
                    geckoEntity.setNpcId(npcId);
                }
                Marallyzen.LOGGER.info("NpcSpawner: Resolved npcId '{}' for existing entity {}", npcId, entity.getUUID());
            } else {
                // No NPC id and no name match -> treat as stray and discard to avoid duplicate spawns.
                Marallyzen.LOGGER.warn("NpcSpawner: Removing stray NPC entity {} (empty npcId, no name match)", entity.getUUID());
                entity.remove(Entity.RemovalReason.DISCARDED);
                return;
            }
        }
        Entity existing = registry.getNpc(npcId);
        if (existing != null && existing != entity) {
            entity.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        registry.registerExistingNpcEntity(entity);
    }

    private static String resolveNpcIdFromName(NpcRegistry registry, Entity entity) {
        if (registry == null || entity == null) {
            return null;
        }
        net.minecraft.network.chat.Component customName = entity.getCustomName();
        String name = customName != null ? customName.getString() : null;
        if (name == null || name.isBlank()) {
            return null;
        }
        String match = null;
        for (NpcData data : registry.getAllNpcData()) {
            if (data == null) {
                continue;
            }
            String dataName = data.getName();
            String dataId = data.getId();
            boolean nameMatches = dataName != null && !dataName.isBlank() && name.equalsIgnoreCase(dataName);
            boolean idMatches = dataId != null && !dataId.isBlank() && name.equalsIgnoreCase(dataId);
            if (nameMatches || idMatches) {
                if (match != null && !match.equals(dataId)) {
                    return null;
                }
                match = dataId;
            }
        }
        return match;
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

    private static void spawnExistingFromSavedData(ServerLevel level, NpcRegistry registry, NpcSavedData data) {
        if (level == null || registry == null || data == null) {
            return;
        }
        Set<String> disabled = NpcStateStore.loadDisabled();
        for (var entry : data.getNpcStates().entrySet()) {
            String npcId = entry.getKey();
            NpcState state = entry.getValue();
            if (npcId == null || state == null) {
                continue;
            }
            if (disabled.contains(npcId)) {
                continue;
            }
            if (!level.dimension().equals(state.dimension())) {
                continue;
            }
            if (registry.getNpc(npcId) != null) {
                continue;
            }
            if (!level.hasChunkAt(state.pos())) {
                continue;
            }
            registry.spawnNpcFromState(npcId, level, state);
        }
    }
}
