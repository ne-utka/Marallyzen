package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.Marallyzen;

public class QuestProgressTracker {

    public ProgressResult handleEvent(ServerPlayer player, QuestDefinition definition, QuestInstance instance, QuestEvent event, QuestConditionEvaluator evaluator, QuestPlayerData data) {
        if (definition == null || instance == null || event == null) {
            return ProgressResult.noChange();
        }
        if (definition.flags().asyncSteps()) {
            return handleAsyncEvent(player, definition, instance, event, evaluator, data);
        }
        QuestStep step = definition.getStep(instance.currentStepIndex());
        if (step == null) {
            return ProgressResult.noChange();
        }
        if (!evaluator.evaluateAll(player, data, step.conditions())) {
            return ProgressResult.noChange();
        }
        for (QuestTriggerDef trigger : step.triggers()) {
            if (matchesTrigger(trigger, event)) {
                int increment = getIncrement(trigger);
                String key = step.id();
                int current = instance.stepProgress().getOrDefault(key, 0);
                int updated = current + increment;
                instance.stepProgress().put(key, updated);
                boolean stepCompleted = updated >= step.target();
                boolean stepAdvanced = false;
                if (stepCompleted) {
                    if (step.requiresHandover()) {
                        instance.setWaitingHandover(true, step.id());
                    } else {
                        instance.setCurrentStepIndex(instance.currentStepIndex() + 1);
                        stepAdvanced = true;
                    }
                    Marallyzen.LOGGER.info("QuestProgress: {} step '{}' completed by {} (event={})",
                            instance.questId(), step.id(), player.getGameProfile().getName(), event.type());
                }
                boolean shouldSync = shouldSync(step, updated, stepCompleted);
                return new ProgressResult(true, stepAdvanced, shouldSync);
            }
        }
        return ProgressResult.noChange();
    }

    private ProgressResult handleAsyncEvent(ServerPlayer player, QuestDefinition definition, QuestInstance instance, QuestEvent event, QuestConditionEvaluator evaluator, QuestPlayerData data) {
        boolean progressed = false;
        boolean shouldSync = false;
        for (QuestStep step : definition.steps()) {
            if (step == null) {
                continue;
            }
            if (!evaluator.evaluateAll(player, data, step.conditions())) {
                continue;
            }
            int target = Math.max(1, step.target());
            int current = instance.stepProgress().getOrDefault(step.id(), 0);
            if (current >= target) {
                continue;
            }
            for (QuestTriggerDef trigger : step.triggers()) {
                if (matchesTrigger(trigger, event)) {
                    int increment = getIncrement(trigger);
                    int updated = current + increment;
                    instance.stepProgress().put(step.id(), updated);
                    boolean stepCompleted = updated >= target;
                    if (stepCompleted && step.requiresHandover()) {
                        instance.setWaitingHandover(true, step.id());
                        return new ProgressResult(true, false, true);
                    }
                    progressed = true;
                    shouldSync |= shouldSync(step, updated, stepCompleted);
                    break;
                }
            }
        }
        if (!progressed) {
            return ProgressResult.noChange();
        }
        boolean allCompleted = true;
        for (QuestStep step : definition.steps()) {
            if (step == null) {
                continue;
            }
            int target = Math.max(1, step.target());
            int current = instance.stepProgress().getOrDefault(step.id(), 0);
            if (current < target) {
                allCompleted = false;
                break;
            }
        }
        if (allCompleted && instance.currentStepIndex() < definition.steps().size()) {
            instance.setCurrentStepIndex(definition.steps().size());
            return new ProgressResult(true, true, true);
        }
        return new ProgressResult(true, false, shouldSync);
    }

    public boolean matchesTrigger(QuestTriggerDef trigger, QuestEvent event) {
        if (trigger == null || trigger.type() == null) {
            return false;
        }
        String type = trigger.type();
        if (!type.equals(event.type())) {
            return false;
        }
        JsonObject data = trigger.data();
        if ("zone_enter".equals(type)) {
            String zoneId = QuestJsonUtils.getString(data, "zoneId", "");
            return zoneId.isBlank() || zoneId.equals(event.getString("zoneId", ""));
        }
        if ("npc_dialog".equals(type)) {
            String npcId = QuestJsonUtils.getString(data, "npcId", "");
            return npcId.isBlank() || npcId.equals(event.getString("npcId", ""));
        }
        if ("kill_entity".equals(type)) {
            String entityId = QuestJsonUtils.getString(data, "entity", "");
            return entityId.isBlank() || entityId.equals(event.getString("entity", ""));
        }
        if ("item_use".equals(type) || "item_pickup".equals(type)) {
            String itemId = QuestJsonUtils.getString(data, "item", "");
            return itemId.isBlank() || itemId.equals(event.getString("item", ""));
        }
        if ("dimension".equals(type)) {
            String dimensionId = QuestJsonUtils.getString(data, "dimension", "");
            return dimensionId.isBlank() || dimensionId.equals(event.getString("dimension", ""));
        }
        if ("biome".equals(type)) {
            String biomeId = QuestJsonUtils.getString(data, "biome", "");
            return biomeId.isBlank() || biomeId.equals(event.getString("biome", ""));
        }
        if ("time_of_day".equals(type)) {
            String value = QuestJsonUtils.getString(data, "value", "");
            return value.isBlank() || value.equalsIgnoreCase(event.getString("value", ""));
        }
        if ("weather".equals(type)) {
            String value = QuestJsonUtils.getString(data, "value", "");
            return value.isBlank() || value.equalsIgnoreCase(event.getString("value", ""));
        }
        if ("timer".equals(type)) {
            String timerId = QuestJsonUtils.getString(data, "id", "");
            return timerId.isBlank() || timerId.equals(event.getString("id", ""));
        }
        if ("custom".equals(type)) {
            String eventId = QuestJsonUtils.getString(data, "eventId", "");
            return eventId.isBlank() || eventId.equals(event.getString("eventId", ""));
        }
        return true;
    }

    private int getIncrement(QuestTriggerDef trigger) {
        if (trigger == null) {
            return 1;
        }
        JsonObject obj = trigger.data();
        return Math.max(1, QuestJsonUtils.getInt(obj, "count", 1));
    }

    private boolean shouldSync(QuestStep step, int updated, boolean stepCompleted) {
        if (stepCompleted) {
            return true;
        }
        if (!step.batched()) {
            return true;
        }
        int batchSize = step.batchSize() > 0 ? step.batchSize() : step.target();
        if (batchSize <= 1) {
            return true;
        }
        return updated % batchSize == 0;
    }

    public record ProgressResult(boolean progressed, boolean stepAdvanced, boolean shouldSync) {
        public static ProgressResult noChange() {
            return new ProgressResult(false, false, false);
        }
    }
}
