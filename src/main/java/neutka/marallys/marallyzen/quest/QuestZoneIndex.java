package neutka.marallys.marallyzen.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestZoneIndex {
    private final Map<Long, List<QuestZoneDefinition>> zonesByChunk = new HashMap<>();

    public void clear() {
        zonesByChunk.clear();
    }

    public void indexZone(QuestZoneDefinition zone) {
        if (zone == null) {
            return;
        }
        for (ChunkPos chunkPos : getChunksForZone(zone)) {
            zonesByChunk.computeIfAbsent(chunkPos.toLong(), key -> new ArrayList<>()).add(zone);
        }
    }

    public List<QuestZoneDefinition> getZonesAt(BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        return zonesByChunk.getOrDefault(chunk.toLong(), List.of());
    }

    private List<ChunkPos> getChunksForZone(QuestZoneDefinition zone) {
        List<ChunkPos> chunks = new ArrayList<>();
        if (zone == null) {
            return chunks;
        }

        if (zone.shape() == QuestZoneDefinition.Shape.SPHERE) {
            int radius = (int) Math.ceil(zone.radius());
            BlockPos center = zone.center();
            if (center == null) {
                return chunks;
            }
            int minX = center.getX() - radius;
            int maxX = center.getX() + radius;
            int minZ = center.getZ() - radius;
            int maxZ = center.getZ() + radius;
            int minChunkX = Math.floorDiv(minX, 16);
            int maxChunkX = Math.floorDiv(maxX, 16);
            int minChunkZ = Math.floorDiv(minZ, 16);
            int maxChunkZ = Math.floorDiv(maxZ, 16);
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    chunks.add(new ChunkPos(cx, cz));
                }
            }
            return chunks;
        }

        BlockPos min = zone.min();
        BlockPos max = zone.max();
        if (min == null || max == null) {
            return chunks;
        }
        int minX = min.getX();
        int maxX = max.getX();
        int minZ = min.getZ();
        int maxZ = max.getZ();
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunks.add(new ChunkPos(cx, cz));
            }
        }
        return chunks;
    }
}
