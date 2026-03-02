package neutka.marallys.marallyzen.trigger.engine;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.denizen.commands.CommandScriptRegistry;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.TriggerAnimationStartPacket;
import neutka.marallys.marallyzen.network.TriggerAnimationStopPacket;
import neutka.marallys.marallyzen.npc.replay.NpcReplayEngine;
import neutka.marallys.marallyzen.door.DoorEngine;
import neutka.marallys.marallyzen.trigger.TriggerRegistry;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprint;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlueprintLoader;
import neutka.marallys.marallyzen.trigger.blueprint.TriggerBlockStateCodec;
import neutka.marallys.marallyzen.trigger.instance.TriggerInstance;
import neutka.marallys.marallyzen.trigger.instance.TriggerInstanceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TriggerEngine {
    private static final String KEY_WAIT_REPLAY_CHAIN_UNLOCK = "chain_wait_replay_unlock";

    private final TriggerBlueprintLoader blueprintLoader;
    private final TriggerInstanceManager instanceManager;
    private final TriggerRegistry triggerRegistry;
    private final TriggerZoneManager zoneManager;
    private final TriggerAnimationController animationController = new TriggerAnimationController();

    public TriggerEngine(
            TriggerBlueprintLoader blueprintLoader,
            TriggerInstanceManager instanceManager,
            TriggerRegistry triggerRegistry,
            TriggerZoneManager zoneManager
    ) {
        this.blueprintLoader = blueprintLoader;
        this.instanceManager = instanceManager;
        this.triggerRegistry = triggerRegistry;
        this.zoneManager = zoneManager;
    }

    public TriggerZoneManager zoneManager() {
        return zoneManager;
    }

    public void rebuildZones() {
        zoneManager.clear();
        for (TriggerInstance instance : instanceManager.allInstances()) {
            zoneManager.registerZone(instance);
        }
    }

    public void onServerTick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (TriggerInstance instance : instanceManager.allInstances()) {
            ServerLevel level = resolveLevel(server, instance.dimensionId());
            if (level == null) {
                continue;
            }

            boolean dirty = false;
            if (instance.cooldownRemaining() > 0) {
                instance.setCooldownRemaining(instance.cooldownRemaining() - 1);
                dirty = true;
            }

            if (instance.structureState() == TriggerInstance.StructureState.ANIMATING) {
                dirty |= tickRunningAnimation(level, instance);
            } else if (instance.structureState() == TriggerInstance.StructureState.HIDDEN) {
                dirty |= checkPassiveTriggers(level, instance);
            }

            if (dirty) {
                instanceManager.persistInstance(instance);
            }
        }
    }

    public void onPlayerTick(ServerPlayer player) {
        if (player == null) {
            return;
        }
        String dimensionId = player.level().dimension().identifier().toString();
        for (TriggerInstance instance : instanceManager.getByDimension(dimensionId)) {
            if (!"zone_enter".equals(instance.triggerType()) || instance.structureState() != TriggerInstance.StructureState.HIDDEN) {
                continue;
            }
            boolean inside = isInsideZone(player, instance.bindPos(), instance.zoneRadius());
            boolean wasInside = instance.playersInside().contains(player.getUUID());
            if (inside && !wasInside) {
                instance.playersInside().add(player.getUUID());
                ServerLevel playerLevel = player.level() instanceof ServerLevel sl ? sl : null;
                tryActivate(instance, player, playerLevel, false);
            } else if (!inside && wasInside) {
                instance.playersInside().remove(player.getUUID());
            }
        }
    }

    public boolean onBlockUse(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return false;
        }
        String dimensionId = player.level().dimension().identifier().toString();
        List<TriggerInstance> list = instanceManager.getByBind(dimensionId, pos);
        if (list.isEmpty()) {
            return false;
        }
        boolean consumed = false;
        for (TriggerInstance instance : list) {
            if (!"block_use".equals(instance.triggerType()) || instance.structureState() != TriggerInstance.StructureState.HIDDEN) {
                continue;
            }
            ServerLevel playerLevel = player.level() instanceof ServerLevel sl ? sl : null;
            consumed |= tryActivate(instance, player, playerLevel, false);
        }
        return consumed;
    }

    public boolean activateManual(String instanceId, ServerPlayer player) {
        TriggerInstance instance = instanceManager.get(instanceId);
        if (instance == null || player == null || instance.structureState() != TriggerInstance.StructureState.HIDDEN) {
            return false;
        }
        ServerLevel level = resolveLevel(player.level().getServer(), instance.dimensionId());
        if (level == null) {
            return false;
        }
        return tryActivate(instance, player, level, false);
    }

    public void clearPlacedStructure(TriggerInstance instance, ServerLevel level) {
        if (instance == null || level == null) {
            return;
        }
        TriggerBlueprint blueprint = blueprintLoader.getBlueprint(instance.blueprintId());
        if (blueprint == null) {
            return;
        }
        for (TriggerBlueprint.BlockEntry entry : blueprint.blocks()) {
            BlockPos target = instance.worldPos().offset(entry.localPos());
            level.removeBlockEntity(target);
            level.setBlock(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        NetworkHelper.sendToAll(new TriggerAnimationStopPacket(instance.id()));
    }

    private boolean checkPassiveTriggers(ServerLevel level, TriggerInstance instance) {
        String type = instance.triggerType();
        if ("timer".equals(type)) {
            long now = level.getGameTime();
            if (instance.nextTimerGameTime() <= 0L) {
                instance.setNextTimerGameTime(now + Math.max(1L, instance.timerIntervalTicks()));
                return true;
            }
            if (now >= instance.nextTimerGameTime()) {
                instance.setNextTimerGameTime(now + Math.max(1L, instance.timerIntervalTicks()));
                return tryActivate(instance, null, level, false) || true;
            }
            return false;
        }
        if ("redstone_power".equals(type)) {
            boolean powered = level.hasNeighborSignal(instance.bindPos());
            boolean risingEdge = powered && !instance.lastPowered();
            instance.setLastPowered(powered);
            if (risingEdge) {
                return tryActivate(instance, null, level, false);
            }
            return false;
        }
        if ("pressure_plate".equals(type)) {
            List<ServerPlayer> players = level.getEntitiesOfClass(
                    ServerPlayer.class,
                    new AABB(instance.bindPos()).inflate(0.05D, 0.6D, 0.05D),
                    ServerPlayer::isAlive
            );
            if (!players.isEmpty()) {
                return tryActivate(instance, players.get(0), level, false);
            }
            return false;
        }
        return false;
    }

    private boolean tickRunningAnimation(ServerLevel level, TriggerInstance instance) {
        TriggerBlueprint blueprint = blueprintLoader.getBlueprint(instance.blueprintId());
        if (blueprint == null) {
            instance.setStructureState(TriggerInstance.StructureState.HIDDEN);
            instance.setAnimationTick(0);
            return true;
        }
        boolean finished = animationController.tick(level, instance, blueprint);
        if (!finished) {
            return true;
        }

        placeStructure(level, instance, blueprint);
        instance.setStructureState(TriggerInstance.StructureState.ACTIVE);
        instance.setAnimationTick(0);
        instance.setCooldownRemaining(instance.cooldownTicks());
        NetworkHelper.sendToAll(new TriggerAnimationStopPacket(instance.id()));
        if (!isWaitingReplayChainUnlock(instance)) {
            unlockNextInChain(instance);
        }
        instanceManager.refreshActiveFile(instance);
        return true;
    }

    private boolean tryActivate(TriggerInstance instance, ServerPlayer player, ServerLevel level, boolean bypassTriggerCheck) {
        if (instance == null || level == null) {
            return false;
        }
        if (instance.structureState() != TriggerInstance.StructureState.HIDDEN) {
            return false;
        }
        if (instance.cooldownRemaining() > 0) {
            return false;
        }
        if (instanceManager.isChainLocked(instance)) {
            return false;
        }
        TriggerBlueprint blueprint = blueprintLoader.getBlueprint(instance.blueprintId());
        if (blueprint == null) {
            return false;
        }
        if (!passesCondition(level, blueprint.settings().condition())) {
            return false;
        }
        if (!bypassTriggerCheck && !triggerRegistry.shouldActivate(instance.triggerType(), player, instance.bindPos(), level)) {
            return false;
        }

        instance.setStructureState(TriggerInstance.StructureState.ANIMATING);
        instance.setAnimationTick(0);
        executeAction(level, player, instance, blueprint.settings().action());
        sendAnimationStart(instance, level, blueprint);
        animationController.onAnimationStart(level, instance, blueprint);
        instanceManager.persistInstance(instance);
        instanceManager.refreshActiveFile(instance);
        return true;
    }

    private void executeAction(
            ServerLevel level,
            ServerPlayer player,
            TriggerInstance instance,
            TriggerBlueprint.Settings.ActionSettings action
    ) {
        if (level == null || action == null || action.type() == null || action.type().isBlank()) {
            return;
        }
        String type = action.type().trim().toLowerCase(Locale.ROOT);
        if ("npc_replay".equals(type)) {
            executeNpcReplayAction(level, instance, action);
            return;
        }
        if ("door".equals(type)) {
            executeDoorAction(level, action);
            return;
        }
        if ("npc_scene".equals(type) || "scene".equals(type)) {
            executeNpcSceneAction(level, player, action);
        }
    }

    private void executeDoorAction(ServerLevel level, TriggerBlueprint.Settings.ActionSettings action) {
        String doorId = action.doorId();
        if (doorId == null || doorId.isBlank()) {
            Marallyzen.LOGGER.warn("TriggerEngine: door action requires door_id field");
            return;
        }
        boolean ok = DoorEngine.getInstance().activate(level.getServer(), doorId);
        if (!ok) {
            Marallyzen.LOGGER.warn("TriggerEngine: failed to activate door '{}'", doorId);
        }
    }

    private void executeNpcReplayAction(ServerLevel level, TriggerInstance sourceInstance, TriggerBlueprint.Settings.ActionSettings action) {
        if (action.npc() == null || action.npc().isBlank() || action.replay() == null || action.replay().isBlank()) {
            Marallyzen.LOGGER.warn("TriggerEngine: npc_replay action requires both npc and replay fields");
            return;
        }
        if (NpcReplayEngine.isDebugEnabled()) {
            Marallyzen.LOGGER.info(
                    "TriggerEngine: trigger-start npc_replay npc='{}' replay='{}'",
                    action.npc(),
                    action.replay()
            );
        }
        Runnable onComplete = null;
        String sourceInstanceId = sourceInstance == null ? "" : sourceInstance.id();
        String nextActionTriggerId = action.nextTrigger() == null ? "" : action.nextTrigger().trim().toLowerCase(Locale.ROOT);
        if (!sourceInstanceId.isBlank() || !nextActionTriggerId.isBlank()) {
            onComplete = () -> {
                if (!sourceInstanceId.isBlank()) {
                    TriggerInstance current = instanceManager.get(sourceInstanceId);
                    if (current != null) {
                        current.persistentData().remove(KEY_WAIT_REPLAY_CHAIN_UNLOCK);
                        unlockNextInChain(current);
                        instanceManager.persistInstance(current);
                        instanceManager.refreshActiveFile(current);
                    }
                }
                if (!nextActionTriggerId.isBlank()) {
                    activateChained(nextActionTriggerId, level);
                }
            };
        }
        NpcReplayEngine.Result result = NpcReplayEngine.play(action.npc(), action.replay(), onComplete);
        if (!result.success()) {
            Marallyzen.LOGGER.warn("TriggerEngine: failed to execute npc_replay action npc='{}' replay='{}': {}",
                    action.npc(), action.replay(), result.message());
            return;
        }
        if (sourceInstance != null) {
            sourceInstance.persistentData().putBoolean(KEY_WAIT_REPLAY_CHAIN_UNLOCK, true);
            instanceManager.persistInstance(sourceInstance);
            instanceManager.refreshActiveFile(sourceInstance);
        }
    }

    private void executeNpcSceneAction(ServerLevel level, ServerPlayer player, TriggerBlueprint.Settings.ActionSettings action) {
        String sceneId = firstNonBlank(action.scene(), action.replay());
        if (sceneId == null || sceneId.isBlank()) {
            Marallyzen.LOGGER.warn("TriggerEngine: npc_scene action requires scene field");
            return;
        }
        if (player == null) {
            Marallyzen.LOGGER.warn("TriggerEngine: npc_scene '{}' requires player activator (block_use/zone_enter/pressure_plate)", sceneId);
            return;
        }
        boolean exists = CommandScriptRegistry.hasCommandScript(sceneId);
        if (!exists) {
            Marallyzen.LOGGER.warn("TriggerEngine: npc_scene command script not found: {}", sceneId);
            return;
        }
        boolean started = CommandScriptRegistry.executeCommandScript(sceneId, player, action.sceneArgs());
        if (!started) {
            Marallyzen.LOGGER.warn("TriggerEngine: failed to execute npc_scene '{}' for player '{}'", sceneId, player.getName().getString());
            return;
        }
        if (action.nextTrigger() != null && !action.nextTrigger().isBlank()) {
            String nextId = action.nextTrigger().trim().toLowerCase(Locale.ROOT);
            activateChained(nextId, level);
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return "";
    }

    private boolean activateChained(String instanceId, ServerLevel currentLevel) {
        if (instanceId == null || instanceId.isBlank()) {
            return false;
        }
        TriggerInstance next = instanceManager.get(instanceId);
        if (next == null) {
            Marallyzen.LOGGER.warn("TriggerEngine: chained instance not found: {}", instanceId);
            return false;
        }
        ServerLevel nextLevel = currentLevel;
        if (!next.dimensionId().equals(currentLevel.dimension().identifier().toString())) {
            nextLevel = resolveLevel(currentLevel.getServer(), next.dimensionId());
        }
        return tryActivate(next, null, nextLevel, true);
    }

    private boolean isWaitingReplayChainUnlock(TriggerInstance instance) {
        if (instance == null) {
            return false;
        }
        return instance.persistentData().getBoolean(KEY_WAIT_REPLAY_CHAIN_UNLOCK).orElse(false);
    }

    private void unlockNextInChain(TriggerInstance instance) {
        if (instance == null) {
            return;
        }
        String nextId = instanceManager.getChainNextInstanceId(instance);
        if (nextId == null || nextId.isBlank()) {
            return;
        }
        TriggerInstance next = instanceManager.get(nextId);
        if (next == null) {
            Marallyzen.LOGGER.warn("TriggerEngine: next chain instance not found: {}", nextId);
            return;
        }
        next.playersInside().clear();
        if (instanceManager.isChainLocked(next)) {
            instanceManager.setChainLocked(next, false);
        }
        instanceManager.persistInstance(next);
        instanceManager.refreshActiveFile(next);
    }

    private void sendAnimationStart(TriggerInstance instance, ServerLevel level, TriggerBlueprint blueprint) {
        List<TriggerAnimationStartPacket.BlockVisual> visuals = new ArrayList<>(blueprint.blocks().size());
        for (TriggerBlueprint.BlockEntry entry : blueprint.blocks()) {
            visuals.add(new TriggerAnimationStartPacket.BlockVisual(entry.localPos(), entry.state()));
        }
        TriggerBlueprint.Settings.AnimationSettings animation = blueprint.settings().animation();
        NetworkHelper.sendToAll(new TriggerAnimationStartPacket(
                instance.id(),
                level.dimension().identifier().toString(),
                instance.worldPos(),
                animation.spawnOffset(),
                Math.max(1, animation.duration()),
                animation.type(),
                animation.easing(),
                visuals
        ));
    }

    private void placeStructure(ServerLevel level, TriggerInstance instance, TriggerBlueprint blueprint) {
        for (TriggerBlueprint.BlockEntry entry : blueprint.blocks()) {
            BlockPos target = instance.worldPos().offset(entry.localPos());
            BlockState state = TriggerBlockStateCodec.decode(entry.state());
            level.setBlock(target, state, 3);
        }
        for (TriggerBlueprint.BlockEntry entry : blueprint.blocks()) {
            if (entry.blockEntitySnbt() == null || entry.blockEntitySnbt().isBlank()) {
                continue;
            }
            BlockPos target = instance.worldPos().offset(entry.localPos());
            BlockState state = level.getBlockState(target);
            try {
                CompoundTag tag = TagParser.parseCompoundFully(entry.blockEntitySnbt());
                tag.putInt("x", target.getX());
                tag.putInt("y", target.getY());
                tag.putInt("z", target.getZ());
                BlockEntity blockEntity = BlockEntity.loadStatic(target, state, tag, level.registryAccess());
                if (blockEntity != null) {
                    level.setBlockEntity(blockEntity);
                }
            } catch (CommandSyntaxException e) {
                Marallyzen.LOGGER.warn("TriggerEngine: failed to parse block entity snbt for {}", instance.id(), e);
            }
        }
    }

    private boolean passesCondition(ServerLevel level, String condition) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        String raw = condition.trim().toLowerCase(Locale.ROOT);
        if ("always".equals(raw)) {
            return true;
        }
        if ("time=day".equals(raw)) {
            long time = level.getDayTime() % 24000L;
            return time >= 0L && time < 12000L;
        }
        if ("time=night".equals(raw)) {
            long time = level.getDayTime() % 24000L;
            return time >= 12000L;
        }
        return true;
    }

    private boolean isInsideZone(ServerPlayer player, BlockPos center, int radius) {
        double cx = center.getX() + 0.5D;
        double cz = center.getZ() + 0.5D;
        return Math.abs(player.getX() - cx) <= radius && Math.abs(player.getZ() - cz) <= radius;
    }

    private ServerLevel resolveLevel(MinecraftServer server, String dimensionId) {
        if (server == null) {
            return null;
        }
        try {
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(dimensionId));
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                return level;
            }
        } catch (Exception ignored) {
        }
        return server.overworld();
    }
}
