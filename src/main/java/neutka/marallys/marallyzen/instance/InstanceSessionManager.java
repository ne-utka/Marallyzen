package neutka.marallys.marallyzen.instance;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.network.InstanceStatusPacket;
import neutka.marallys.marallyzen.network.InstanceRegistryPacket;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.ScreenFadePacket;
import neutka.marallys.marallyzen.quest.QuestDefinition;
import neutka.marallys.marallyzen.quest.QuestInstanceSpec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InstanceSessionManager {
    private static final InstanceSessionManager INSTANCE = new InstanceSessionManager(new InstanceWorldManager());

    private final InstanceWorldManager worldManager;
    private final Map<UUID, InstanceSession> sessions = new HashMap<>();
    private final Map<String, UUID> sessionsByQuest = new HashMap<>();
    private final Map<UUID, UUID> playerToSession = new HashMap<>();
    private final Map<UUID, PlayerState> playerStates = new HashMap<>();
    private final Set<UUID> restrictedPlayers = new HashSet<>();
    private final Map<String, Integer> worldUsage = new HashMap<>();
    private final Map<UUID, UUID> pendingInstanceRespawn = new HashMap<>();
    private final Set<UUID> blockedZoneEnter = new HashSet<>();
    private final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();
    private MinecraftServer server;

    private static final int WAIT_FADE_OUT = 10;
    private static final int WAIT_BLACK = 40;
    private static final int WAIT_FADE_IN = 10;
    private static final int PRE_TELEPORT_FADE_OUT = 5;
    private static final int PRE_TELEPORT_BLACK = 80;
    private static final int PRE_TELEPORT_FADE_IN = 5;
    private static final int PRE_TELEPORT_DELAY = PRE_TELEPORT_FADE_OUT + 2;

    private InstanceSessionManager(InstanceWorldManager worldManager) {
        this.worldManager = worldManager;
    }

    public static InstanceSessionManager getInstance() {
        return INSTANCE;
    }

    public void initialize(MinecraftServer server) {
        this.server = server;
    }

    public void preRegisterInstanceWorlds(Iterable<QuestDefinition> definitions) {
        if (server == null || definitions == null) {
            return;
        }
        int count = 0;
        for (QuestDefinition definition : definitions) {
            if (definition == null || definition.instanceSpec() == null) {
                continue;
            }
            String worldName = definition.instanceSpec().world();
            count++;
            Marallyzen.LOGGER.info("InstanceSessionManager: pre-registering instance world '{}'", worldName);
            worldManager.preRegisterInstanceWorld(server, worldName);
        }
        if (count > 0) {
            Marallyzen.LOGGER.info("InstanceSessionManager: pre-register pass complete ({} world(s))", count);
        }
    }

    public void shutdown() {
        for (UUID sessionId : new HashSet<>(sessions.keySet())) {
            endSession(sessionId, "shutdown");
        }
        restrictedPlayers.clear();
        playerStates.clear();
        pendingInstanceRespawn.clear();
        pendingTeleports.clear();
        blockedZoneEnter.clear();
    }

    public void onZoneEnter(ServerPlayer player, QuestDefinition definition, QuestInstanceSpec spec, String zoneId) {
        if (player == null || definition == null || spec == null || zoneId == null || zoneId.isBlank()) {
            return;
        }
        if (blockedZoneEnter.contains(player.getUUID())) {
            return;
        }
        if (playerToSession.containsKey(player.getUUID())) {
            return;
        }
        InstanceSession session = getOrCreateSession(definition, spec, zoneId, player.getUUID());
        if (session == null) {
            return;
        }
        if (session.state() != SessionState.WAITING) {
            return;
        }
        if (!session.tryAllowPlayer(player.getUUID())) {
            return;
        }
        Marallyzen.LOGGER.info(
                "InstanceSessionManager: zone_enter quest={} zone={} player={} state={} size={}/{}",
                definition.id(),
                zoneId,
                player.getGameProfile().getName(),
                session.state(),
                session.players().size() + 1,
                spec.groupRequired()
        );
        session.addPlayer(player.getUUID());
        playerToSession.put(player.getUUID(), session.sessionId());
        if (session.isReady()) {
            startSession(session);
        }
    }

    public void onQuestEnded(String questId) {
        if (questId == null || questId.isBlank()) {
            return;
        }
        UUID sessionId = sessionsByQuest.get(questId);
        if (sessionId == null) {
            return;
        }
        InstanceSession session = sessions.get(sessionId);
        if (session == null || server == null) {
            return;
        }
        for (UUID playerId : new HashSet<>(session.players())) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                session.removePlayer(playerId);
                continue;
            }
            leaveSession(player, "quest_end");
        }
        if (sessions.containsKey(sessionId) && session.players().isEmpty()) {
            endSession(sessionId, "quest_end_empty");
        }
    }

    public boolean leaveSession(ServerPlayer player, String reason) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUUID();
        setPlayerState(playerId, PlayerState.LEAVE_PENDING);
        Marallyzen.LOGGER.info(
                "[InstanceLeave] restoring snapshot player={} reason={}",
                player.getGameProfile().getName(),
                reason
        );
        if (restoreAfterLeave(player, reason)) {
            return true;
        }
        if (isInstanceDimension(player) && playerToSession.containsKey(playerId)) {
            restrictedPlayers.add(playerId);
            setPlayerState(playerId, PlayerState.IN_INSTANCE);
            return false;
        }
        setPlayerState(playerId, PlayerState.OUTSIDE);
        detachFromSessions(player, reason);
        clearRestrictions(player);
        NetworkHelper.sendToPlayer(player, new InstanceStatusPacket(false, null));
        return false;
    }

    public void onPlayerLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        pendingTeleports.remove(playerId);
        UUID sessionId = playerToSession.remove(playerId);
        restrictedPlayers.remove(playerId);
        if (isInstanceDimension(player)) {
            setPlayerState(playerId, PlayerState.LOGIN_RESTORE_PENDING);
        } else {
            setPlayerState(playerId, PlayerState.OUTSIDE);
        }
        if (sessionId == null) {
            return;
        }
        InstanceSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        session.removePlayer(playerId);
        if (playerId.equals(session.leaderId())) {
            promoteLeader(session);
        }
        if (session.players().isEmpty()) {
            endSession(sessionId, "logout");
        }
    }

    public boolean isPlayerRestricted(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUUID();
        if (!restrictedPlayers.contains(playerId)) {
            return false;
        }
        PlayerState state = playerStates.getOrDefault(playerId, PlayerState.OUTSIDE);
        if (state != PlayerState.IN_INSTANCE && state != PlayerState.IN_INSTANCE_DEAD) {
            return false;
        }
        UUID sessionId = playerToSession.get(playerId);
        if (sessionId == null) {
            return false;
        }
        InstanceSession session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        if (!isInstanceDimension(player)) {
            return false;
        }
        return session.state() == SessionState.LOADING || session.state() == SessionState.ACTIVE;
    }

    public PlayerState getPlayerState(UUID playerId) {
        if (playerId == null) {
            return PlayerState.OUTSIDE;
        }
        return playerStates.getOrDefault(playerId, PlayerState.OUTSIDE);
    }

    private void setPlayerState(UUID playerId, PlayerState state) {
        if (playerId == null) {
            return;
        }
        if (state == null || state == PlayerState.OUTSIDE) {
            playerStates.remove(playerId);
            return;
        }
        playerStates.put(playerId, state);
    }

    public boolean isPlayerInSession(UUID playerId) {
        return playerId != null && playerToSession.containsKey(playerId);
    }

    private boolean restoreAfterLeave(ServerPlayer player, String reason) {
        return restoreFromSnapshotInternal(player, reason, true);
    }

    public boolean restoreAfterLogin(ServerPlayer player, String reason) {
        return restoreFromSnapshotInternal(player, reason, false);
    }

    private boolean restoreFromSnapshotInternal(ServerPlayer player, String reason, boolean blockZoneEnter) {
        if (player == null) {
            return false;
        }
        PlayerSnapshot snapshot = PlayerSnapshot.loadFromPlayer(player);
        if (snapshot == null) {
            Marallyzen.LOGGER.info(
                    "LOGIN_RESTORE_SKIPPED player={} reason=no_snapshot",
                    player.getGameProfile().getName()
            );
            return false;
        }
        setPlayerState(player.getUUID(), blockZoneEnter ? PlayerState.LEAVE_PENDING : PlayerState.LOGIN_RESTORE_PENDING);
        Marallyzen.LOGGER.info(
                "LOGIN_SNAPSHOT_FOUND player={} dim={} snapshotDim={}",
                player.getGameProfile().getName(),
                player.level().dimension().location(),
                snapshot.dimension() != null ? snapshot.dimension().location() : null
        );
        clearRestrictions(player);
        boolean restored = snapshot.restore(player);
        if (!restored) {
            UUID playerId = player.getUUID();
            if (isInstanceDimension(player) && playerToSession.containsKey(playerId)) {
                restrictedPlayers.add(playerId);
                setPlayerState(playerId, PlayerState.IN_INSTANCE);
            } else {
                setPlayerState(playerId, PlayerState.OUTSIDE);
            }
            Marallyzen.LOGGER.warn(
                    "LOGIN_RESTORE_FAILED player={} snapshot kept",
                    player.getGameProfile().getName()
            );
            return false;
        }
        detachFromSessions(player, reason);
        PlayerSnapshot.clearFromPlayer(player);
        setPlayerState(player.getUUID(), PlayerState.OUTSIDE);
        NetworkHelper.sendToPlayer(player, new InstanceStatusPacket(false, null));
        if (blockZoneEnter) {
            blockInstanceZoneEnter(player, reason);
        }
        Marallyzen.LOGGER.info(
                "LOGIN_RESTORE_FORCED player={} dim={} reason={}",
                player.getGameProfile().getName(),
                player.level().dimension().location(),
                reason
        );
        return true;
    }

    private void forceEndAnySession(ServerPlayer player, String reason) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        restrictedPlayers.remove(playerId);
        playerToSession.remove(playerId);
        pendingInstanceRespawn.remove(playerId);
        blockedZoneEnter.remove(playerId);
        pendingTeleports.remove(playerId);
        setPlayerState(playerId, PlayerState.OUTSIDE);
        for (Map.Entry<UUID, InstanceSession> entry : new HashMap<>(sessions).entrySet()) {
            InstanceSession session = entry.getValue();
            if (session != null && session.players().contains(playerId)) {
                session.removePlayer(playerId);
                if (playerId.equals(session.leaderId())) {
                    promoteLeader(session);
                }
                if (session.players().isEmpty()) {
                    endSession(entry.getKey(), reason);
                }
                break;
            }
        }
    }

    private void detachFromSessions(ServerPlayer player, String reason) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        restrictedPlayers.remove(playerId);
        playerToSession.remove(playerId);
        pendingInstanceRespawn.remove(playerId);
        pendingTeleports.remove(playerId);
        setPlayerState(playerId, PlayerState.OUTSIDE);
        for (Map.Entry<UUID, InstanceSession> entry : new HashMap<>(sessions).entrySet()) {
            InstanceSession session = entry.getValue();
            if (session == null) {
                continue;
            }
            if (session.players().contains(playerId)) {
                session.removePlayer(playerId);
                if (playerId.equals(session.leaderId())) {
                    promoteLeader(session);
                }
                if (session.players().isEmpty()) {
                    endSession(entry.getKey(), reason);
                }
                break;
            }
        }
    }

    public void clearRestrictions(ServerPlayer player) {
        if (player == null) {
            return;
        }
        restrictedPlayers.remove(player.getUUID());
    }

    public void markOutside(ServerPlayer player) {
        if (player == null) {
            return;
        }
        setPlayerState(player.getUUID(), PlayerState.OUTSIDE);
    }

    private void clearPlayerState(ServerPlayer player, String reason) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        UUID sessionId = playerToSession.remove(playerId);
        restrictedPlayers.remove(playerId);
        setPlayerState(playerId, PlayerState.OUTSIDE);
        if (sessionId == null) {
            return;
        }
        InstanceSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        session.removePlayer(playerId);
        if (session.players().isEmpty()) {
            endSession(sessionId, reason);
        }
    }

    public void markInstanceDeath(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!isInstanceDimension(player)) {
            return;
        }
        UUID playerId = player.getUUID();
        if (getPlayerState(playerId) != PlayerState.IN_INSTANCE) {
            return;
        }
        UUID sessionId = playerToSession.get(playerId);
        if (sessionId == null) {
            return;
        }
        InstanceSession session = sessions.get(sessionId);
        if (session == null || session.state() != SessionState.ACTIVE) {
            return;
        }
        pendingInstanceRespawn.put(playerId, sessionId);
        setPlayerState(playerId, PlayerState.IN_INSTANCE_DEAD);
        Marallyzen.LOGGER.info(
                "[InstanceDeath] queued respawn player={} session={}",
                player.getGameProfile().getName(),
                sessionId
        );
    }

    public boolean isPendingInstanceRespawn(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return pendingInstanceRespawn.containsKey(player.getUUID());
    }

    public void carryPendingInstanceRespawn(ServerPlayer original, ServerPlayer clone) {
        if (original == null || clone == null) {
            return;
        }
        UUID sessionId = pendingInstanceRespawn.remove(original.getUUID());
        if (sessionId == null) {
            return;
        }
        pendingInstanceRespawn.put(clone.getUUID(), sessionId);
    }

    public void blockInstanceZoneEnter(ServerPlayer player, String reason) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        blockedZoneEnter.add(playerId);
        Marallyzen.LOGGER.info(
                "[InstanceLeave] zone_enter blocked player={} reason={}",
                player.getGameProfile().getName(),
                reason
        );
    }

    public boolean isInstanceZoneEnterBlocked(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return blockedZoneEnter.contains(player.getUUID());
    }

    public void clearInstanceZoneEnterBlock(ServerPlayer player) {
        if (player == null) {
            return;
        }
        blockedZoneEnter.remove(player.getUUID());
    }

    private UUID consumePendingInstanceRespawn(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return pendingInstanceRespawn.remove(player.getUUID());
    }

    public void onPlayerRespawn(ServerPlayer player) {
        if (player == null || server == null) {
            return;
        }
        UUID sessionId = playerToSession.get(player.getUUID());
        if (sessionId == null) {
            sessionId = consumePendingInstanceRespawn(player);
        } else {
            pendingInstanceRespawn.remove(player.getUUID());
        }
        if (sessionId == null) {
            return;
        }
        InstanceSession session = sessions.get(sessionId);
        if (session == null || session.spec() == null) {
            return;
        }
        ServerLevel target = worldManager.getOrLoadWorld(server, session.spec().world());
        if (target == null) {
            return;
        }
        BlockPos spawn = session.spawn();
        if (spawn == null) {
            spawn = session.spec().spawn() != null ? session.spec().spawn() : target.getSharedSpawnPos();
            if (session.spec().spawn() == null) {
                spawn = clampSpawnHeight(target, spawn);
            }
            session.setSpawn(spawn);
        }
        player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0f;
        setPlayerState(player.getUUID(), PlayerState.IN_INSTANCE);
        Marallyzen.LOGGER.info(
                "[InstanceDeath] respawn in instance player={} dim={} spawn={}",
                player.getGameProfile().getName(),
                target.dimension().location(),
                spawn
        );
    }

    private InstanceSession getOrCreateSession(QuestDefinition definition, QuestInstanceSpec spec, String zoneId, UUID leaderId) {
        String questId = definition.id();
        UUID existing = sessionsByQuest.get(questId);
        if (existing != null) {
            return sessions.get(existing);
        }
        UUID sessionId = UUID.randomUUID();
        InstanceSession session = new InstanceSession(sessionId, questId, zoneId, spec, leaderId);
        sessions.put(sessionId, session);
        sessionsByQuest.put(questId, sessionId);
        return session;
    }

    private void startSession(InstanceSession session) {
        if (session == null || session.state() != SessionState.WAITING || server == null) {
            return;
        }
        session.setState(SessionState.LOADING);
        Marallyzen.LOGGER.info(
                "InstanceSessionManager: starting session {} quest={} players={}",
                session.sessionId(),
                session.questId(),
                session.players().size()
        );
        QuestInstanceSpec spec = session.spec();
        ServerLevel target = worldManager.getOrLoadWorld(server, spec.world());
        if (target == null) {
            notifySessionFailure(session, "Instance world not loaded: " + spec.world());
            endSession(session.sessionId(), "world_missing");
            return;
        }
        worldUsage.merge(spec.world(), 1, Integer::sum);
        BlockPos spawn = spec.spawn() != null ? spec.spawn() : target.getSharedSpawnPos();
        if (spec.spawn() == null) {
            spawn = clampSpawnHeight(target, spawn);
        }
        session.setSpawn(spawn);
        Marallyzen.LOGGER.info(
                "InstanceSessionManager: teleporting session {} to world={} spawn={}",
                session.sessionId(),
                spec.world(),
                spawn
        );
        ensureChunkLoaded(target, spawn);
        Marallyzen.LOGGER.info(
                "InstanceSessionManager: chunk ticket placed for session {}",
                session.sessionId()
        );
        for (UUID playerId : session.players()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                Marallyzen.LOGGER.warn(
                        "InstanceSessionManager: player {} missing during teleport for session {}",
                        playerId,
                        session.sessionId()
                );
                continue;
            }
            if (!session.snapshots().containsKey(playerId)) {
                PlayerSnapshot snapshot = PlayerSnapshot.capture(player);
                session.snapshots().put(playerId, snapshot);
                snapshot.saveToPlayer(player);
            }
            NetworkHelper.sendToPlayer(player, new ScreenFadePacket(
                    PRE_TELEPORT_FADE_OUT,
                    PRE_TELEPORT_BLACK,
                    PRE_TELEPORT_FADE_IN,
                    Component.empty(),
                    Component.empty(),
                    true,
                    null
            ));
            setPlayerState(playerId, PlayerState.ENTER_PENDING);
            pendingTeleports.put(playerId, new PendingTeleport(session.sessionId(), PRE_TELEPORT_DELAY));
        }
    }

    public void tickPendingTeleport(ServerPlayer player) {
        if (player == null || server == null) {
            return;
        }
        UUID playerId = player.getUUID();
        PendingTeleport pending = pendingTeleports.get(playerId);
        if (pending == null) {
            return;
        }
        if (pending.ticksRemaining > 0) {
            pending.ticksRemaining--;
            return;
        }
        InstanceSession session = sessions.get(pending.sessionId);
        if (session == null) {
            pendingTeleports.remove(playerId);
            return;
        }
        QuestInstanceSpec spec = session.spec();
        ServerLevel target = worldManager.getOrLoadWorld(server, spec.world());
        BlockPos spawn = session.spawn();
        if (target == null || spawn == null) {
            pendingTeleports.remove(playerId);
            return;
        }
        Marallyzen.LOGGER.info(
                "InstanceSessionManager: preparing teleport for {} to {} {} {}",
                player.getGameProfile().getName(),
                target.dimension().location(),
                spawn.getX(),
                spawn.getZ()
        );
        NetworkHelper.sendToPlayer(player, new InstanceRegistryPacket(spec.world()));
        if (spec.clearInventory()) {
            player.getInventory().clearContent();
        }
        GameType mode = spec.mode() != null ? spec.mode() : GameType.ADVENTURE;
        player.setGameMode(mode);
        restrictedPlayers.add(playerId);
        try {
            player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
        } catch (Exception e) {
            Marallyzen.LOGGER.warn(
                    "InstanceSessionManager: teleport failed for {} session {}",
                    player.getGameProfile().getName(),
                    session.sessionId(),
                    e
            );
            pendingTeleports.remove(playerId);
            restoreAfterLeave(player, "teleport_failed");
            return;
        }
        Marallyzen.LOGGER.info(
                "InstanceSessionManager: teleported player {} to {} {} {}",
                player.getGameProfile().getName(),
                target.dimension().location(),
                spawn.getX(),
                spawn.getZ()
        );
        Marallyzen.LOGGER.info(
                "InstanceSessionManager: post-teleport server pos={} dim={}",
                player.blockPosition(),
                player.level().dimension().location()
        );
        NetworkHelper.sendToPlayer(player, new InstanceStatusPacket(true, session.questId()));
        sendWaitingOverlay(player);
        pendingTeleports.remove(playerId);
        setPlayerState(playerId, PlayerState.IN_INSTANCE);
        if (session.state() == SessionState.LOADING && !hasPendingTeleports(session.sessionId())) {
            session.setState(SessionState.ACTIVE);
        }
    }

    private boolean hasPendingTeleports(UUID sessionId) {
        if (sessionId == null) {
            return false;
        }
        for (PendingTeleport pending : pendingTeleports.values()) {
            if (sessionId.equals(pending.sessionId)) {
                return true;
            }
        }
        return false;
    }

    private void endSession(UUID sessionId, String reason) {
        InstanceSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        session.setState(SessionState.ENDING);
        sessionsByQuest.remove(session.questId());
        if (server == null) {
            for (UUID playerId : new HashSet<>(session.players())) {
                playerToSession.remove(playerId);
                restrictedPlayers.remove(playerId);
                setPlayerState(playerId, PlayerState.OUTSIDE);
            }
            return;
        }
        for (UUID playerId : new HashSet<>(session.players())) {
            playerToSession.remove(playerId);
            restrictedPlayers.remove(playerId);
            setPlayerState(playerId, PlayerState.OUTSIDE);
            pendingTeleports.remove(playerId);
        }
        decrementWorldUsage(session.spec() != null ? session.spec().world() : null);
        session.setState(SessionState.CLOSED);
        Marallyzen.LOGGER.info("InstanceSessionManager: ended session {} reason={} quest={}", sessionId, reason, session.questId());
    }

    private void decrementWorldUsage(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        Integer count = worldUsage.get(worldName);
        if (count == null) {
            return;
        }
        if (count <= 1) {
            worldUsage.remove(worldName);
            worldManager.unloadWorld(server, worldName);
            return;
        }
        worldUsage.put(worldName, count - 1);
    }

    private boolean isInstanceDimension(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        var key = player.level().dimension().location();
        return Marallyzen.MODID.equals(key.getNamespace()) && key.getPath().startsWith("instance/");
    }

    private void sendWaitingOverlay(InstanceSession session) {
        if (session == null || server == null) {
            return;
        }
        Component title = Component.translatable("screen.marallyzen.instance_waiting");
        for (UUID playerId : session.players()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            Marallyzen.LOGGER.info(
                    "InstanceSessionManager: sending waiting overlay to {}",
                    player.getGameProfile().getName()
            );
            NetworkHelper.sendToPlayer(player, new ScreenFadePacket(
                    WAIT_FADE_OUT,
                    WAIT_BLACK,
                    WAIT_FADE_IN,
                    title,
                    null,
                    true,
                    null
            ));
        }
    }

    private void sendWaitingOverlay(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Component title = Component.translatable("screen.marallyzen.instance_waiting");
        NetworkHelper.sendToPlayer(player, new ScreenFadePacket(
                WAIT_FADE_OUT,
                WAIT_BLACK,
                WAIT_FADE_IN,
                title,
                null,
                true,
                null
        ));
    }

    private void notifySessionFailure(InstanceSession session, String message) {
        if (session == null || server == null) {
            return;
        }
        Component text = Component.literal(message);
        for (UUID playerId : session.players()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.sendSystemMessage(text);
            }
        }
    }

    private void promoteLeader(InstanceSession session) {
        if (session == null) {
            return;
        }
        if (!session.players().isEmpty()) {
            UUID newLeader = session.players().iterator().next();
            session.setLeaderId(newLeader);
        } else {
            session.setLeaderId(null);
        }
    }

    private void ensureChunkLoaded(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, 0);
    }

    private BlockPos clampSpawnHeight(ServerLevel level, BlockPos base) {
        if (level == null || base == null) {
            return base;
        }
        try {
            int minY = level.getMinBuildHeight();
            int y = base.getY();
            if (y <= minY + 1) {
                y = Math.max(level.getSeaLevel(), minY + 1);
            }
            return new BlockPos(base.getX(), y, base.getZ());
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("InstanceSessionManager: failed to clamp spawn at {}", base, e);
            return base;
        }
    }

    private static final class PendingTeleport {
        private final UUID sessionId;
        private int ticksRemaining;

        private PendingTeleport(UUID sessionId, int ticksRemaining) {
            this.sessionId = sessionId;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
