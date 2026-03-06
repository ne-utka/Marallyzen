package neutka.marallys.marallyzen.npc.replay;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.npc.NpcClickHandler;

import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Server-side recorder for 1:1 player movement replay scripts.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public final class NpcReplayRecorder {
    public record Result(boolean success, String message) {
    }

    private static final NpcReplayRecorder INSTANCE = new NpcReplayRecorder();
    private static final int DEFAULT_MAX_RECORD_MINUTES = 10;
    private static final int MAX_COMPRESSED_FRAMES = 100_000;

    private final NpcReplayRegistry registry = new NpcReplayRegistry();
    private final NpcReplayLoader loader = NpcReplayLoader.getInstance();
    private final Set<UUID> forcedAttackTicks = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Queue<String>> emoteQueueByPlayer = new ConcurrentHashMap<>();

    private NpcReplayRecorder() {
    }

    public static NpcReplayRecorder getInstance() {
        return INSTANCE;
    }

    public NpcReplayRegistry registry() {
        return registry;
    }

    public Result startRecording(ServerPlayer player, String npcId, String scriptId) {
        if (player == null) {
            return new Result(false, "Command must be executed by a player.");
        }
        String normalizedNpc = normalize(npcId);
        String normalizedScript = normalize(scriptId);
        if (normalizedNpc.isBlank() || normalizedScript.isBlank()) {
            return new Result(false, "npcId and scriptId are required.");
        }
        if (registry.getRecording(player) != null) {
            return new Result(false, "You already have an active replay recording.");
        }
        Entity entity = NpcClickHandler.getRegistry().getNpc(normalizedNpc);
        if (entity == null) {
            return new Result(false, "NPC not found in world: " + normalizedNpc);
        }
        loader.ensureLoaded();
        long now = player.level().getGameTime();
        int maxTicks = DEFAULT_MAX_RECORD_MINUTES * 60 * 20;
        forcedAttackTicks.remove(player.getUUID());
        emoteQueueByPlayer.remove(player.getUUID());
        registry.startRecording(player, normalizedNpc, normalizedScript, now, maxTicks);
        neutka.marallys.marallyzen.Marallyzen.LOGGER.debug(
                "NpcReplayRecorder: start recording npc='{}' script='{}' player={}",
                normalizedNpc,
                normalizedScript,
                player.getScoreboardName()
        );
        return new Result(true, "Replay recording started: npc=" + normalizedNpc + ", id=" + normalizedScript);
    }

    public Result stopRecording(ServerPlayer player) {
        if (player == null) {
            return new Result(false, "Command must be executed by a player.");
        }
        NpcReplayRegistry.RecordingSession session = registry.stopRecording(player);
        if (session == null) {
            return new Result(false, "No active replay recording.");
        }
        forcedAttackTicks.remove(player.getUUID());
        emoteQueueByPlayer.remove(player.getUUID());
        return finalizeSession(session);
    }

    public void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (NpcReplayRegistry.RecordingSession session : registry.recordingSnapshot()) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId());
            if (player == null || !player.isAlive()) {
                continue;
            }
            boolean forcedAttack = forcedAttackTicks.remove(player.getUUID());
            String emoteId = pollEmote(player);
            NpcReplayFrame frame = NpcReplayFrame.fromPlayer(player, forcedAttack, emoteId);
            appendCompressed(session, frame);
            session.incrementTicks();
            if (session.frames().size() >= MAX_COMPRESSED_FRAMES) {
                registry.stopRecording(player);
                finalizeSession(session);
                continue;
            }
            if (session.recordedTicks() >= session.maxTicks()) {
                registry.stopRecording(player);
                finalizeSession(session);
            }
        }
    }

    private Result finalizeSession(NpcReplayRegistry.RecordingSession session) {
        NpcReplayScript script = new NpcReplayScript(
                session.scriptId(),
                false,
                NpcReplayScript.ReplayMode.PHYSICS,
                false,
                session.maxTicks(),
                session.npcId(),
                "",
                session.frames()
        );
        if (script.frames().isEmpty()) {
            return new Result(false, "Replay has no frames and was not saved.");
        }
        boolean saved = loader.save(script);
        if (!saved) {
            return new Result(false, "Failed to save replay script: " + session.scriptId());
        }
        return new Result(true, "Replay saved: " + session.scriptId() + " (" + script.frames().size() + " compressed frame(s)).");
    }

    private void appendCompressed(NpcReplayRegistry.RecordingSession session, NpcReplayFrame next) {
        if (session == null || next == null) {
            return;
        }
        var frames = session.frames();
        if (frames.isEmpty()) {
            frames.add(next.withRepeat(1));
            return;
        }
        int lastIndex = frames.size() - 1;
        NpcReplayFrame last = frames.get(lastIndex);
        if (last.nearlyEquals(next)) {
            frames.set(lastIndex, last.withRepeat(last.repeat() + 1));
            return;
        }
        frames.add(next.withRepeat(1));
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    private void flagAttack(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (registry.getRecording(player) == null) {
            return;
        }
        forcedAttackTicks.add(player.getUUID());
    }

    public void captureEmote(ServerPlayer player, String emoteId) {
        if (player == null || emoteId == null || emoteId.isBlank()) {
            return;
        }
        if (registry.getRecording(player) == null) {
            return;
        }
        emoteQueueByPlayer
                .computeIfAbsent(player.getUUID(), ignored -> new ConcurrentLinkedQueue<>())
                .offer(emoteId.trim());
    }

    private String pollEmote(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        Queue<String> queue = emoteQueueByPlayer.get(player.getUUID());
        if (queue == null) {
            return "";
        }
        String emote = queue.poll();
        if (queue.isEmpty()) {
            emoteQueueByPlayer.remove(player.getUUID(), queue);
        }
        return emote == null ? "" : emote;
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        INSTANCE.flagAttack(player);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        INSTANCE.flagAttack(player);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        INSTANCE.flagAttack(player);
    }
}
