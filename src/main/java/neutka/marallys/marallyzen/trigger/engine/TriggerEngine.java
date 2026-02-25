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
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.TriggerAnimationStartPacket;
import neutka.marallys.marallyzen.network.TriggerAnimationStopPacket;
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
                tryActivate(instance, player, playerLevel);
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
            consumed |= tryActivate(instance, player, playerLevel);
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
        return tryActivate(instance, player, level);
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
                return tryActivate(instance, null, level) || true;
            }
            return false;
        }
        if ("redstone_power".equals(type)) {
            boolean powered = level.hasNeighborSignal(instance.bindPos());
            boolean risingEdge = powered && !instance.lastPowered();
            instance.setLastPowered(powered);
            if (risingEdge) {
                return tryActivate(instance, null, level);
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
                return tryActivate(instance, players.get(0), level);
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
        instanceManager.refreshActiveFile(instance);
        return true;
    }

    private boolean tryActivate(TriggerInstance instance, ServerPlayer player, ServerLevel level) {
        if (instance == null || level == null) {
            return false;
        }
        if (instance.structureState() != TriggerInstance.StructureState.HIDDEN) {
            return false;
        }
        if (instance.cooldownRemaining() > 0) {
            return false;
        }
        TriggerBlueprint blueprint = blueprintLoader.getBlueprint(instance.blueprintId());
        if (blueprint == null) {
            return false;
        }
        if (!passesCondition(level, blueprint.settings().condition())) {
            return false;
        }
        if (!triggerRegistry.shouldActivate(instance.triggerType(), player, instance.bindPos(), level)) {
            return false;
        }

        instance.setStructureState(TriggerInstance.StructureState.ANIMATING);
        instance.setAnimationTick(0);
        sendAnimationStart(instance, level, blueprint);
        animationController.onAnimationStart(level, instance, blueprint);
        instanceManager.persistInstance(instance);
        instanceManager.refreshActiveFile(instance);
        return true;
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
