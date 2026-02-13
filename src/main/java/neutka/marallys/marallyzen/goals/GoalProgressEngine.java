package neutka.marallys.marallyzen.goals;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.entity.GoalDisplayEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central server runtime for global goals.
 */
public final class GoalProgressEngine {
    public record GoalOperationResult(boolean success, String message) {
    }

    private static final GoalProgressEngine INSTANCE = new GoalProgressEngine();

    private final GoalScriptLoader scriptLoader = GoalScriptLoader.getInstance();
    private final GoalRegistry registry = GoalRegistry.getInstance();
    private final GoalZoneManager zoneManager = GoalZoneManager.getInstance();
    private final Map<String, GoalInstance> activeGoals = new ConcurrentHashMap<>();

    private MinecraftServer server;
    private GoalSavedData savedData;
    private boolean initialized;

    private GoalProgressEngine() {
    }

    public static GoalProgressEngine getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize(MinecraftServer server) {
        this.server = server;
        scriptLoader.reloadScripts();
        if (server == null || server.overworld() == null) {
            initialized = false;
            return;
        }
        this.savedData = GoalSavedData.get(server.overworld());
        rebuildRuntimeFromSaved();
        initialized = true;
        Marallyzen.LOGGER.info("GoalProgressEngine initialized: {} active goal(s)", activeGoals.size());
    }

    public synchronized void reload(MinecraftServer server) {
        if (server != null) {
            this.server = server;
        }
        scriptLoader.reloadScripts();
        if (this.server == null || this.server.overworld() == null) {
            initialized = false;
            return;
        }
        this.savedData = GoalSavedData.get(this.server.overworld());
        rebuildRuntimeFromSaved();
        initialized = true;
        Marallyzen.LOGGER.info("GoalProgressEngine reloaded: {} script(s), {} runtime goal(s)", scriptLoader.getLoadedScriptIds().size(), activeGoals.size());
    }

    public synchronized void shutdown() {
        activeGoals.clear();
        zoneManager.clear();
        initialized = false;
    }

    public void onServerTick() {
        if (!initialized || server == null || savedData == null) {
            return;
        }
        if (activeGoals.isEmpty()) {
            return;
        }

        ArrayList<String> removeQueue = new ArrayList<>();
        for (GoalInstance instance : activeGoals.values()) {
            ServerLevel level = resolveLevel(instance.dimensionId());
            if (level == null) {
                continue;
            }
            GoalRuntimeContext context = new GoalRuntimeContext(server, level, savedData, zoneManager, level.getGameTime());
            boolean dirty = false;

            long previousProgress = instance.progress();
            boolean wasCompleted = instance.completed();
            int previousMilestones = instance.triggeredMilestonesSnapshot().size();

            GoalType goalType = registry.getGoalType(instance.script().goal().type());
            if (goalType == null) {
                continue;
            }

            if (!instance.completed()) {
                goalType.tick(context, instance);
            }

            dirty |= instance.progress() != previousProgress;

            for (GoalScript.Milestone milestone : instance.script().milestones()) {
                if (instance.progress() < milestone.at() || instance.hasMilestone(milestone.at())) {
                    continue;
                }
                instance.markMilestone(milestone.at());
                executeCommands(level, instance.spawnPos(), milestone.commands());
                if (milestone.globalMessage()) {
                    broadcast(Component.literal("Goal " + instance.goalId() + ": milestone " + milestone.at()));
                }
            }

            if (!instance.completed() && shouldComplete(instance)) {
                instance.setCompleted(true);
                GoalScript.OnComplete onComplete = instance.script().onComplete();
                executeCommands(level, instance.spawnPos(), onComplete.commands());
                spawnCompleteParticles(level, instance.spawnPos(), onComplete.particles());
                if (onComplete.globalMessage()) {
                    broadcast(Component.literal("Goal completed: " + instance.script().display().title()));
                }
                if (onComplete.removeGoal()) {
                    removeQueue.add(instance.goalId());
                }
            }

            dirty |= wasCompleted != instance.completed();
            dirty |= previousMilestones != instance.triggeredMilestonesSnapshot().size();

            GoalDisplayEntity displayEntity = ensureDisplayEntity(instance, level);
            if (displayEntity != null) {
                Map<String, String> vars = buildDisplayVariables(context, instance, goalType);
                displayEntity.updateRuntime(instance.progress(), instance.target(), vars);
            }

            if (dirty) {
                savedData.setDirty();
            }
        }

        for (String goalId : removeQueue) {
            removeGoal(goalId);
        }
    }

    public synchronized GoalOperationResult addGoal(ServerPlayer player, String scriptId) {
        if (player == null) {
            return new GoalOperationResult(false, "Only players can use this command.");
        }
        if (!initialized) {
            initialize(player.level().getServer());
        }
        if (savedData == null) {
            return new GoalOperationResult(false, "Goal system is not ready.");
        }
        String id = normalizeId(scriptId);
        GoalScript script = scriptLoader.getScript(id);
        if (script == null) {
            return new GoalOperationResult(false, "Script not found: " + id + " (config/marallyzen/goals/" + id + ".json)");
        }
        if (savedData.hasGoal(id)) {
            return new GoalOperationResult(false, "Goal already exists: " + id);
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return new GoalOperationResult(false, "Cannot resolve server level.");
        }
        BlockPos spawnPos = BlockPos.containing(player.getPosition(1.0F));
        GoalSavedData.GoalProgressData progressData = GoalSavedData.GoalProgressData.create(
                spawnPos,
                level.dimension().identifier().toString(),
                script.zone().radius(),
                script.zone().protect()
        );
        savedData.putGoal(id, progressData);
        GoalInstance instance = new GoalInstance(id, script, progressData);
        activeGoals.put(id, instance);

        zoneManager.registerZone(id, progressData.dimensionId(), progressData.spawnPos(), progressData.zoneRadius(), progressData.protect());
        GoalDisplayEntity displayEntity = ensureDisplayEntity(instance, level);
        if (displayEntity != null) {
            displayEntity.updateRuntime(0L, script.goal().target(), Map.of());
        }
        savedData.setDirty();
        return new GoalOperationResult(true, "Goal added: " + id);
    }

    public synchronized GoalOperationResult removeGoal(String scriptId) {
        if (savedData == null) {
            return new GoalOperationResult(false, "Goal system is not ready.");
        }
        String id = normalizeId(scriptId);
        GoalSavedData.GoalProgressData data = savedData.getGoal(id);
        if (data == null) {
            return new GoalOperationResult(false, "Goal not found: " + id);
        }

        removeDisplayEntity(id, data);
        zoneManager.removeZone(id);
        activeGoals.remove(id);
        savedData.removeGoal(id);
        savedData.setDirty();
        return new GoalOperationResult(true, "Goal removed: " + id);
    }

    public Set<String> getLoadedScriptIds() {
        return scriptLoader.getLoadedScriptIds();
    }

    public Set<String> getPersistedGoalIds() {
        if (savedData == null) {
            return Set.of();
        }
        return savedData.ids();
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void rebuildRuntimeFromSaved() {
        activeGoals.clear();
        zoneManager.clear();
        if (savedData == null) {
            return;
        }
        for (Map.Entry<String, GoalSavedData.GoalProgressData> entry : savedData.goals().entrySet()) {
            String goalId = normalizeId(entry.getKey());
            GoalScript script = scriptLoader.getScript(goalId);
            if (script == null) {
                Marallyzen.LOGGER.warn("GoalProgressEngine: script '{}' missing, goal state kept in SavedData", goalId);
                continue;
            }
            GoalSavedData.GoalProgressData progressData = entry.getValue();
            zoneManager.registerZone(goalId, progressData.dimensionId(), progressData.spawnPos(), progressData.zoneRadius(), progressData.protect());

            GoalInstance instance = new GoalInstance(goalId, script, progressData);
            activeGoals.put(goalId, instance);

            ServerLevel level = resolveLevel(progressData.dimensionId());
            if (level != null) {
                GoalDisplayEntity displayEntity = ensureDisplayEntity(instance, level);
                if (displayEntity != null) {
                    displayEntity.updateRuntime(instance.progress(), instance.target(), Map.of());
                }
            }
        }
    }

    private GoalDisplayEntity ensureDisplayEntity(GoalInstance instance, ServerLevel level) {
        BlockPos spawn = instance.spawnPos();
        String goalId = instance.goalId();
        String storedUuid = instance.entityUuid();

        GoalDisplayEntity entity = findDisplayEntity(level, storedUuid);
        if (entity == null) {
            entity = findDisplayEntityByGoalId(level, goalId, spawn, 3.0D);
        }
        if (entity == null && storedUuid != null && !storedUuid.isBlank() && !level.isLoaded(spawn)) {
            // If entity UUID is known but the chunk is not loaded yet, defer spawning to avoid duplicates on reconnect/reload.
            return null;
        }
        if (entity == null) {
            entity = new GoalDisplayEntity(Marallyzen.GOAL_DISPLAY_ENTITY.get(), level);
            entity.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            if (!level.addFreshEntity(entity)) {
                return null;
            }
        }

        String entityUuid = entity.getUUID().toString();
        if (!entityUuid.equals(storedUuid)) {
            instance.setEntityUuid(entityUuid);
            savedData.setDirty();
        }

        discardDuplicateDisplayEntities(level, goalId, entity.getUUID(), spawn, 3.0D);
        entity.configureFromScript(instance.goalId(), instance.script());
        return entity;
    }

    private GoalDisplayEntity findDisplayEntity(ServerLevel level, String uuidRaw) {
        if (level == null || uuidRaw == null || uuidRaw.isBlank()) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(uuidRaw);
            var entity = level.getEntity(uuid);
            if (entity instanceof GoalDisplayEntity goalDisplayEntity && goalDisplayEntity.isAlive()) {
                return goalDisplayEntity;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private GoalDisplayEntity findDisplayEntityByGoalId(ServerLevel level, String goalId, BlockPos centerPos, double searchRadius) {
        if (level == null || goalId == null || goalId.isBlank() || centerPos == null) {
            return null;
        }
        Vec3 center = Vec3.atCenterOf(centerPos);
        AABB box = AABB.ofSize(center, searchRadius * 2.0D, searchRadius * 2.0D, searchRadius * 2.0D);
        for (GoalDisplayEntity candidate : level.getEntitiesOfClass(
                GoalDisplayEntity.class,
                box,
                e -> e.isAlive() && goalId.equalsIgnoreCase(e.goalId())
        )) {
            return candidate;
        }
        return null;
    }

    private void discardDuplicateDisplayEntities(ServerLevel level, String goalId, UUID keepUuid, BlockPos centerPos, double searchRadius) {
        if (level == null || goalId == null || goalId.isBlank() || keepUuid == null || centerPos == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(centerPos);
        AABB box = AABB.ofSize(center, searchRadius * 2.0D, searchRadius * 2.0D, searchRadius * 2.0D);
        for (GoalDisplayEntity candidate : level.getEntitiesOfClass(
                GoalDisplayEntity.class,
                box,
                e -> e.isAlive() && goalId.equalsIgnoreCase(e.goalId())
        )) {
            if (!keepUuid.equals(candidate.getUUID())) {
                candidate.discard();
            }
        }
    }

    private void discardDisplayEntitiesByGoalId(ServerLevel level, String goalId, BlockPos centerPos, double searchRadius) {
        if (level == null || goalId == null || goalId.isBlank() || centerPos == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(centerPos);
        AABB box = AABB.ofSize(center, searchRadius * 2.0D, searchRadius * 2.0D, searchRadius * 2.0D);
        for (GoalDisplayEntity candidate : level.getEntitiesOfClass(
                GoalDisplayEntity.class,
                box,
                e -> e.isAlive() && goalId.equalsIgnoreCase(e.goalId())
        )) {
            candidate.discard();
        }
    }

    private void removeDisplayEntity(String goalId, GoalSavedData.GoalProgressData data) {
        if (data == null) {
            return;
        }
        ServerLevel level = resolveLevel(data.dimensionId());
        if (level == null) {
            return;
        }
        if (!data.entityUuid().isBlank()) {
            try {
                UUID uuid = UUID.fromString(data.entityUuid());
                var entity = level.getEntity(uuid);
                if (entity instanceof GoalDisplayEntity displayEntity) {
                    displayEntity.discard();
                }
            } catch (Exception ignored) {
            }
        }
        discardDisplayEntitiesByGoalId(level, goalId, data.spawnPos(), 3.0D);
        data.setEntityUuid("");
    }

    private boolean shouldComplete(GoalInstance instance) {
        long target = instance.target();
        if (target <= 0L) {
            return true;
        }
        return instance.progress() >= target;
    }

    private Map<String, String> buildDisplayVariables(GoalRuntimeContext context, GoalInstance instance, GoalType goalType) {
        Map<String, String> vars = new HashMap<>();
        long progress = instance.progress();
        long target = instance.target();
        long percent = target <= 0L ? 0L : Math.round((progress * 100.0D) / Math.max(1L, target));

        vars.put("progress", Long.toString(progress));
        vars.put("target", Long.toString(target));
        vars.put("percent", Long.toString(percent));
        vars.putAll(registry.collectPlaceholderValues(context, instance));
        goalType.collectDisplayVariables(context, instance, vars);

        CompoundTag customData = instance.customData();
        if (customData.contains("display_vars")) {
            CompoundTag displayVars = customData.getCompound("display_vars").orElse(null);
            if (displayVars != null) {
                for (String key : displayVars.keySet()) {
                    vars.put(key, displayVars.getString(key).orElse(""));
                }
            }
        }
        return vars;
    }

    private void executeCommands(ServerLevel level, BlockPos pos, java.util.List<String> commands) {
        if (server == null || level == null || commands == null || commands.isEmpty()) {
            return;
        }
        CommandSourceStack source = server.createCommandSourceStack();

        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            try {
                server.getCommands().performPrefixedCommand(source, command);
            } catch (Exception e) {
                Marallyzen.LOGGER.error("GoalProgressEngine: failed to execute command '{}'", command, e);
            }
        }
    }

    private void spawnCompleteParticles(ServerLevel level, BlockPos pos, String particleId) {
        if (level == null || pos == null || particleId == null || particleId.isBlank()) {
            return;
        }
        String normalized = particleId.trim().toLowerCase(Locale.ROOT);
        if ("firework".equals(normalized) || "fireworks".equals(normalized)) {
            level.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 16, 0.4D, 0.4D, 0.4D, 0.02D);
            return;
        }
        if ("happy_villager".equals(normalized)) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 10, 0.3D, 0.3D, 0.3D, 0.01D);
        }
    }

    private void broadcast(Component component) {
        if (server == null || component == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }
    }

    private ServerLevel resolveLevel(String dimensionId) {
        if (server == null) {
            return null;
        }
        try {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionId));
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                return level;
            }
        } catch (Exception ignored) {
        }
        return server.overworld();
    }

    private String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
