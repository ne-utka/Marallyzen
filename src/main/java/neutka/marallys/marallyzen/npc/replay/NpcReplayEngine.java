package neutka.marallys.marallyzen.npc.replay;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import neutka.marallys.marallyzen.npc.NpcAnimationHandler;
import neutka.marallys.marallyzen.npc.NpcClickHandler;
import neutka.marallys.marallyzen.npc.NpcDefaultAnimationHandler;
import neutka.marallys.marallyzen.npc.NpcSavedData;
import neutka.marallys.marallyzen.npc.NpcState;
import neutka.marallys.marallyzen.npc.NpcWorldPolicy;
import neutka.marallys.marallyzen.util.PermissionHelper;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcReplayEngine {
    public record Result(boolean success, String message) {
    }

    public record Status(
            boolean replayRunning,
            String npcId,
            String currentReplayId,
            int currentFrameIndex,
            int totalFrames,
            boolean loop,
            String mode,
            boolean interpolate,
            ReplayStopReason lastStopReason,
            long uptimeTicks,
            double uptimeSeconds,
            Double npcX,
            Double npcY,
            Double npcZ,
            Double scriptStartX,
            Double scriptStartY,
            Double scriptStartZ,
            long lastTickCostNanos,
            long averageTickCostNanos,
            long scriptMemoryBytes
    ) {
    }

    private record PreviewSnapshot(
            double x,
            double y,
            double z,
            double dx,
            double dy,
            double dz,
            float yaw,
            float pitch,
            float bodyYaw,
            float headYaw,
            boolean sprinting,
            boolean crouching,
            boolean noGravity,
            boolean swimming,
            float fallDistance,
            float movementSpeed,
            boolean playerFlying
    ) {
        static PreviewSnapshot from(Entity entity) {
            float bodyYaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
            float headYaw = entity instanceof LivingEntity living ? living.yHeadRot : entity.getYRot();
            float speed = 0.1F;
            if (entity instanceof LivingEntity living) {
                AttributeInstance movement = living.getAttribute(Attributes.MOVEMENT_SPEED);
                if (movement != null) {
                    speed = (float) movement.getBaseValue();
                }
            }
            boolean playerFlying = entity instanceof ServerPlayer player && player.getAbilities().flying;
            var delta = entity.getDeltaMovement();
            return new PreviewSnapshot(
                    entity.getX(), entity.getY(), entity.getZ(),
                    delta.x, delta.y, delta.z,
                    entity.getYRot(), entity.getXRot(), bodyYaw, headYaw,
                    entity.isSprinting(), entity.isShiftKeyDown(),
                    entity.isNoGravity(), entity.isSwimming(),
                    (float) entity.fallDistance, speed, playerFlying
            );
        }
    }

    private record LastSnapshot(
            String replayId,
            int frameIndex,
            long uptimeTicks,
            ReplayStopReason stopReason,
            long lastTickCostNanos,
            long averageTickCostNanos
    ) {
    }

    private static final NpcReplayEngine INSTANCE = new NpcReplayEngine();
    private static final int STATE_SCAN_INTERVAL_TICKS = 40;
    private static final int MAX_STUCK_TICKS = 10;
    private static final int WATCHDOG_FREEZE_GAP_TICKS = 200;
    private static final long RESUME_WORLD_TIME_DRIFT_LIMIT_TICKS = 50_000_000L;
    private static final double PHYSICS_HARD_RESYNC_DISTANCE_SQ = 256.0D;
    private static final double DESYNC_STOP_DISTANCE_SQ = 100.0D;
    private static final double EXACT_EXPECTED_DISTANCE_SQ = 64.0D;

    private final NpcReplayLoader loader = NpcReplayLoader.getInstance();
    private final NpcReplayRegistry registry = NpcReplayRecorder.getInstance().registry();
    private final Map<String, PreviewSnapshot> previewSnapshots = new ConcurrentHashMap<>();
    private final Map<String, LastSnapshot> lastSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, String> debugVisualTargets = new ConcurrentHashMap<>();
    private final Set<String> poseLockedNpcs = ConcurrentHashMap.newKeySet();
    private MinecraftServer server;
    private int stateScanTicks;
    private boolean debugEnabled;

    private NpcReplayEngine() {
    }

    public static NpcReplayEngine getInstance() {
        return INSTANCE;
    }

    public static Result play(String npcId, String replayId) {
        return INSTANCE.playInternal(npcId, replayId, null, 0, false, true, false);
    }

    public static Result play(String npcId, String replayId, Runnable onComplete) {
        return INSTANCE.playInternal(npcId, replayId, onComplete, 0, false, true, false);
    }

    public static Result preview(String npcId, String replayId) {
        return INSTANCE.playInternal(npcId, replayId, null, 0, false, false, true);
    }

    public static Result stop(String npcId) {
        return INSTANCE.stopInternal(npcId, true, ReplayStopReason.MANUAL);
    }

    public static Status status(String npcId) {
        return INSTANCE.buildStatus(npcId);
    }

    public static boolean isDebugEnabled() {
        return INSTANCE.debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        INSTANCE.debugEnabled = enabled;
        Marallyzen.LOGGER.info("NpcReplayEngine: debug mode {}", enabled ? "enabled" : "disabled");
    }

    public static Result toggleDebugVisual(ServerPlayer player, String npcId) {
        return INSTANCE.toggleDebugVisualInternal(player, npcId);
    }

    public static void tick(MinecraftServer server) {
        INSTANCE.tickInternal(server);
    }

    public static void initialize(MinecraftServer server) {
        INSTANCE.initializeInternal(server);
    }

    public static void reload() {
        INSTANCE.loader.reload();
    }

    public static Set<String> loadedReplayIds() {
        return INSTANCE.loader.ids();
    }

    public static Map<String, NpcReplayScript> loadedReplayScripts() {
        return INSTANCE.loader.snapshot();
    }

    public static boolean isReplayRunning(String npcId) {
        if (npcId == null || npcId.isBlank()) {
            return false;
        }
        return INSTANCE.registry.getActive(npcId.trim().toLowerCase(Locale.ROOT)) != null;
    }

    public static boolean isPoseLocked(String npcId) {
        if (npcId == null || npcId.isBlank()) {
            return false;
        }
        return INSTANCE.poseLockedNpcs.contains(npcId.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isReplayRunningByScript(String replayId) {
        String normalized = normalize(replayId);
        if (normalized.isBlank()) {
            return false;
        }
        for (NpcReplayState state : registry.activeSnapshot()) {
            if (state != null && normalized.equals(state.replayId())) {
                return true;
            }
        }
        return false;
    }

    private void initializeInternal(MinecraftServer server) {
        this.server = server;
        loader.reload();
        stateScanTicks = 0;
        previewSnapshots.clear();
        debugVisualTargets.clear();
        resumeFromSaved(server);
    }

    public void resumeFromSaved(MinecraftServer server) {
        if (server == null || server.overworld() == null) {
            return;
        }
        NpcSavedData data = NpcSavedData.get(server.overworld());
        for (Map.Entry<String, NpcState> entry : data.getNpcStates().entrySet()) {
            String npcId = normalize(entry.getKey());
            NpcState persisted = entry.getValue();
            if (persisted == null || !persisted.replayRunning() || persisted.currentReplayId() == null || persisted.currentReplayId().isBlank()) {
                continue;
            }

            NpcReplayScript script = loader.get(persisted.currentReplayId());
            if (script == null) {
                rememberStop(npcId, persisted.currentReplayId(), persisted.replayFrameIndex(), 0L, ReplayStopReason.SCRIPT_MISSING, 0L, 0L);
                clearPersistedReplayState(npcId);
                continue;
            }
            if (isInvalidResumeState(persisted, script, server.overworld().getGameTime())) {
                rememberStop(npcId, persisted.currentReplayId(), persisted.replayFrameIndex(), 0L, ReplayStopReason.INVALID_STATE, 0L, 0L);
                clearPersistedReplayState(npcId);
                debugLog("NpcReplayEngine: invalid resume state npc='{}' replay='{}' frame={}",
                        npcId, persisted.currentReplayId(), persisted.replayFrameIndex());
                continue;
            }

            Result result = playInternal(npcId, persisted.currentReplayId(), null, persisted.replayFrameIndex(), true, true, false);
            if (!result.success()) {
                Marallyzen.LOGGER.warn(
                        "NpcReplayEngine: failed to resume replay npc='{}' replay='{}' from frame {}: {}",
                        npcId,
                        persisted.currentReplayId(),
                        persisted.replayFrameIndex(),
                        result.message()
                );
                rememberStop(npcId, persisted.currentReplayId(), persisted.replayFrameIndex(), 0L, ReplayStopReason.ERROR, 0L, 0L);
                clearPersistedReplayState(npcId);
                continue;
            }
            debugLog("NpcReplayEngine: resumed replay npc='{}' replay='{}' frame={}",
                    npcId, persisted.currentReplayId(), persisted.replayFrameIndex());
        }
    }

    private boolean isInvalidResumeState(NpcState persisted, NpcReplayScript script, long worldTime) {
        if (persisted == null || script == null) {
            return true;
        }
        int frame = persisted.replayFrameIndex();
        int expanded = Math.max(1, script.expandedLength());
        if (frame < 0 || frame > expanded * 2) {
            return true;
        }
        if (!script.loop() && frame >= expanded) {
            return true;
        }
        if (frame > 0) {
            long drift = Math.abs(worldTime - frame);
            long dynamicLimit = Math.max(RESUME_WORLD_TIME_DRIFT_LIMIT_TICKS, expanded * 40L);
            if (drift > dynamicLimit) {
                return true;
            }
        }
        return false;
    }

    private void tickInternal(MinecraftServer server) {
        if (server == null) {
            return;
        }
        this.server = server;
        for (NpcReplayState state : registry.activeSnapshot()) {
            if (state == null || !state.running()) {
                continue;
            }
            long tickStartNanos = System.nanoTime();
            String npcId = normalize(state.npcId());

            NpcReplayScript script = loader.get(state.replayId());
            if (script == null || script.frames().isEmpty()) {
                stopInternal(npcId, true, ReplayStopReason.SCRIPT_MISSING);
                continue;
            }

            Entity entity = NpcClickHandler.getRegistry().getNpc(npcId);
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                stopInternal(npcId, true, ReplayStopReason.NPC_REMOVED);
                continue;
            }
            if (!(entity.level() instanceof ServerLevel level)) {
                stopInternal(npcId, true, ReplayStopReason.INVALID_STATE);
                continue;
            }

            int clampedIndex = Math.min(Math.max(0, state.frameIndex()), script.frames().size() - 1);
            state.setFrameIndex(clampedIndex);
            NpcReplayFrame currentFrame = script.frames().get(clampedIndex);
            long worldTick = level.getGameTime();
            boolean firstTickOfFrame = state.repeatTick() == 0;

            ReplayStopReason safetyReason = checkSafety(entity, state, currentFrame, worldTick);
            if (safetyReason != null) {
                stopInternal(npcId, true, safetyReason);
                continue;
            }

            NpcReplayFrame frameToApply = resolveFrameForTick(script, state, currentFrame);
            applyFrame(entity, frameToApply, state, script);
            if (firstTickOfFrame) {
                playFrameEmote(entity, currentFrame);
            }
            state.setLastAppliedPosition(frameToApply.x(), frameToApply.y(), frameToApply.z());

            if (state.persistState()) {
                persistReplayState(entity, npcId, state.replayId(), state.frameIndex(), true);
            }

            state.incrementReplayUptimeTicks();
            state.setLastProcessedGameTick(worldTick);
            state.recordTickCostNanos(System.nanoTime() - tickStartNanos);
            emitDebugVisual(level, state, currentFrame, script);

            state.incrementRepeatTick();
            if (state.repeatTick() < Math.max(1, currentFrame.repeat())) {
                continue;
            }

            state.setRepeatTick(0);
            int nextFrame = state.frameIndex() + 1;
            if (nextFrame < script.frames().size()) {
                state.setFrameIndex(nextFrame);
                continue;
            }
            if (script.loop()) {
                state.setFrameIndex(0);
                continue;
            }

            Runnable callback = state.onComplete();
            stopInternal(npcId, true, ReplayStopReason.FINISHED);
            if (callback != null) {
                try {
                    callback.run();
                } catch (Exception e) {
                    Marallyzen.LOGGER.warn("NpcReplayEngine: replay completion callback failed for {}", npcId, e);
                }
            }
        }

        stateScanTicks++;
        if (stateScanTicks >= STATE_SCAN_INTERVAL_TICKS) {
            stateScanTicks = 0;
            resumeFromSaved(server);
        }
    }

    private Result playInternal(
            String npcId,
            String replayId,
            Runnable onComplete,
            int frameIndex,
            boolean resumeMode,
            boolean persistState,
            boolean previewMode
    ) {
        String normalizedNpc = normalize(npcId);
        String normalizedReplay = normalize(replayId);
        if (normalizedNpc.isBlank() || normalizedReplay.isBlank()) {
            return new Result(false, "npcId and replayId are required.");
        }

        NpcReplayScript script = loader.get(normalizedReplay);
        if (script == null) {
            return new Result(false, "Replay not found: " + normalizedReplay);
        }
        Entity entity = NpcClickHandler.getRegistry().getNpc(normalizedNpc);
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return new Result(false, "NPC not found in world: " + normalizedNpc);
        }

        NpcReplayState existing = registry.getActive(normalizedNpc);
        if (existing != null && existing.running()) {
            if (resumeMode) {
                return new Result(true, "Replay already resumed for NPC: " + normalizedNpc);
            }
            stopInternal(normalizedNpc, true, ReplayStopReason.REPLACED);
        }

        if (previewMode) {
            previewSnapshots.put(normalizedNpc, PreviewSnapshot.from(entity));
        } else {
            previewSnapshots.remove(normalizedNpc);
        }
        poseLockedNpcs.remove(normalizedNpc);

        NpcReplayState state = new NpcReplayState(
                normalizedNpc,
                normalizedReplay,
                script.mode(),
                script.loop(),
                script.interpolate(),
                script.nextTriggerId(),
                Math.max(0, frameIndex),
                persistState,
                previewMode
        );
        state.setOnComplete(onComplete);
        state.clearLastAppliedPosition();
        long now = entity.level().getGameTime();
        long startedTick = resumeMode ? Math.max(0L, now - Math.max(0, frameIndex)) : now;
        state.setStartedGameTick(startedTick);
        state.setLastProcessedGameTick(now);
        state.setLastStopReason(ReplayStopReason.NONE);

        registry.putActive(state);
        if (persistState) {
            persistReplayState(entity, normalizedNpc, normalizedReplay, state.frameIndex(), true);
        }
        rememberStop(normalizedNpc, normalizedReplay, state.frameIndex(), 0L, ReplayStopReason.NONE, 0L, 0L);

        if (previewMode) {
            debugLog("NpcReplayEngine: preview started npc='{}' replay='{}'", normalizedNpc, normalizedReplay);
            return new Result(true, "Replay preview started: npc=" + normalizedNpc + ", replay=" + normalizedReplay);
        }

        debugLog("NpcReplayEngine: replay started npc='{}' replay='{}'", normalizedNpc, normalizedReplay);
        return new Result(true, "Replay started: npc=" + normalizedNpc + ", replay=" + normalizedReplay);
    }

    private Result stopInternal(String npcId, boolean clearSavedState, ReplayStopReason reason) {
        String normalizedNpc = normalize(npcId);
        if (normalizedNpc.isBlank()) {
            return new Result(false, "npcId is required.");
        }

        NpcReplayState removed = registry.removeActive(normalizedNpc);
        if (removed == null) {
            if (poseLockedNpcs.remove(normalizedNpc)) {
                Entity entity = NpcClickHandler.getRegistry().getNpc(normalizedNpc);
                if (entity != null) {
                    resetEntityAfterReplay(entity);
                }
                return new Result(true, "Replay pose lock cleared for NPC: " + normalizedNpc);
            }
            return new Result(false, "Replay is not running for NPC: " + normalizedNpc);
        }
        removed.setLastStopReason(reason);
        removed.stop();

        Entity entity = NpcClickHandler.getRegistry().getNpc(normalizedNpc);
        restoreMobilityState(removed, entity);
        boolean keepFinalPose = reason == ReplayStopReason.FINISHED && !removed.preview();
        if (keepFinalPose) {
            poseLockedNpcs.add(normalizedNpc);
        } else {
            poseLockedNpcs.remove(normalizedNpc);
        }
        if (removed.preview()) {
            restorePreviewSnapshot(normalizedNpc, entity);
        } else if (entity != null) {
            if (keepFinalPose) {
                stabilizeEntityAtFinalPose(entity);
            } else {
                resetEntityAfterReplay(entity);
            }
        }

        if (clearSavedState && removed.persistState()) {
            clearPersistedReplayState(normalizedNpc);
        }

        rememberStop(
                normalizedNpc,
                removed.replayId(),
                removed.frameIndex(),
                removed.replayUptimeTicks(),
                reason,
                removed.lastTickCostNanos(),
                removed.averageTickCostNanos()
        );

        if (reason == ReplayStopReason.ERROR || reason == ReplayStopReason.INVALID_STATE) {
            Marallyzen.LOGGER.warn("NpcReplayEngine: replay stopped npc='{}' reason={}", normalizedNpc, reason);
        } else {
            debugLog("NpcReplayEngine: replay stopped npc='{}' reason={}", normalizedNpc, reason);
        }

        String action = switch (reason) {
            case MANUAL -> "forced stop";
            case FINISHED -> "completed";
            case REPLACED -> "replaced";
            default -> "stopped (" + reason.name() + ")";
        };
        return new Result(true, "Replay " + action + " for NPC: " + normalizedNpc);
    }

    private void rememberStop(
            String npcId,
            String replayId,
            int frameIndex,
            long uptimeTicks,
            ReplayStopReason reason,
            long lastTickCostNanos,
            long averageTickCostNanos
    ) {
        if (npcId == null || npcId.isBlank()) {
            return;
        }
        lastSnapshots.put(normalize(npcId), new LastSnapshot(
                replayId == null ? "" : replayId,
                Math.max(0, frameIndex),
                Math.max(0L, uptimeTicks),
                reason == null ? ReplayStopReason.NONE : reason,
                Math.max(0L, lastTickCostNanos),
                Math.max(0L, averageTickCostNanos)
        ));
    }

    private ReplayStopReason checkSafety(Entity entity, NpcReplayState state, NpcReplayFrame frame, long worldTick) {
        if (entity == null || state == null || frame == null) {
            return ReplayStopReason.INVALID_STATE;
        }
        if (!isFinite(entity.getDeltaMovement())) {
            return ReplayStopReason.INVALID_STATE;
        }
        if (!isFinite(frame.x()) || !isFinite(frame.y()) || !isFinite(frame.z())
                || !isFinite(frame.dx()) || !isFinite(frame.dy()) || !isFinite(frame.dz())) {
            return ReplayStopReason.INVALID_STATE;
        }
        if (state.lastProcessedGameTick() >= 0L && worldTick - state.lastProcessedGameTick() > WATCHDOG_FREEZE_GAP_TICKS) {
            return ReplayStopReason.INVALID_STATE;
        }
        if (state.hasLastAppliedPosition()) {
            double allowed = state.mode() == NpcReplayScript.ReplayMode.EXACT ? EXACT_EXPECTED_DISTANCE_SQ : DESYNC_STOP_DISTANCE_SQ;
            double distSq = entity.distanceToSqr(state.lastAppliedX(), state.lastAppliedY(), state.lastAppliedZ());
            if (distSq > allowed) {
                debugLog("NpcReplayEngine: watchdog desync npc='{}' distSq={}", state.npcId(), distSq);
                return ReplayStopReason.DESYNC;
            }
        }
        if (isInsideCollision(entity)) {
            state.incrementStuckTicks();
            if (state.stuckTicks() >= MAX_STUCK_TICKS) {
                return ReplayStopReason.COLLISION_STUCK;
            }
        } else {
            state.resetStuckTicks();
        }
        return null;
    }

    private boolean isInsideCollision(Entity entity) {
        if (entity == null || entity.level() == null) {
            return false;
        }
        try {
            return !entity.level().noCollision(entity, entity.getBoundingBox().deflate(0.01D));
        } catch (Exception ignored) {
            return false;
        }
    }

    private NpcReplayFrame resolveFrameForTick(NpcReplayScript script, NpcReplayState state, NpcReplayFrame current) {
        if (script == null || state == null || current == null) {
            return current;
        }
        if (!state.interpolate()) {
            return current;
        }
        if (state.frameIndex() >= script.frames().size() - 1) {
            return current;
        }
        int repeat = Math.max(1, current.repeat());
        if (repeat <= 1 && !script.loop()) {
            return current;
        }

        int nextIndex = state.frameIndex() + 1;
        if (nextIndex >= script.frames().size()) {
            nextIndex = script.loop() ? 0 : script.frames().size() - 1;
        }
        NpcReplayFrame next = script.frames().get(nextIndex);
        double alpha = state.repeatTick() / (double) repeat;
        return NpcReplayFrame.interpolate(current, next, alpha);
    }

    private void applyFrame(Entity entity, NpcReplayFrame frame, NpcReplayState state, NpcReplayScript script) {
        if (entity == null || frame == null || state == null || script == null) {
            return;
        }

        if (entity instanceof Mob mob) {
            state.captureOriginalNoAi(mob.isNoAi());
            if (mob.isNoAi()) {
                mob.setNoAi(false);
            }
        }

        if (state.mode() == NpcReplayScript.ReplayMode.EXACT) {
            entity.setPos(frame.x(), frame.y(), frame.z());
            entity.setDeltaMovement(frame.dx(), frame.dy(), frame.dz());
            entity.setNoGravity(frame.flying() || frame.fallFlying());
        } else {
            boolean kinematicFallback = entity instanceof ServerPlayer;
            if (state.frameIndex() == 0 && state.repeatTick() == 0) {
                entity.setPos(frame.x(), frame.y(), frame.z());
            } else {
                double distSq = entity.distanceToSqr(frame.x(), frame.y(), frame.z());
                if (distSq > PHYSICS_HARD_RESYNC_DISTANCE_SQ) {
                    entity.setPos(frame.x(), frame.y(), frame.z());
                    debugLog("NpcReplayEngine: physics hard-resync npc='{}' distSq={}", state.npcId(), distSq);
                }
            }
            Vec3 desiredVelocity = computePhysicsVelocity(entity, frame, state, script);
            if (kinematicFallback) {
                // FakePlayer-based NPCs often ignore server-side horizontal movement.
                // Keep horizontal track, but do not pin vertical position while airborne,
                // otherwise jump+crouch segments can look like temporary hovering.
                if (frame.onGround()) {
                    entity.setPos(frame.x(), frame.y(), frame.z());
                } else {
                    entity.setPos(frame.x(), entity.getY(), frame.z());
                }
                entity.setDeltaMovement(desiredVelocity);
            } else {
                entity.setDeltaMovement(desiredVelocity);
                // If physics is blocked externally, use a soft catch-up instead of freezing in place.
                double lagSq = entity.distanceToSqr(frame.x(), frame.y(), frame.z());
                if (lagSq > 9.0D) {
                    entity.setPos(frame.x(), frame.y(), frame.z());
                }
            }
            entity.setNoGravity(entity instanceof ServerPlayer && (frame.flying() || frame.fallFlying()));
        }

        entity.setYRot(frame.yaw());
        entity.setXRot(frame.pitch());
        entity.yRotO = frame.yaw();
        entity.xRotO = frame.pitch();
        entity.setSprinting(frame.sprint());
        entity.setShiftKeyDown(frame.crouch());
        entity.setSwimming(frame.swimming());
        entity.fallDistance = frame.fallDistance();
        state.setLastCrouching(frame.crouch());

        if (entity instanceof ServerPlayer player) {
            player.getAbilities().flying = frame.flying();
            // Force crouch pose only when grounded; in-air crouch pose pinning can look like hovering.
            boolean crouchPose = frame.crouch() && frame.onGround() && !frame.swimming() && !frame.fallFlying();
            player.setPose(crouchPose ? Pose.CROUCHING : Pose.STANDING);
            player.onUpdateAbilities();
        }

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = frame.bodyYaw();
            living.yBodyRotO = frame.bodyYaw();
            living.yHeadRot = frame.headYaw();
            living.yHeadRotO = frame.headYaw();

            AttributeInstance movement = living.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null && frame.movementSpeed() > 0.0F) {
                movement.setBaseValue(frame.movementSpeed());
            }

            applyMainHandItem(living, frame, state);
            applyOffHandItem(living, frame, state);
            applyUsingItemState(living, frame, state);

            float previousSwing = state.lastSwingProgress();
            boolean swingByProgress = frame.swingProgress() > previousSwing + 0.35F || (frame.swingProgress() > 0.9F && previousSwing < 0.1F);
            if (frame.attack() || swingByProgress) {
                living.swing(InteractionHand.MAIN_HAND);
                if (living instanceof ServerPlayer fakePlayer && fakePlayer.level() instanceof ServerLevel level) {
                    broadcastFakePlayerSwing(level, fakePlayer);
                }
            }
            state.setLastSwingProgress(frame.swingProgress());
        }

        if (entity instanceof GeckoNpcEntity geckoNpc) {
            if (frame.sprint()) {
                geckoNpc.setExpression("walk");
            } else {
                geckoNpc.setExpression("idle");
            }
        }
    }

    private Vec3 computePhysicsVelocity(Entity entity, NpcReplayFrame frame, NpcReplayState state, NpcReplayScript script) {
        if (entity == null || frame == null || state == null || script == null) {
            return Vec3.ZERO;
        }
        Vec3 recorded = new Vec3(frame.dx(), frame.dy(), frame.dz());
        Vec3 desired = recorded;
        int nextIndex = Math.min(state.frameIndex() + 1, script.frames().size() - 1);
        if (nextIndex > state.frameIndex()) {
            NpcReplayFrame next = script.frames().get(nextIndex);
            Vec3 positionalStep = new Vec3(
                    next.x() - frame.x(),
                    next.y() - frame.y(),
                    next.z() - frame.z()
            );
            // Blend recorded velocity with positional delta to avoid mob-physics divergence and teleport jitter.
            desired = recorded.scale(0.35D).add(positionalStep.scale(0.65D));
        }

        if (frame.onGround() && !frame.jumping()) {
            double vy = desired.y;
            if (Math.abs(vy) < 0.15D) {
                vy = 0.0D;
            }
            desired = new Vec3(desired.x, vy, desired.z);
        } else if (frame.jumping() && entity.onGround()) {
            desired = new Vec3(desired.x, Math.max(desired.y, 0.42D), desired.z);
        }

        if (!isFinite(desired)) {
            return Vec3.ZERO;
        }
        return desired;
    }

    private void playFrameEmote(Entity entity, NpcReplayFrame frame) {
        if (entity == null || frame == null) {
            return;
        }
        String emoteId = frame.emoteId();
        if (emoteId == null || emoteId.isBlank()) {
            return;
        }
        NpcDefaultAnimationHandler.markNpcPlayingOtherAnimation(entity);
        NpcAnimationHandler.sendAnimationToNearbyPlayers(entity, emoteId, 32);
    }

    private void applyMainHandItem(LivingEntity living, NpcReplayFrame frame, NpcReplayState state) {
        if (living == null || frame == null || state == null) {
            return;
        }
        String itemId = frame.mainHandItemId() == null ? "" : frame.mainHandItemId();
        int count = Math.max(0, frame.mainHandCount());
        if (state.isSameMainHand(itemId, count)) {
            return;
        }
        if (itemId.isBlank() || count <= 0) {
            living.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            state.setLastMainHand("", 0);
            if (living instanceof ServerPlayer fakePlayer && fakePlayer.level() instanceof ServerLevel level) {
                syncFakePlayerEquipment(level, fakePlayer, EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            return;
        }
        var item = BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.tryParse(itemId)).orElse(null);
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item, count);
        living.setItemSlot(EquipmentSlot.MAINHAND, stack);
        state.setLastMainHand(itemId, count);
        if (living instanceof ServerPlayer fakePlayer && fakePlayer.level() instanceof ServerLevel level) {
            syncFakePlayerEquipment(level, fakePlayer, EquipmentSlot.MAINHAND, stack);
        }
    }

    private void applyOffHandItem(LivingEntity living, NpcReplayFrame frame, NpcReplayState state) {
        if (living == null || frame == null || state == null) {
            return;
        }
        String itemId = frame.offHandItemId() == null ? "" : frame.offHandItemId();
        int count = Math.max(0, frame.offHandCount());
        if (state.isSameOffHand(itemId, count)) {
            return;
        }
        if (itemId.isBlank() || count <= 0) {
            living.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            state.setLastOffHand("", 0);
            if (living instanceof ServerPlayer fakePlayer && fakePlayer.level() instanceof ServerLevel level) {
                syncFakePlayerEquipment(level, fakePlayer, EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            return;
        }
        var item = BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.tryParse(itemId)).orElse(null);
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item, count);
        living.setItemSlot(EquipmentSlot.OFFHAND, stack);
        state.setLastOffHand(itemId, count);
        if (living instanceof ServerPlayer fakePlayer && fakePlayer.level() instanceof ServerLevel level) {
            syncFakePlayerEquipment(level, fakePlayer, EquipmentSlot.OFFHAND, stack);
        }
    }

    private void applyUsingItemState(LivingEntity living, NpcReplayFrame frame, NpcReplayState state) {
        if (living == null || frame == null || state == null) {
            return;
        }
        boolean using = frame.usingItem();
        if (state.isSameUsingItem(using)) {
            return;
        }
        if (using) {
            living.startUsingItem(InteractionHand.MAIN_HAND);
        } else {
            living.stopUsingItem();
        }
        state.setLastUsingItem(using);
    }

    private void syncFakePlayerEquipment(ServerLevel level, ServerPlayer fakePlayer, EquipmentSlot slot, ItemStack stack) {
        if (level == null || fakePlayer == null || slot == null || stack == null) {
            return;
        }
        var equipment = java.util.List.of(Pair.of(slot, stack.copy()));
        ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(fakePlayer.getId(), equipment);
        for (ServerPlayer viewer : level.players()) {
            if (viewer == fakePlayer || viewer.connection == null) {
                continue;
            }
            if (viewer.distanceToSqr(fakePlayer) > 64.0D * 64.0D) {
                continue;
            }
            viewer.connection.send(packet);
        }
    }

    private void broadcastFakePlayerSwing(ServerLevel level, ServerPlayer fakePlayer) {
        if (level == null || fakePlayer == null) {
            return;
        }
        ClientboundAnimatePacket packet = new ClientboundAnimatePacket(fakePlayer, 0);
        for (ServerPlayer viewer : level.players()) {
            if (viewer == fakePlayer || viewer.connection == null) {
                continue;
            }
            if (viewer.distanceToSqr(fakePlayer) > 64.0D * 64.0D) {
                continue;
            }
            viewer.connection.send(packet);
        }
    }

    private void emitDebugVisual(ServerLevel level, NpcReplayState state, NpcReplayFrame currentFrame, NpcReplayScript script) {
        if (level == null || state == null || currentFrame == null || script == null || server == null) {
            return;
        }
        if (debugVisualTargets.isEmpty()) {
            return;
        }
        String npcId = state.npcId();
        int nextIndex = Math.min(state.frameIndex() + 1, script.frames().size() - 1);
        NpcReplayFrame next = script.frames().isEmpty() ? null : script.frames().get(nextIndex);

        for (Map.Entry<UUID, String> entry : debugVisualTargets.entrySet()) {
            if (!npcId.equals(entry.getValue())) {
                continue;
            }
            ServerPlayer viewer = server.getPlayerList().getPlayer(entry.getKey());
            if (viewer == null || !viewer.isAlive()) {
                debugVisualTargets.remove(entry.getKey());
                continue;
            }
            if (!isOperator(viewer)) {
                debugVisualTargets.remove(entry.getKey());
                continue;
            }
            if (viewer.level() != level) {
                continue;
            }

            level.sendParticles(viewer, ParticleTypes.END_ROD, true, false,
                    currentFrame.x(), currentFrame.y() + 0.05D, currentFrame.z(), 2, 0.02D, 0.02D, 0.02D, 0.0D);

            if (next != null) {
                level.sendParticles(viewer, ParticleTypes.FLAME, true, false,
                        next.x(), next.y() + 0.05D, next.z(), 2, 0.02D, 0.02D, 0.02D, 0.0D);
                int points = 8;
                for (int i = 0; i <= points; i++) {
                    double t = i / (double) points;
                    double px = currentFrame.x() + (next.x() - currentFrame.x()) * t;
                    double py = currentFrame.y() + (next.y() - currentFrame.y()) * t + 0.03D;
                    double pz = currentFrame.z() + (next.z() - currentFrame.z()) * t;
                    level.sendParticles(viewer, ParticleTypes.CRIT, true, false, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }

            if ((state.replayUptimeTicks() % 10L) == 0L) {
                viewer.displayClientMessage(Component.literal(
                        "Replay[" + npcId + "] frame " + state.frameIndex() + "/" + Math.max(1, script.frames().size() - 1)
                ), true);
            }
        }
    }

    private Result toggleDebugVisualInternal(ServerPlayer player, String npcId) {
        if (player == null) {
            return new Result(false, "This command must be run by a player.");
        }
        if (!isOperator(player)) {
            return new Result(false, "Operator permissions are required.");
        }
        String normalizedNpc = normalize(npcId);
        if (normalizedNpc.isBlank()) {
            return new Result(false, "npcId is required.");
        }
        String existing = debugVisualTargets.get(player.getUUID());
        if (normalizedNpc.equals(existing)) {
            debugVisualTargets.remove(player.getUUID());
            return new Result(true, "Replay debug visualization disabled for npc=" + normalizedNpc);
        }
        debugVisualTargets.put(player.getUUID(), normalizedNpc);
        return new Result(true, "Replay debug visualization enabled for npc=" + normalizedNpc);
    }

    private void restorePreviewSnapshot(String npcId, Entity entity) {
        PreviewSnapshot snapshot = previewSnapshots.remove(npcId);
        if (snapshot == null || entity == null || entity.isRemoved()) {
            return;
        }

        entity.setPos(snapshot.x(), snapshot.y(), snapshot.z());
        entity.setDeltaMovement(snapshot.dx(), snapshot.dy(), snapshot.dz());
        entity.setYRot(snapshot.yaw());
        entity.setXRot(snapshot.pitch());
        entity.yRotO = snapshot.yaw();
        entity.xRotO = snapshot.pitch();
        entity.setSprinting(snapshot.sprinting());
        entity.setShiftKeyDown(snapshot.crouching());
        entity.setNoGravity(snapshot.noGravity());
        entity.setSwimming(snapshot.swimming());
        entity.fallDistance = snapshot.fallDistance();

        if (entity instanceof ServerPlayer player) {
            player.getAbilities().flying = snapshot.playerFlying();
            player.onUpdateAbilities();
        }

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = snapshot.bodyYaw();
            living.yBodyRotO = snapshot.bodyYaw();
            living.yHeadRot = snapshot.headYaw();
            living.yHeadRotO = snapshot.headYaw();
            AttributeInstance movement = living.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null && snapshot.movementSpeed() > 0.0F) {
                movement.setBaseValue(snapshot.movementSpeed());
            }
        }

        debugLog("NpcReplayEngine: preview restored npc='{}'", npcId);
    }

    private void resetEntityAfterReplay(Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return;
        }
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        entity.setSprinting(false);
        entity.setShiftKeyDown(false);
        entity.setSwimming(false);
        entity.setNoGravity(false);
        entity.fallDistance = 0.0F;

        if (entity instanceof ServerPlayer player) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        if (entity instanceof GeckoNpcEntity geckoNpc) {
            geckoNpc.setExpression("idle");
        }
    }

    private void stabilizeEntityAtFinalPose(Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return;
        }
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        entity.setNoGravity(false);
        entity.fallDistance = 0.0F;

        if (entity instanceof ServerPlayer player) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private void restoreMobilityState(NpcReplayState state, Entity entity) {
        if (state == null || entity == null || entity.isRemoved()) {
            return;
        }
        if (entity instanceof Mob mob && state.originalNoAiCaptured()) {
            mob.setNoAi(state.originalNoAi());
        }
    }

    private void persistReplayState(Entity entity, String npcId, String replayId, int frameIndex, boolean running) {
        if (!(entity instanceof LivingEntity) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!NpcWorldPolicy.isPersistentLevel(level)) {
            return;
        }
        NpcSavedData data = NpcSavedData.get(level);
        NpcState existing = data.getState(npcId);
        String appearance = existing != null ? existing.appearanceId() : npcId;
        String ai = existing != null ? existing.aiState() : null;
        String dialog = existing != null ? existing.dialogState() : null;
        String currentReplay = (replayId == null || replayId.isBlank()) ? null : replayId;
        int safeFrame = Math.max(0, frameIndex);
        BlockPos pos = entity.blockPosition();
        data.putState(npcId, new NpcState(
                level.dimension(),
                pos,
                entity.getYRot(),
                appearance,
                ai,
                dialog,
                currentReplay,
                safeFrame,
                running
        ));
    }

    private void clearPersistedReplayState(String npcId) {
        if (server == null || server.overworld() == null) {
            return;
        }
        NpcSavedData data = NpcSavedData.get(server.overworld());
        NpcState existing = data.getState(npcId);
        if (existing == null) {
            return;
        }
        data.putState(npcId, new NpcState(
                existing.dimension(),
                existing.pos(),
                existing.yaw(),
                existing.appearanceId(),
                existing.aiState(),
                existing.dialogState(),
                null,
                0,
                false
        ));
    }

    private Status buildStatus(String npcId) {
        String normalizedNpc = normalize(npcId);
        if (normalizedNpc.isBlank()) {
            return new Status(false, "", "", 0, 0, false, "UNKNOWN", false, ReplayStopReason.NONE,
                    0L, 0.0D, null, null, null, null, null, null, 0L, 0L, 0L);
        }

        NpcReplayState running = registry.getActive(normalizedNpc);
        LastSnapshot previous = lastSnapshots.get(normalizedNpc);

        String replayId = running != null ? running.replayId() : (previous != null ? previous.replayId() : "");
        NpcReplayScript script = replayId == null || replayId.isBlank() ? null : loader.get(replayId);
        int totalFrames = script != null ? script.frames().size() : 0;
        boolean loop = running != null ? running.loop() : (script != null && script.loop());
        String mode = running != null
                ? running.mode().name()
                : (script != null ? script.mode().name() : "UNKNOWN");
        boolean interpolate = running != null
                ? running.interpolate()
                : (script != null && script.interpolate());

        int frameIndex = running != null
                ? running.frameIndex()
                : (previous != null ? previous.frameIndex() : 0);
        ReplayStopReason stopReason = running != null
                ? running.lastStopReason()
                : (previous != null ? previous.stopReason() : ReplayStopReason.NONE);
        long uptimeTicks = running != null
                ? running.replayUptimeTicks()
                : (previous != null ? previous.uptimeTicks() : 0L);
        long lastTickCost = running != null
                ? running.lastTickCostNanos()
                : (previous != null ? previous.lastTickCostNanos() : 0L);
        long avgTickCost = running != null
                ? running.averageTickCostNanos()
                : (previous != null ? previous.averageTickCostNanos() : 0L);

        Entity entity = NpcClickHandler.getRegistry().getNpc(normalizedNpc);
        Double npcX = entity != null ? entity.getX() : null;
        Double npcY = entity != null ? entity.getY() : null;
        Double npcZ = entity != null ? entity.getZ() : null;

        Double startX = null;
        Double startY = null;
        Double startZ = null;
        if (script != null && !script.frames().isEmpty()) {
            NpcReplayFrame first = script.frames().get(0);
            startX = first.x();
            startY = first.y();
            startZ = first.z();
        }

        return new Status(
                running != null && running.running(),
                normalizedNpc,
                replayId == null ? "" : replayId,
                Math.max(0, frameIndex),
                Math.max(0, totalFrames),
                loop,
                mode,
                interpolate,
                stopReason,
                Math.max(0L, uptimeTicks),
                Math.max(0L, uptimeTicks) / 20.0D,
                npcX, npcY, npcZ,
                startX, startY, startZ,
                Math.max(0L, lastTickCost),
                Math.max(0L, avgTickCost),
                estimateScriptMemoryBytes(script)
        );
    }

    private long estimateScriptMemoryBytes(NpcReplayScript script) {
        if (script == null) {
            return 0L;
        }
        try {
            return script.toJson().toString().getBytes(StandardCharsets.UTF_8).length;
        } catch (Exception ignored) {
            return Math.max(0L, script.frames().size() * 176L);
        }
    }

    private boolean isFinite(Vec3 vec) {
        if (vec == null) {
            return false;
        }
        return isFinite(vec.x) && isFinite(vec.y) && isFinite(vec.z);
    }

    private boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    private boolean isOperator(ServerPlayer player) {
        return PermissionHelper.isOp(player);
    }

    private void debugLog(String message, Object... args) {
        if (!debugEnabled) {
            return;
        }
        Marallyzen.LOGGER.info(message, args);
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
