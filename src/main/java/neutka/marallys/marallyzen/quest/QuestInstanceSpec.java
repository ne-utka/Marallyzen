package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;

public record QuestInstanceSpec(
        String world,
        BlockPos spawn,
        int groupRequired,
        GameType mode,
        boolean clearInventory
) {
    public static QuestInstanceSpec fromJson(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        String world = QuestJsonUtils.getString(obj, "world", "");
        if (world == null || world.isBlank()) {
            return null;
        }
        BlockPos spawn = parseSpawn(obj.get("spawn"));
        int groupRequired = Math.max(1, QuestJsonUtils.getInt(obj, "groupRequired", 1));
        GameType mode = parseMode(QuestJsonUtils.getString(obj, "mode", "adventure"));
        boolean clearInventory = QuestJsonUtils.getBoolean(obj, "clearInventory", false);
        return new QuestInstanceSpec(world, spawn, groupRequired, mode, clearInventory);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("world", world);
        if (spawn != null) {
            JsonArray spawnArray = new JsonArray();
            spawnArray.add(spawn.getX());
            spawnArray.add(spawn.getY());
            spawnArray.add(spawn.getZ());
            obj.add("spawn", spawnArray);
        }
        obj.addProperty("groupRequired", groupRequired);
        if (mode != null) {
            obj.addProperty("mode", mode.getName());
        }
        if (clearInventory) {
            obj.addProperty("clearInventory", true);
        }
        return obj;
    }

    private static BlockPos parseSpawn(com.google.gson.JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() < 3) {
            return null;
        }
        try {
            int x = array.get(0).getAsInt();
            int y = array.get(1).getAsInt();
            int z = array.get(2).getAsInt();
            return new BlockPos(x, y, z);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static GameType parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return GameType.ADVENTURE;
        }
        try {
            GameType result = GameType.byName(mode.toLowerCase());
            return result != null ? result : GameType.ADVENTURE;
        } catch (Exception ignored) {
            return GameType.ADVENTURE;
        }
    }
}
