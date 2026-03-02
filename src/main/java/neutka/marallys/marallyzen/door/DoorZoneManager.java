package neutka.marallys.marallyzen.door;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import neutka.marallys.marallyzen.util.PermissionHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DoorZoneManager {
    public record ProtectedZone(String doorId, ResourceKey<Level> dimension, BlockPos min, BlockPos max) {
    }

    private final Map<String, ProtectedZone> zones = new ConcurrentHashMap<>();

    public void clear() {
        zones.clear();
    }

    public void registerZone(String doorId, String dimensionId, BlockPos min, BlockPos max) {
        if (doorId == null || doorId.isBlank() || min == null || max == null) {
            return;
        }
        ResourceKey<Level> dimension = parseDimension(dimensionId);
        zones.put(doorId, new ProtectedZone(doorId, dimension, min.immutable(), max.immutable()));
    }

    public void removeZone(String doorId) {
        if (doorId == null || doorId.isBlank()) {
            return;
        }
        zones.remove(doorId);
    }

    public boolean isProtectedPosition(ServerLevel level, BlockPos pos, Entity actor) {
        if (level == null || pos == null) {
            return false;
        }
        if (canBypass(actor)) {
            return false;
        }
        for (ProtectedZone zone : zones.values()) {
            if (contains(zone, level, pos)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceKey<Level> parseDimension(String id) {
        try {
            return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(id));
        } catch (Exception e) {
            return Level.OVERWORLD;
        }
    }

    private static boolean contains(ProtectedZone zone, ServerLevel level, BlockPos pos) {
        if (zone == null || level == null || pos == null) {
            return false;
        }
        if (!zone.dimension().equals(level.dimension())) {
            return false;
        }
        return pos.getX() >= zone.min().getX()
                && pos.getY() >= zone.min().getY()
                && pos.getZ() >= zone.min().getZ()
                && pos.getX() <= zone.max().getX()
                && pos.getY() <= zone.max().getY()
                && pos.getZ() <= zone.max().getZ();
    }

    private static boolean canBypass(Entity actor) {
        return actor instanceof ServerPlayer player && PermissionHelper.isOp(player);
    }
}
