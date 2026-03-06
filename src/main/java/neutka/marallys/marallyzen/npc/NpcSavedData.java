package neutka.marallys.marallyzen.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NpcSavedData extends SavedData {
    public static final String DATA_NAME = "marallyzen_npcs";
    private static final String KEY_NPCS = "npcs";
    private static final String KEY_ID = "id";
    private static final String KEY_STATE = "state";
    public static final Codec<NpcSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, NpcState.CODEC)
                    .fieldOf(KEY_NPCS)
                    .forGetter(data -> data.npcStates)
    ).apply(instance, map -> {
        NpcSavedData data = new NpcSavedData();
        data.npcStates.putAll(map);
        data.rebuildIndex();
        return data;
    }));
    public static final SavedDataType<NpcSavedData> TYPE = new SavedDataType<>(DATA_NAME, NpcSavedData::new, CODEC);

    private final Map<String, NpcState> npcStates = new HashMap<>();
    private transient Map<ResourceKey<Level>, Map<Long, Set<String>>> chunkIndex = new HashMap<>();

    public static NpcSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<String, NpcState> getNpcStates() {
        return npcStates;
    }

    public NpcState getState(String npcId) {
        return npcStates.get(npcId);
    }

    public void putState(String npcId, NpcState state) {
        if (npcId == null || state == null) {
            return;
        }
        npcStates.put(npcId, state);
        indexState(npcId, state);
        setDirty();
    }

    public void removeState(String npcId) {
        if (npcId == null) {
            return;
        }
        npcStates.remove(npcId);
        rebuildIndex();
        setDirty();
    }

    public Set<String> getNpcIdsForChunk(ResourceKey<Level> dimension, long chunkKey) {
        if (dimension == null) {
            return Collections.emptySet();
        }
        Map<Long, Set<String>> dimIndex = chunkIndex.get(dimension);
        if (dimIndex == null) {
            return Collections.emptySet();
        }
        return dimIndex.getOrDefault(chunkKey, Collections.emptySet());
    }

    public void rebuildIndex() {
        chunkIndex = new HashMap<>();
        for (Map.Entry<String, NpcState> entry : npcStates.entrySet()) {
            indexState(entry.getKey(), entry.getValue());
        }
    }

    private void indexState(String npcId, NpcState state) {
        if (npcId == null || state == null) {
            return;
        }
        Map<Long, Set<String>> dimIndex = chunkIndex.computeIfAbsent(state.dimension(), key -> new HashMap<>());
        long chunkKey = ChunkPos.asLong(state.pos().getX() >> 4, state.pos().getZ() >> 4);
        dimIndex.computeIfAbsent(chunkKey, key -> new HashSet<>()).add(npcId);
    }

    // SavedData serialization is handled via CODEC.

    public void updatePosition(String npcId, ServerLevel level, BlockPos pos, float yaw) {
        if (npcId == null || level == null || pos == null) {
            return;
        }
        NpcState existing = npcStates.get(npcId);
        String appearance = existing != null ? existing.appearanceId() : null;
        String ai = existing != null ? existing.aiState() : null;
        String dialog = existing != null ? existing.dialogState() : null;
        String replayId = existing != null ? existing.currentReplayId() : null;
        int replayFrame = existing != null ? existing.replayFrameIndex() : 0;
        boolean replayRunning = existing != null && existing.replayRunning();
        putState(npcId, new NpcState(level.dimension(), pos, yaw, appearance, ai, dialog, replayId, replayFrame, replayRunning));
    }

    public void updateReplay(String npcId, ServerLevel level, String replayId, int frameIndex, boolean running) {
        if (npcId == null || level == null) {
            return;
        }
        NpcState existing = npcStates.get(npcId);
        BlockPos pos = existing != null ? existing.pos() : BlockPos.ZERO;
        float yaw = existing != null ? existing.yaw() : 0.0F;
        String appearance = existing != null ? existing.appearanceId() : null;
        String ai = existing != null ? existing.aiState() : null;
        String dialog = existing != null ? existing.dialogState() : null;
        String normalizedReplayId = (replayId == null || replayId.isBlank()) ? null : replayId;
        putState(npcId, new NpcState(
                level.dimension(),
                pos,
                yaw,
                appearance,
                ai,
                dialog,
                normalizedReplayId,
                Math.max(0, frameIndex),
                running
        ));
    }
}
