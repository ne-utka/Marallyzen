package neutka.marallys.marallyzen.instance;

import net.minecraft.core.BlockPos;
import neutka.marallys.marallyzen.quest.QuestInstanceSpec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InstanceSession {
    public enum State {
        WAITING,
        LOADING,
        ACTIVE,
        ENDING
    }

    private final UUID sessionId;
    private final String questId;
    private final String zoneId;
    private final QuestInstanceSpec spec;
    private State state;
    private final Set<UUID> players = new HashSet<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private BlockPos spawn;

    public InstanceSession(UUID sessionId, String questId, String zoneId, QuestInstanceSpec spec) {
        this.sessionId = sessionId;
        this.questId = questId;
        this.zoneId = zoneId;
        this.spec = spec;
        this.state = State.WAITING;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public String questId() {
        return questId;
    }

    public String zoneId() {
        return zoneId;
    }

    public QuestInstanceSpec spec() {
        return spec;
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Set<UUID> players() {
        return players;
    }

    public Map<UUID, PlayerSnapshot> snapshots() {
        return snapshots;
    }

    public BlockPos spawn() {
        return spawn;
    }

    public void setSpawn(BlockPos spawn) {
        this.spawn = spawn;
    }

    public boolean addPlayer(UUID playerId) {
        return players.add(playerId);
    }

    public boolean removePlayer(UUID playerId) {
        snapshots.remove(playerId);
        return players.remove(playerId);
    }

    public boolean isReady() {
        int required = spec != null ? Math.max(1, spec.groupRequired()) : 1;
        return players.size() >= required;
    }
}
