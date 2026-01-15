package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record QuestConditionDef(String type, JsonObject data) {
    public static List<QuestConditionDef> listFromJson(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<QuestConditionDef> list = new ArrayList<>();
        for (JsonElement element : array) {
            if (element instanceof JsonObject obj && obj.has("type")) {
                String type = QuestJsonUtils.getString(obj, "type", "");
                JsonObject data = obj.deepCopy();
                data.remove("type");
                list.add(new QuestConditionDef(type, data));
            }
        }
        return list;
    }

    public JsonObject toJson() {
        JsonObject obj = data != null ? data.deepCopy() : new JsonObject();
        obj.addProperty("type", type);
        return obj;
    }

    public static JsonArray toJsonArray(List<QuestConditionDef> conditions) {
        JsonArray array = new JsonArray();
        if (conditions == null) {
            return array;
        }
        for (QuestConditionDef condition : conditions) {
            if (condition != null) {
                array.add(condition.toJson());
            }
        }
        return array;
    }
}
