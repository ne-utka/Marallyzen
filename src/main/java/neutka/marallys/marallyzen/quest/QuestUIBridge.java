package neutka.marallys.marallyzen.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.QuestSyncPacket;

import java.util.Map;

public final class QuestUIBridge {
    private static final Gson GSON = new GsonBuilder().create();

    private QuestUIBridge() {
    }

    public static void sendFullSync(ServerPlayer player,
                                    Map<String, QuestDefinition> definitions,
                                    QuestPlayerData data,
                                    Map<String, QuestZoneDefinition> zones) {
        if (player == null) {
            return;
        }
        JsonObject root = new JsonObject();
        JsonObject defs = new JsonObject();
        if (definitions != null) {
            for (Map.Entry<String, QuestDefinition> entry : definitions.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                defs.add(entry.getKey(), entry.getValue().toJson());
            }
        }
        root.add("definitions", defs);
        root.add("player", data != null ? data.toJson() : new JsonObject());
        JsonObject activeZone = buildActiveZone(player, definitions, data, zones);
        if (activeZone != null) {
            root.add("activeZone", activeZone);
        }
        String payload = GSON.toJson(root);
        NetworkHelper.sendToPlayer(player, new QuestSyncPacket(payload));
    }

    private static JsonObject buildActiveZone(ServerPlayer player,
                                              Map<String, QuestDefinition> definitions,
                                              QuestPlayerData data,
                                              Map<String, QuestZoneDefinition> zones) {
        if (player == null || definitions == null || data == null || zones == null || zones.isEmpty()) {
            return null;
        }
        QuestZoneContext context = findActiveZoneContext(definitions, data, zones);
        if (context == null || context.zone == null) {
            return null;
        }
        QuestZoneDefinition zone = context.zone;
        QuestCategory category = context.category != null ? context.category : QuestCategory.SIDE;
        var level = player.getServer() != null ? player.getServer().getLevel(zone.dimension()) : null;
        if (level == null) {
            level = player.serverLevel();
        }
        double minX;
        double minY;
        double minZ;
        double maxX;
        double maxY;
        double maxZ;
        double centerX;
        double centerY;
        double centerZ;

        if (zone.shape() == QuestZoneDefinition.Shape.SPHERE && zone.center() != null) {
            centerX = zone.center().getX() + 0.5;
            centerY = zone.center().getY() + 0.5;
            centerZ = zone.center().getZ() + 0.5;
            double radius = zone.radius();
            minX = centerX - radius;
            maxX = centerX + radius;
            minZ = centerZ - radius;
            maxZ = centerZ + radius;
        } else if (zone.min() != null && zone.max() != null) {
            minX = zone.min().getX();
            minZ = zone.min().getZ();
            maxX = zone.max().getX() + 1.0;
            maxZ = zone.max().getZ() + 1.0;
            centerX = (minX + maxX) * 0.5;
            centerZ = (minZ + maxZ) * 0.5;
            centerY = 0.0;
        } else {
            return null;
        }

        if (zone.ignoreHeight()) {
            minY = level.getMinBuildHeight();
            maxY = level.getMaxBuildHeight();
            if (zone.shape() == QuestZoneDefinition.Shape.SPHERE) {
                centerY = (minY + maxY) * 0.5;
            }
        } else if (zone.shape() == QuestZoneDefinition.Shape.SPHERE && zone.center() != null) {
            double radius = zone.radius();
            minY = centerY - radius;
            maxY = centerY + radius;
        } else if (zone.min() != null && zone.max() != null) {
            minY = zone.min().getY();
            maxY = zone.max().getY() + 1.0;
            centerY = (minY + maxY) * 0.5;
        } else {
            minY = level.getMinBuildHeight();
            maxY = level.getMaxBuildHeight();
            centerY = (minY + maxY) * 0.5;
        }

        JsonObject zoneJson = new JsonObject();
        zoneJson.addProperty("id", zone.id());
        zoneJson.addProperty("dimension", zone.dimension().location().toString());
        zoneJson.addProperty("category", category.name().toLowerCase());
        zoneJson.addProperty("ignoreHeight", zone.ignoreHeight());
        zoneJson.add("min", vec3Json(minX, minY, minZ));
        zoneJson.add("max", vec3Json(maxX, maxY, maxZ));
        zoneJson.add("center", vec3Json(centerX, centerY, centerZ));
        return zoneJson;
    }

    private static QuestZoneContext findActiveZoneContext(Map<String, QuestDefinition> definitions,
                                                          QuestPlayerData data,
                                                          Map<String, QuestZoneDefinition> zones) {
        String activeQuestId = data.activeQuestId();
        QuestZoneContext context = findZoneForQuest(activeQuestId, definitions, data, zones);
        if (context != null) {
            return context;
        }
        for (String questId : data.activeFarmQuests()) {
            context = findZoneForQuest(questId, definitions, data, zones);
            if (context != null) {
                return context;
            }
        }
        return null;
    }

    private static QuestZoneContext findZoneForQuest(String questId,
                                                     Map<String, QuestDefinition> definitions,
                                                     QuestPlayerData data,
                                                     Map<String, QuestZoneDefinition> zones) {
        if (questId == null) {
            return null;
        }
        QuestDefinition definition = definitions.get(questId);
        QuestInstance instance = data.quests().get(questId);
        if (definition == null || instance == null || instance.state() != QuestInstance.State.ACTIVE) {
            return null;
        }
        if (instance.isWaitingHandover()) {
            return null;
        }
        QuestStep step = definition.getStep(instance.currentStepIndex());
        if (step == null) {
            return null;
        }
        String zoneId = findZoneTriggerId(step);
        if (zoneId == null || zoneId.isBlank()) {
            return null;
        }
        QuestZoneDefinition zone = zones.get(zoneId);
        if (zone == null) {
            return null;
        }
        return new QuestZoneContext(zone, definition.resolvedCategory());
    }

    private static String findZoneTriggerId(QuestStep step) {
        if (step == null || step.triggers() == null) {
            return null;
        }
        for (QuestTriggerDef trigger : step.triggers()) {
            if (trigger == null || !"zone_enter".equals(trigger.type())) {
                continue;
            }
            return QuestJsonUtils.getString(trigger.data(), "zoneId", "");
        }
        return null;
    }

    private static JsonObject vec3Json(double x, double y, double z) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", x);
        obj.addProperty("y", y);
        obj.addProperty("z", z);
        return obj;
    }

    private record QuestZoneContext(QuestZoneDefinition zone, QuestCategory category) {}
}
