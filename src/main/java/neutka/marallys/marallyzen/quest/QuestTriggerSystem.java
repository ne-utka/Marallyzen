package neutka.marallys.marallyzen.quest;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestTriggerSystem {
    private final QuestConditionEvaluator conditionEvaluator;
    private final QuestProgressTracker progressTracker;
    private final QuestRewardHandler rewardHandler;

    public QuestTriggerSystem(QuestConditionEvaluator conditionEvaluator, QuestProgressTracker progressTracker, QuestRewardHandler rewardHandler) {
        this.conditionEvaluator = conditionEvaluator;
        this.progressTracker = progressTracker;
        this.rewardHandler = rewardHandler;
    }

    public boolean handleEvent(ServerPlayer player, QuestPlayerData data, Map<String, QuestDefinition> definitions, QuestEvent event, QuestManager manager) {
        boolean changed = false;
        changed |= handleStartTriggers(player, data, definitions, event, manager);
        changed |= handleActiveTriggers(player, data, definitions, event, manager);
        return changed;
    }

    private boolean handleStartTriggers(ServerPlayer player, QuestPlayerData data, Map<String, QuestDefinition> definitions, QuestEvent event, QuestManager manager) {
        boolean changed = false;
        for (QuestDefinition definition : definitions.values()) {
            QuestInstance existing = data.quests().get(definition.id());
            if (existing != null && existing.state() != QuestInstance.State.NOT_STARTED) {
                if (existing.state() == QuestInstance.State.COMPLETED && definition.flags().repeatable()) {
                    // Allow re-starting repeatable quests on trigger
                } else {
                    continue;
                }
            }
            if (!matchesStartTrigger(definition, event, player)) {
                continue;
            }
            if (!conditionEvaluator.evaluateAll(player, data, definition.conditions())) {
                continue;
            }
            QuestInstance instance = manager.startQuest(player, definition.id());
            if (instance != null) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean handleActiveTriggers(ServerPlayer player, QuestPlayerData data, Map<String, QuestDefinition> definitions, QuestEvent event, QuestManager manager) {
        boolean changed = false;
        for (QuestInstance instance : new ArrayList<>(data.quests().values())) {
            if (instance.state() != QuestInstance.State.ACTIVE) {
                continue;
            }
            QuestDefinition definition = definitions.get(instance.questId());
            if (definition == null) {
                continue;
            }
            if (shouldFail(player, data, definition)) {
                manager.failQuest(player, instance);
                changed = true;
                continue;
            }
            if (instance.isWaitingHandover()) {
                if (tryHandleHandover(player, instance, definition, event, manager)) {
                    changed = true;
                }
                continue;
            }
            int beforeStep = instance.currentStepIndex();
            QuestProgressTracker.ProgressResult result = progressTracker.handleEvent(player, definition, instance, event, conditionEvaluator, data);
            if (result.progressed()) {
                if (result.shouldSync()) {
                    changed = true;
                }
                if (instance.currentStepIndex() != beforeStep) {
                    manager.onStepChanged(player, definition, instance);
                }
                if (instance.currentStepIndex() >= definition.steps().size()) {
                    manager.completeQuest(player, instance, definition);
                }
            }
        }
        return changed;
    }

    private boolean tryHandleHandover(ServerPlayer player, QuestInstance instance, QuestDefinition definition, QuestEvent event, QuestManager manager) {
        QuestStep step = definition.getStep(instance.currentStepIndex());
        if (step == null) {
            instance.setWaitingHandover(false, null);
            return true;
        }
        List<QuestTriggerDef> triggers = step.handoverTriggers();
        if (triggers == null || triggers.isEmpty()) {
            triggers = step.triggers();
        }
        if (triggers == null || triggers.isEmpty()) {
            return false;
        }
        for (QuestTriggerDef trigger : triggers) {
            if (progressTracker.matchesTrigger(trigger, event)) {
                instance.setWaitingHandover(false, null);
                instance.setCurrentStepIndex(instance.currentStepIndex() + 1);
                manager.onStepChanged(player, definition, instance);
                if (instance.currentStepIndex() >= definition.steps().size()) {
                    manager.completeQuest(player, instance, definition);
                }
                return true;
            }
        }
        return false;
    }

    private boolean matchesStartTrigger(QuestDefinition definition, QuestEvent event, ServerPlayer player) {
        if (definition.triggers() == null || definition.triggers().isEmpty()) {
            return false;
        }
        for (QuestTriggerDef trigger : definition.triggers()) {
            if (trigger == null || trigger.type() == null) {
                continue;
            }
            if (!trigger.type().equals(event.type())) {
                continue;
            }
            if (progressTracker != null && progressTracker.matchesTrigger(trigger, event)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldFail(ServerPlayer player, QuestPlayerData data, QuestDefinition definition) {
        if (definition.failConditions() == null || definition.failConditions().isEmpty()) {
            return false;
        }
        return conditionEvaluator.evaluateAll(player, data, definition.failConditions());
    }
}
