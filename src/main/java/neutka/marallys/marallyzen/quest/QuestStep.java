package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public record QuestStep(
        String id,
        String title,
        String type,
        List<QuestTriggerDef> triggers,
        List<QuestConditionDef> conditions,
        int target,
        boolean batched,
        int batchSize,
        boolean requiresHandover,
        List<QuestTriggerDef> handoverTriggers
) {
    public static QuestStep fromJson(JsonObject obj) {
        if (obj == null || !obj.has("id")) {
            return null;
        }
        String id = QuestJsonUtils.getString(obj, "id", "");
        String title = QuestJsonUtils.getString(obj, "title", id);
        String type = QuestJsonUtils.getString(obj, "type", "generic");
        List<QuestTriggerDef> triggers = QuestTriggerDef.listFromJson(obj.getAsJsonArray("triggers"));
        List<QuestConditionDef> conditions = QuestConditionDef.listFromJson(obj.getAsJsonArray("conditions"));

        int target = 1;
        JsonObject progressObj = obj.has("progress") ? obj.getAsJsonObject("progress") : null;
        if (progressObj != null) {
            target = QuestJsonUtils.getInt(progressObj, "target", target);
        }

        boolean batched = QuestJsonUtils.getBoolean(obj, "batched", false);
        int batchSize = QuestJsonUtils.getInt(obj, "batchSize", target);
        boolean requiresHandover = QuestJsonUtils.getBoolean(obj, "requiresHandover", false);
        List<QuestTriggerDef> handoverTriggers = QuestTriggerDef.listFromJson(obj.getAsJsonArray("handoverTriggers"));

        return new QuestStep(id, title, type, triggers, conditions, target, batched, batchSize, requiresHandover, handoverTriggers);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("title", title);
        obj.addProperty("type", type);
        JsonArray triggerArray = QuestTriggerDef.toJsonArray(triggers);
        if (triggerArray.size() > 0) {
            obj.add("triggers", triggerArray);
        }
        JsonArray conditionArray = QuestConditionDef.toJsonArray(conditions);
        if (conditionArray.size() > 0) {
            obj.add("conditions", conditionArray);
        }
        JsonObject progressObj = new JsonObject();
        progressObj.addProperty("target", target);
        obj.add("progress", progressObj);
        if (batched) {
            obj.addProperty("batched", true);
            if (batchSize > 0 && batchSize != target) {
                obj.addProperty("batchSize", batchSize);
            }
        }
        if (requiresHandover) {
            obj.addProperty("requiresHandover", true);
            JsonArray handoverArray = QuestTriggerDef.toJsonArray(handoverTriggers);
            if (handoverArray.size() > 0) {
                obj.add("handoverTriggers", handoverArray);
            }
        }
        return obj;
    }
}
