package neutka.marallys.marallyzen.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
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
        syncSavedDataWithConfigs(data, registry, level);
        data.rebuildIndex();
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
        // Keep NPC entities in the world; don't despawn on chunk unload.
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof NpcEntity) && !(entity instanceof GeckoNpcEntity)) {
            return;
        }
        registerExistingEntity(level, NpcClickHandler.getRegistry(), entity, true, bootstrapped);
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
            Entity existingInChunk = findExistingNpcEntity(level, registry, npcId, chunkPos);
            if (existingInChunk != null) {
            registerExistingEntity(level, registry, existingInChunk, true, true);
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
            registerExistingEntity(level, registry, entity, true, true);
        }
        for (GeckoNpcEntity entity : level.getEntitiesOfClass(GeckoNpcEntity.class, box)) {
            registerExistingEntity(level, registry, entity, true, true);
        }
    }

    public static void registerAllExisting(ServerLevel level, NpcRegistry registry) {
        AABB box = new AABB(
                -3.0E7, level.getMinBuildHeight(), -3.0E7,
                3.0E7, level.getMaxBuildHeight(), 3.0E7
        );
        for (NpcEntity entity : level.getEntitiesOfClass(NpcEntity.class, box)) {
            registerExistingEntity(level, registry, entity, true, true);
        }
        for (GeckoNpcEntity entity : level.getEntitiesOfClass(GeckoNpcEntity.class, box)) {
            registerExistingEntity(level, registry, entity, true, true);
        }
    }

    private static void registerExistingEntity(ServerLevel level, NpcRegistry registry, Entity entity, boolean allowResolve, boolean allowDiscard) {
        if (level == null || registry == null || entity == null) {
            return;
        }
        String npcId = null;
        if (entity instanceof NpcEntity npcEntity) {
            npcId = npcEntity.getNpcId();
        } else if (entity instanceof GeckoNpcEntity geckoEntity) {
            npcId = geckoEntity.getNpcId();
        }
        if (npcId == null || npcId.isEmpty()) {
            if (!allowResolve) {
                return;
            }
            npcId = resolveNpcIdFromName(registry, entity);
            if (npcId == null) {
                npcId = resolveNpcIdFromPosition(level, entity);
            }
            if (npcId == null) {
                npcId = resolveNpcIdFromRegistryPosition(registry, entity);
            }
            if (npcId != null) {
                if (entity instanceof NpcEntity npcEntity) {
                    npcEntity.setNpcId(npcId);
                } else if (entity instanceof GeckoNpcEntity geckoEntity) {
                    geckoEntity.setNpcId(npcId);
                }
                Marallyzen.LOGGER.info("NpcSpawner: Resolved npcId '{}' for existing entity {}", npcId, entity.getUUID());
            } else if (allowDiscard) {
                // No NPC id and no name match -> treat as stray and discard to avoid duplicate spawns.
                Marallyzen.LOGGER.warn("NpcSpawner: Removing stray NPC entity {} (empty npcId, no name match)", entity.getUUID());
                entity.remove(Entity.RemovalReason.DISCARDED);
                return;
            } else {
                return;
            }
        }
        Entity existing = registry.getNpc(npcId);
        if (existing != null && existing != entity) {
            entity.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        registry.registerExistingNpcEntity(entity);
        NpcSavedData.get(level).putState(
                npcId,
                new NpcState(level.dimension(), entity.blockPosition(), entity.getYRot(), npcId, null, null)
        );
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

    private static String resolveNpcIdFromPosition(ServerLevel level, Entity entity) {
        if (level == null || entity == null) {
            return null;
        }
        NpcSavedData data = NpcSavedData.get(level);
        if (data == null) {
            return null;
        }
        BlockPos pos = entity.blockPosition();
        if (pos == null) {
            return null;
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        Set<String> ids = data.getNpcIdsForChunk(level.dimension(), chunkPos.toLong());
        if (ids.isEmpty()) {
            return null;
        }
        String match = null;
        for (String npcId : ids) {
            NpcState state = data.getState(npcId);
            if (state == null || !level.dimension().equals(state.dimension())) {
                continue;
            }
            if (!pos.equals(state.pos())) {
                continue;
            }
            if (match != null && !match.equals(npcId)) {
                return null;
            }
            match = npcId;
        }
        return match;
    }

    private static String resolveNpcIdFromRegistryPosition(NpcRegistry registry, Entity entity) {
        if (registry == null || entity == null) {
            return null;
        }
        BlockPos pos = entity.blockPosition();
        if (pos == null) {
            return null;
        }
        String match = null;
        double matchDistSq = Double.MAX_VALUE;
        final double maxDistSq = 64.0;
        for (NpcData data : registry.getAllNpcData()) {
            if (data == null) {
                continue;
            }
            BlockPos spawnPos = data.getSpawnPos();
            if (spawnPos == null) {
                continue;
            }
            double distSq = pos.distSqr(spawnPos);
            if (distSq > maxDistSq) {
                continue;
            }
            String dataId = data.getId();
            if (dataId == null || dataId.isBlank()) {
                continue;
            }
            if (match != null && distSq == matchDistSq && !match.equals(dataId)) {
                return null;
            }
            if (distSq < matchDistSq) {
                matchDistSq = distSq;
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
            ChunkPos chunkPos = new ChunkPos(state.pos());
            Entity existingInChunk = findExistingNpcEntity(level, registry, npcId, chunkPos);
            if (existingInChunk != null) {
                registerExistingEntity(level, registry, existingInChunk, true, true);
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

    private static Entity findExistingNpcEntity(ServerLevel level, NpcRegistry registry, String npcId, ChunkPos chunkPos) {
        if (level == null || registry == null || npcId == null || npcId.isEmpty() || chunkPos == null) {
            return null;
        }
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX() + 1;
        int maxZ = chunkPos.getMaxBlockZ() + 1;
        AABB box = new AABB(minX, level.getMinBuildHeight(), minZ, maxX, level.getMaxBuildHeight(), maxZ);
        for (GeckoNpcEntity entity : level.getEntitiesOfClass(GeckoNpcEntity.class, box)) {
            if (matchesNpcId(registry, entity, npcId)) {
                return entity;
            }
        }
        for (NpcEntity entity : level.getEntitiesOfClass(NpcEntity.class, box)) {
            if (matchesNpcId(registry, entity, npcId)) {
                return entity;
            }
        }
        return null;
    }

    private static boolean matchesNpcId(NpcRegistry registry, Entity entity, String npcId) {
        if (entity == null || npcId == null || npcId.isEmpty()) {
            return false;
        }
        String existingId = null;
        if (entity instanceof NpcEntity npcEntity) {
            existingId = npcEntity.getNpcId();
        } else if (entity instanceof GeckoNpcEntity geckoEntity) {
            existingId = geckoEntity.getNpcId();
        }
        if (existingId != null && !existingId.isEmpty()) {
            return npcId.equals(existingId);
        }
        String resolved = resolveNpcIdFromName(registry, entity);
        if (resolved == null || !npcId.equals(resolved)) {
            return false;
        }
        if (entity instanceof NpcEntity npcEntity) {
            npcEntity.setNpcId(resolved);
        } else if (entity instanceof GeckoNpcEntity geckoEntity) {
            geckoEntity.setNpcId(resolved);
        }
        return true;
    }
}
