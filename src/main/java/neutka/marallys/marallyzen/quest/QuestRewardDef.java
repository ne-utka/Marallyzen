package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record QuestRewardDef(String type, JsonObject data) {
    public static List<QuestRewardDef> listFromJson(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<QuestRewardDef> list = new ArrayList<>();
        for (JsonElement element : array) {
            if (element instanceof JsonObject obj && obj.has("type")) {
                String type = QuestJsonUtils.getString(obj, "type", "");
                JsonObject data = obj.deepCopy();
                data.remove("type");
                list.add(new QuestRewardDef(type, data));
            }
        }
        return list;
    }

    public JsonObject toJson() {
        JsonObject obj = data != null ? data.deepCopy() : new JsonObject();
        obj.addProperty("type", type);
        return obj;
    }

    public static JsonArray toJsonArray(List<QuestRewardDef> rewards) {
        JsonArray array = new JsonArray();
        if (rewards == null) {
            return array;
        }
        for (QuestRewardDef reward : rewards) {
            if (reward != null) {
                array.add(reward.toJson());
            }
        }
        return array;
    }
}
