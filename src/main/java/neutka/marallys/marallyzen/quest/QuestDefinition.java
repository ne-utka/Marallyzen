package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class QuestDefinition {
    private final String id;
    private final int version;
    private final String title;
    private final String description;
    private final QuestGiver giver;
    private final List<QuestConditionDef> conditions;
    private final List<QuestTriggerDef> triggers;
    private final List<QuestStep> steps;
    private final List<QuestConditionDef> failConditions;
    private final List<QuestRewardDef> rewards;
    private final QuestFlags flags;
    private final QuestCategory category;

    public QuestDefinition(
            String id,
            int version,
            String title,
            String description,
            QuestGiver giver,
            List<QuestConditionDef> conditions,
            List<QuestTriggerDef> triggers,
            List<QuestStep> steps,
            List<QuestConditionDef> failConditions,
            List<QuestRewardDef> rewards,
            QuestFlags flags,
            QuestCategory category
    ) {
        this.id = id;
        this.version = version;
        this.title = title;
        this.description = description;
        this.giver = giver;
        this.conditions = conditions != null ? conditions : List.of();
        this.triggers = triggers != null ? triggers : List.of();
        this.steps = steps != null ? steps : List.of();
        this.failConditions = failConditions != null ? failConditions : List.of();
        this.rewards = rewards != null ? rewards : List.of();
        this.flags = flags != null ? flags : new QuestFlags(false, false, false, false, false);
        this.category = category;
    }

    public String id() {
        return id;
    }

    public int version() {
        return version;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public QuestGiver giver() {
        return giver;
    }

    public List<QuestConditionDef> conditions() {
        return conditions;
    }

    public List<QuestTriggerDef> triggers() {
        return triggers;
    }

    public List<QuestStep> steps() {
        return steps;
    }

    public List<QuestConditionDef> failConditions() {
        return failConditions;
    }

    public List<QuestRewardDef> rewards() {
        return rewards;
    }

    public QuestFlags flags() {
        return flags;
    }

    public QuestCategory category() {
        return category;
    }

    public QuestCategory resolvedCategory() {
        if (category != null) {
            return category;
        }
        if (flags.story()) {
            return QuestCategory.STORY;
        }
        return QuestCategory.SIDE;
    }

    public QuestStep getStep(int index) {
        if (index < 0 || index >= steps.size()) {
            return null;
        }
        return steps.get(index);
    }

    public boolean isValid() {
        return id != null && !id.isBlank() && !steps.isEmpty();
    }

    public static QuestDefinition fromJson(JsonObject obj) {
        if (obj == null || !obj.has("id")) {
            return null;
        }
        String id = QuestJsonUtils.getString(obj, "id", "");
        int version = QuestJsonUtils.getInt(obj, "version", 1);
        String title = QuestJsonUtils.getString(obj, "title", id);
        String description = QuestJsonUtils.getString(obj, "description", "");
        QuestCategory category = parseCategory(QuestJsonUtils.getString(obj, "category", null));

        QuestGiver giver = QuestGiver.fromJson(obj.has("giver") ? obj.getAsJsonObject("giver") : null);
        QuestFlags flags = QuestFlags.fromJson(obj.has("flags") ? obj.getAsJsonObject("flags") : null);

        List<QuestConditionDef> conditions = QuestConditionDef.listFromJson(obj.getAsJsonArray("conditions"));
        List<QuestTriggerDef> triggers = QuestTriggerDef.listFromJson(obj.getAsJsonArray("triggers"));
        List<QuestConditionDef> failConditions = QuestConditionDef.listFromJson(obj.getAsJsonArray("failConditions"));
        List<QuestRewardDef> rewards = QuestRewardDef.listFromJson(obj.getAsJsonArray("rewards"));

        List<QuestStep> steps = new ArrayList<>();
        JsonArray stepsArray = obj.getAsJsonArray("steps");
        if (stepsArray != null) {
            for (JsonElement element : stepsArray) {
                if (element instanceof JsonObject stepObj) {
                    QuestStep step = QuestStep.fromJson(stepObj);
                    if (step != null) {
                        steps.add(step);
                    }
                }
            }
        }

        QuestDefinition definition = new QuestDefinition(
                id,
                version,
                title,
                description,
                giver,
                conditions,
                triggers,
                steps,
                failConditions,
                rewards,
                flags,
                category
        );
        if (!definition.isValid()) {
            Marallyzen.LOGGER.warn("QuestDefinition '{}' is invalid (missing id or steps)", id);
        }
        return definition;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("version", version);
        obj.addProperty("title", title);
        obj.addProperty("description", description);
        if (giver != null) {
            obj.add("giver", giver.toJson());
        }
        if (category != null) {
            obj.addProperty("category", category.name().toLowerCase());
        }
        JsonArray conditionArray = QuestConditionDef.toJsonArray(conditions);
        if (conditionArray.size() > 0) {
            obj.add("conditions", conditionArray);
        }
        JsonArray triggerArray = QuestTriggerDef.toJsonArray(triggers);
        if (triggerArray.size() > 0) {
            obj.add("triggers", triggerArray);
        }
        JsonArray stepsArray = new JsonArray();
        for (QuestStep step : steps) {
            if (step != null) {
                stepsArray.add(step.toJson());
            }
        }
        obj.add("steps", stepsArray);
        JsonArray failArray = QuestConditionDef.toJsonArray(failConditions);
        if (failArray.size() > 0) {
            obj.add("failConditions", failArray);
        }
        JsonArray rewardArray = QuestRewardDef.toJsonArray(rewards);
        if (rewardArray.size() > 0) {
            obj.add("rewards", rewardArray);
        }
        obj.add("flags", flags.toJson());
        return obj;
    }

    private static QuestCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return QuestCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public record QuestGiver(String type, String npcId) {
        public static QuestGiver fromJson(JsonObject obj) {
            if (obj == null) {
                return null;
            }
            String type = QuestJsonUtils.getString(obj, "type", "system");
            String npcId = QuestJsonUtils.getString(obj, "npcId", null);
            return new QuestGiver(type, npcId);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", type);
            if (npcId != null) {
                obj.addProperty("npcId", npcId);
            }
            return obj;
        }
    }

    public record QuestFlags(boolean daily, boolean repeatable, boolean story, boolean hidden, boolean asyncSteps) {
        public static QuestFlags fromJson(JsonObject obj) {
            if (obj == null) {
                return new QuestFlags(false, false, false, false, false);
            }
            return new QuestFlags(
                    QuestJsonUtils.getBoolean(obj, "daily", false),
                    QuestJsonUtils.getBoolean(obj, "repeatable", false),
                    QuestJsonUtils.getBoolean(obj, "story", false),
                    QuestJsonUtils.getBoolean(obj, "hidden", false),
                    QuestJsonUtils.getBoolean(obj, "asyncSteps", false)
            );
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("daily", daily);
            obj.addProperty("repeatable", repeatable);
            obj.addProperty("story", story);
            obj.addProperty("hidden", hidden);
            if (asyncSteps) {
                obj.addProperty("asyncSteps", true);
            }
            return obj;
        }
    }
}
