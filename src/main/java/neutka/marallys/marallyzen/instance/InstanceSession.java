package neutka.marallys.marallyzen.instance;

import net.minecraft.core.BlockPos;
import neutka.marallys.marallyzen.quest.QuestInstanceSpec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InstanceSession {
    private final UUID sessionId;
    private final String questId;
    private final String zoneId;
    private final QuestInstanceSpec spec;
    private SessionState state;
    private UUID leaderId;
    private final Set<UUID> allowedPlayers = new HashSet<>();
    private final Set<UUID> players = new HashSet<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private BlockPos spawn;

    public InstanceSession(UUID sessionId, String questId, String zoneId, QuestInstanceSpec spec, UUID leaderId) {
        this.sessionId = sessionId;
        this.questId = questId;
        this.zoneId = zoneId;
        this.spec = spec;
        this.state = SessionState.WAITING;
        this.leaderId = leaderId;
        if (leaderId != null) {
            allowedPlayers.add(leaderId);
        }
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

    public SessionState state() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public UUID leaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
        if (leaderId != null) {
            allowedPlayers.add(leaderId);
        }
    }

    public Set<UUID> allowedPlayers() {
        return allowedPlayers;
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

    public boolean tryAllowPlayer(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        if (allowedPlayers.contains(playerId)) {
            return true;
        }
        if (allowedPlayers.size() >= requiredPlayers()) {
            return false;
        }
        return allowedPlayers.add(playerId);
    }

    public boolean removePlayer(UUID playerId) {
        snapshots.remove(playerId);
        return players.remove(playerId);
    }

    public boolean isReady() {
        return players.size() >= requiredPlayers();
    }

    public boolean canAcceptPlayer(UUID playerId) {
        return playerId != null && allowedPlayers.contains(playerId);
    }

    public int requiredPlayers() {
        return spec != null ? Math.max(1, spec.groupRequired()) : 1;
    }
}
