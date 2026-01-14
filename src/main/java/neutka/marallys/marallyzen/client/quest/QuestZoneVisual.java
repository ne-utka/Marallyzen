package neutka.marallys.marallyzen.client.quest;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.quest.QuestCategory;
import neutka.marallys.marallyzen.quest.QuestJsonUtils;

public record QuestZoneVisual(
        String id,
        ResourceKey<Level> dimension,
        AABB bounds,
        Vec3 center,
        QuestCategory category,
        boolean ignoreHeight
) {
    public static QuestZoneVisual fromJson(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        String id = QuestJsonUtils.getString(obj, "id", null);
        String dimensionId = QuestJsonUtils.getString(obj, "dimension", null);
        if (dimensionId == null) {
            return null;
        }
        ResourceKey<Level> dimension = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(dimensionId)
        );
        QuestCategory category = parseCategory(QuestJsonUtils.getString(obj, "category", null));
        boolean ignoreHeight = QuestJsonUtils.getBoolean(obj, "ignoreHeight", false);

        JsonObject minObj = obj.getAsJsonObject("min");
        JsonObject maxObj = obj.getAsJsonObject("max");
        JsonObject centerObj = obj.getAsJsonObject("center");
        if (minObj == null || maxObj == null || centerObj == null) {
            return null;
        }
        double minX = QuestJsonUtils.getDouble(minObj, "x", 0.0);
        double minY = QuestJsonUtils.getDouble(minObj, "y", 0.0);
        double minZ = QuestJsonUtils.getDouble(minObj, "z", 0.0);
        double maxX = QuestJsonUtils.getDouble(maxObj, "x", 0.0);
        double maxY = QuestJsonUtils.getDouble(maxObj, "y", 0.0);
        double maxZ = QuestJsonUtils.getDouble(maxObj, "z", 0.0);
        double centerX = QuestJsonUtils.getDouble(centerObj, "x", 0.0);
        double centerY = QuestJsonUtils.getDouble(centerObj, "y", 0.0);
        double centerZ = QuestJsonUtils.getDouble(centerObj, "z", 0.0);
        AABB bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        Vec3 center = new Vec3(centerX, centerY, centerZ);
        return new QuestZoneVisual(id, dimension, bounds, center, category, ignoreHeight);
    }

    public double distanceTo(Vec3 pos) {
        if (pos == null) {
            return Double.MAX_VALUE;
        }
        double dx = pos.x - center.x;
        double dz = pos.z - center.z;
        if (ignoreHeight) {
            return Math.sqrt(dx * dx + dz * dz);
        }
        double dy = pos.y - center.y;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public boolean contains(AABB playerBox) {
        if (playerBox == null) {
            return false;
        }
        if (ignoreHeight) {
            return playerBox.minX >= bounds.minX
                    && playerBox.maxX <= bounds.maxX
                    && playerBox.minZ >= bounds.minZ
                    && playerBox.maxZ <= bounds.maxZ;
        }
        return playerBox.minX >= bounds.minX
                && playerBox.maxX <= bounds.maxX
                && playerBox.minY >= bounds.minY
                && playerBox.maxY <= bounds.maxY
                && playerBox.minZ >= bounds.minZ
                && playerBox.maxZ <= bounds.maxZ;
    }

    private static QuestCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return QuestCategory.SIDE;
        }
        try {
            return QuestCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return QuestCategory.SIDE;
        }
    }
}
