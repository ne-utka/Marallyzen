package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record QuestTriggerDef(String type, JsonObject data) {
    public static List<QuestTriggerDef> listFromJson(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<QuestTriggerDef> list = new ArrayList<>();
        for (JsonElement element : array) {
            if (element instanceof JsonObject obj && obj.has("type")) {
                String type = QuestJsonUtils.getString(obj, "type", "");
                JsonObject data = obj.deepCopy();
                data.remove("type");
                list.add(new QuestTriggerDef(type, data));
            }
        }
        return list;
    }

    public JsonObject toJson() {
        JsonObject obj = data != null ? data.deepCopy() : new JsonObject();
        obj.addProperty("type", type);
        return obj;
    }

    public static JsonArray toJsonArray(List<QuestTriggerDef> triggers) {
        JsonArray array = new JsonArray();
        if (triggers == null) {
            return array;
        }
        for (QuestTriggerDef trigger : triggers) {
            if (trigger != null) {
                array.add(trigger.toJson());
            }
        }
        return array;
    }
}
