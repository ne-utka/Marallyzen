package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QuestPlayerData {
    private final Map<String, QuestInstance> quests = new HashMap<>();
    private final Map<String, Boolean> npcTradeUnlocked = new HashMap<>();
    private final Set<String> activeFarmQuests = new HashSet<>();
    private boolean hasJoinedBefore;
    private String activeQuestId;

    public Map<String, QuestInstance> quests() {
        return quests;
    }

    public boolean hasJoinedBefore() {
        return hasJoinedBefore;
    }

    public void setHasJoinedBefore(boolean hasJoinedBefore) {
        this.hasJoinedBefore = hasJoinedBefore;
    }

    public String activeQuestId() {
        return activeQuestId;
    }

    public void setActiveQuestId(String activeQuestId) {
        this.activeQuestId = activeQuestId;
    }

    public Set<String> activeFarmQuests() {
        return Collections.unmodifiableSet(activeFarmQuests);
    }

    public void addActiveFarmQuest(String questId) {
        if (questId != null && !questId.isBlank()) {
            activeFarmQuests.add(questId);
        }
    }

    public void removeActiveFarmQuest(String questId) {
        if (questId != null) {
            activeFarmQuests.remove(questId);
        }
    }

    public boolean isFarmQuestActive(String questId) {
        return questId != null && activeFarmQuests.contains(questId);
    }

    public void setNpcTradeUnlocked(String npcId, boolean unlocked) {
        npcTradeUnlocked.put(npcId, unlocked);
    }

    public boolean isNpcTradeUnlocked(String npcId) {
        return npcTradeUnlocked.getOrDefault(npcId, false);
    }

    public Map<String, Boolean> npcTradeUnlocked() {
        return Collections.unmodifiableMap(npcTradeUnlocked);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        JsonObject questsObj = new JsonObject();
        for (Map.Entry<String, QuestInstance> entry : quests.entrySet()) {
            questsObj.add(entry.getKey(), entry.getValue().toJson());
        }
        obj.add("quests", questsObj);
        obj.addProperty("hasJoinedBefore", hasJoinedBefore);
        if (activeQuestId != null) {
            obj.addProperty("activeQuestId", activeQuestId);
        }
        if (!activeFarmQuests.isEmpty()) {
            com.google.gson.JsonArray farmArray = new com.google.gson.JsonArray();
            for (String questId : activeFarmQuests) {
                farmArray.add(questId);
            }
            obj.add("activeFarmQuests", farmArray);
        }
        JsonObject tradesObj = new JsonObject();
        for (Map.Entry<String, Boolean> entry : npcTradeUnlocked.entrySet()) {
            tradesObj.addProperty(entry.getKey(), entry.getValue());
        }
        obj.add("npcTradeUnlocked", tradesObj);
        return obj;
    }

    public static QuestPlayerData fromJson(JsonObject obj) {
        QuestPlayerData data = new QuestPlayerData();
        if (obj == null) {
            return data;
        }
        data.hasJoinedBefore = QuestJsonUtils.getBoolean(obj, "hasJoinedBefore", false);
        data.activeQuestId = QuestJsonUtils.getString(obj, "activeQuestId", null);
        var farmArray = obj.getAsJsonArray("activeFarmQuests");
        if (farmArray != null) {
            for (var element : farmArray) {
                if (element != null && element.isJsonPrimitive()) {
                    String questId = element.getAsString();
                    if (questId != null && !questId.isBlank()) {
                        data.activeFarmQuests.add(questId);
                    }
                }
            }
        }
        JsonObject questsObj = obj.getAsJsonObject("quests");
        if (questsObj != null) {
            for (String questId : questsObj.keySet()) {
                QuestInstance instance = QuestInstance.fromJson(questsObj.getAsJsonObject(questId));
                if (instance != null) {
                    data.quests.put(questId, instance);
                }
            }
        }
        JsonObject tradesObj = obj.getAsJsonObject("npcTradeUnlocked");
        if (tradesObj != null) {
            for (String key : tradesObj.keySet()) {
                data.npcTradeUnlocked.put(key, tradesObj.get(key).getAsBoolean());
            }
        }
        return data;
    }
}
