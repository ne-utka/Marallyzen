package neutka.marallys.marallyzen.npc;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
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

    private final Map<String, NpcState> npcStates = new HashMap<>();
    private transient Map<ResourceKey<Level>, Map<Long, Set<String>>> chunkIndex = new HashMap<>();

    public static NpcSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(NpcSavedData::new, NpcSavedData::load),
                DATA_NAME
        );
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

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<String, NpcState> entry : npcStates.entrySet()) {
            CompoundTag npcTag = new CompoundTag();
            npcTag.putString(KEY_ID, entry.getKey());
            npcTag.put(KEY_STATE, entry.getValue().toTag());
            list.add(npcTag);
        }
        tag.put(KEY_NPCS, list);
        return tag;
    }

    public static NpcSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        NpcSavedData data = new NpcSavedData();
        if (tag == null || !tag.contains(KEY_NPCS)) {
            return data;
        }
        ListTag list = tag.getList(KEY_NPCS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag npcTag = list.getCompound(i);
            String id = npcTag.getString(KEY_ID);
            NpcState state = NpcState.fromTag(npcTag.getCompound(KEY_STATE));
            if (id != null && !id.isEmpty() && state != null) {
                data.npcStates.put(id, state);
            }
        }
        data.rebuildIndex();
        return data;
    }

    public void updatePosition(String npcId, ServerLevel level, BlockPos pos, float yaw) {
        if (npcId == null || level == null || pos == null) {
            return;
        }
        NpcState existing = npcStates.get(npcId);
        String appearance = existing != null ? existing.appearanceId() : null;
        String ai = existing != null ? existing.aiState() : null;
        String dialog = existing != null ? existing.dialogState() : null;
        putState(npcId, new NpcState(level.dimension(), pos, yaw, appearance, ai, dialog));
    }
}
