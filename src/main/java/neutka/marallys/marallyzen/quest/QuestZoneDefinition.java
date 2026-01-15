package neutka.marallys.marallyzen.quest;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class QuestZoneDefinition {
    public enum Shape {
        AABB,
        SPHERE
    }

    private final String id;
    private final Shape shape;
    private final ResourceKey<Level> dimension;
    private final BlockPos min;
    private final BlockPos max;
    private final BlockPos center;
    private final double radius;
    private final boolean ignoreHeight;

    private QuestZoneDefinition(
            String id,
            Shape shape,
            ResourceKey<Level> dimension,
            BlockPos min,
            BlockPos max,
            BlockPos center,
            double radius,
            boolean ignoreHeight
    ) {
        this.id = id;
        this.shape = shape;
        this.dimension = dimension;
        this.min = min;
        this.max = max;
        this.center = center;
        this.radius = radius;
        this.ignoreHeight = ignoreHeight;
    }

    public String id() {
        return id;
    }

    public Shape shape() {
        return shape;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public BlockPos min() {
        return min;
    }

    public BlockPos max() {
        return max;
    }

    public BlockPos center() {
        return center;
    }

    public double radius() {
        return radius;
    }

    public boolean ignoreHeight() {
        return ignoreHeight;
    }

    public boolean contains(BlockPos pos) {
        if (shape == Shape.SPHERE) {
            double dx = pos.getX() + 0.5 - center.getX() - 0.5;
            double dz = pos.getZ() + 0.5 - center.getZ() - 0.5;
            if (ignoreHeight) {
                return (dx * dx + dz * dz) <= radius * radius;
            }
            double dy = pos.getY() + 0.5 - center.getY() - 0.5;
            return (dx * dx + dy * dy + dz * dz) <= radius * radius;
        }
        if (ignoreHeight) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public static QuestZoneDefinition fromJson(JsonObject obj) {
        if (obj == null || !obj.has("id")) {
            return null;
        }
        String id = QuestJsonUtils.getString(obj, "id", "");
        String shapeStr = QuestJsonUtils.getString(obj, "shape", "aabb");
        Shape shape = "sphere".equalsIgnoreCase(shapeStr) ? Shape.SPHERE : Shape.AABB;
        String dimensionStr = QuestJsonUtils.getString(obj, "dimension", "minecraft:overworld");
        ResourceKey<Level> dimension = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.parse(dimensionStr)
        );

        if (shape == Shape.SPHERE) {
            JsonObject centerObj = obj.getAsJsonObject("center");
            int cx = QuestJsonUtils.getInt(centerObj, "x", 0);
            int cy = QuestJsonUtils.getInt(centerObj, "y", 0);
            int cz = QuestJsonUtils.getInt(centerObj, "z", 0);
            boolean ignoreHeight = centerObj == null || !centerObj.has("y");
            double radius = QuestJsonUtils.getDouble(obj, "radius", 1.0);
            return new QuestZoneDefinition(id, shape, dimension, null, null, new BlockPos(cx, cy, cz), radius, ignoreHeight);
        }

        JsonObject minObj = obj.getAsJsonObject("min");
        JsonObject maxObj = obj.getAsJsonObject("max");
        int minX = QuestJsonUtils.getInt(minObj, "x", 0);
        int minY = QuestJsonUtils.getInt(minObj, "y", 0);
        int minZ = QuestJsonUtils.getInt(minObj, "z", 0);
        int maxX = QuestJsonUtils.getInt(maxObj, "x", 0);
        int maxY = QuestJsonUtils.getInt(maxObj, "y", 0);
        int maxZ = QuestJsonUtils.getInt(maxObj, "z", 0);
        boolean ignoreHeight = minObj == null || maxObj == null || !minObj.has("y") || !maxObj.has("y");
        return new QuestZoneDefinition(id, shape, dimension,
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ),
                null,
                0.0,
                ignoreHeight
        );
    }
}
