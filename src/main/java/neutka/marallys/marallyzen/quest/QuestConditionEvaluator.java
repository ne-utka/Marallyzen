package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class QuestConditionEvaluator {

    public boolean evaluateAll(ServerPlayer player, QuestPlayerData data, List<QuestConditionDef> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (QuestConditionDef condition : conditions) {
            if (!evaluateCondition(player, data, condition)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateCondition(ServerPlayer player, QuestPlayerData data, QuestConditionDef condition) {
        if (condition == null || condition.type() == null) {
            return true;
        }
        JsonObject obj = condition.data();
        String type = condition.type();

        return switch (type) {
            case "quest_completed" -> hasQuestState(data, QuestInstance.State.COMPLETED, QuestJsonUtils.getString(obj, "questId", ""));
            case "quest_active" -> hasQuestState(data, QuestInstance.State.ACTIVE, QuestJsonUtils.getString(obj, "questId", ""));
            case "quest_not_started" -> hasQuestState(data, QuestInstance.State.NOT_STARTED, QuestJsonUtils.getString(obj, "questId", ""));
            case "quest_step" -> hasQuestStep(data,
                    QuestJsonUtils.getString(obj, "questId", ""),
                    QuestJsonUtils.getInt(obj, "stepIndex", -1),
                    QuestJsonUtils.getString(obj, "stepId", null));
            case "item_in_inventory" -> hasItem(player, QuestJsonUtils.getString(obj, "item", ""), QuestJsonUtils.getInt(obj, "count", 1));
            case "dimension" -> matchesDimension(player, QuestJsonUtils.getString(obj, "dimension", ""));
            case "biome" -> matchesBiome(player, QuestJsonUtils.getString(obj, "biome", ""));
            case "time_of_day" -> matchesTime(player, QuestJsonUtils.getString(obj, "value", ""));
            case "time_range" -> matchesTimeRange(player, QuestJsonUtils.getInt(obj, "start", 0), QuestJsonUtils.getInt(obj, "end", 23999));
            case "weather" -> matchesWeather(player, QuestJsonUtils.getString(obj, "value", ""));
            case "cooldown" -> cooldownPassed(data, QuestJsonUtils.getString(obj, "questId", ""), QuestJsonUtils.getLong(obj, "seconds", 0));
            case "trade_unlocked" -> data.isNpcTradeUnlocked(QuestJsonUtils.getString(obj, "npcId", ""));
            default -> true;
        };
    }

    private boolean hasQuestState(QuestPlayerData data, QuestInstance.State state, String questId) {
        if (questId == null || questId.isBlank()) {
            return false;
        }
        QuestInstance instance = data.quests().get(questId);
        if (instance == null) {
            return state == QuestInstance.State.NOT_STARTED;
        }
        return instance.state() == state;
    }

    private boolean hasQuestStep(QuestPlayerData data, String questId, int stepIndex, String stepId) {
        if (questId == null || questId.isBlank()) {
            return false;
        }
        QuestInstance instance = data.quests().get(questId);
        if (instance == null) {
            return false;
        }
        if (stepIndex >= 0) {
            return instance.currentStepIndex() == stepIndex;
        }
        return stepId == null || stepId.isBlank();
    }

    private boolean hasItem(ServerPlayer player, String itemId, int count) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        ResourceLocation id = ResourceLocation.parse(itemId);
        var item = BuiltInRegistries.ITEM.get(id);
        if (item == null) {
            return false;
        }
        int total = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
                if (total >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesDimension(ServerPlayer player, String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) {
            return true;
        }
        return player.level().dimension().location().toString().equals(dimensionId);
    }

    private boolean matchesBiome(ServerPlayer player, String biomeId) {
        if (biomeId == null || biomeId.isBlank()) {
            return true;
        }
        BlockPos pos = player.blockPosition();
        var key = player.level().getBiome(pos).unwrapKey();
        return key.map(resourceKey -> resourceKey.location().toString().equals(biomeId)).orElse(false);
    }

    private boolean matchesTime(ServerPlayer player, String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        long time = player.level().getDayTime() % 24000L;
        boolean isDay = time >= 0 && time < 12000;
        if ("day".equalsIgnoreCase(value)) {
            return isDay;
        }
        if ("night".equalsIgnoreCase(value)) {
            return !isDay;
        }
        return true;
    }

    private boolean matchesTimeRange(ServerPlayer player, int start, int end) {
        long time = player.level().getDayTime() % 24000L;
        if (start <= end) {
            return time >= start && time <= end;
        }
        return time >= start || time <= end;
    }

    private boolean matchesWeather(ServerPlayer player, String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        boolean raining = player.level().isRaining();
        boolean thundering = player.level().isThundering();
        return switch (value) {
            case "rain" -> raining;
            case "storm" -> thundering;
            case "clear" -> !raining;
            default -> true;
        };
    }

    private boolean cooldownPassed(QuestPlayerData data, String questId, long seconds) {
        if (questId == null || questId.isBlank()) {
            return true;
        }
        QuestInstance instance = data.quests().get(questId);
        if (instance == null || instance.completedAt() == 0) {
            return true;
        }
        long cooldownMs = seconds * 1000L;
        return System.currentTimeMillis() - instance.completedAt() >= cooldownMs;
    }
}
