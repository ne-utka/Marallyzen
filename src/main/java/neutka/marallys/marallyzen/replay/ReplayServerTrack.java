package neutka.marallys.marallyzen.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReplayServerTrack {
    private final String dimension;
    private final Map<UUID, ReplayEntityInfo> entities = new LinkedHashMap<>();
    private final List<ReplayServerSnapshot> snapshots = new ArrayList<>();
    private final List<ReplayChunkSnapshot> chunkSnapshots = new ArrayList<>();

    public ReplayServerTrack(String dimension) {
        this.dimension = dimension;
    }

    public String getDimension() {
        return dimension;
    }

    public Map<UUID, ReplayEntityInfo> getEntities() {
        return Collections.unmodifiableMap(entities);
    }

    public List<ReplayServerSnapshot> getSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    public List<ReplayChunkSnapshot> getChunkSnapshots() {
        return Collections.unmodifiableList(chunkSnapshots);
    }

    public void addEntityInfo(ReplayEntityInfo info) {
        if (info == null) {
            return;
        }
        entities.putIfAbsent(info.id(), info);
    }

    public void addSnapshot(ReplayServerSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        snapshots.add(snapshot);
    }

    public void addChunkSnapshot(ReplayChunkSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        chunkSnapshots.add(snapshot);
    }
}
