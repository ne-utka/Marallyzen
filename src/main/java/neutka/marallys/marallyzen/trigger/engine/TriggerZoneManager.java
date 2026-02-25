package neutka.marallys.marallyzen.trigger.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import neutka.marallys.marallyzen.trigger.instance.TriggerInstance;
import neutka.marallys.marallyzen.util.PermissionHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerZoneManager {
    public record ProtectedZone(String instanceId, ResourceKey<Level> dimension, BlockPos min, BlockPos max) {
    }

    private final Map<String, ProtectedZone> zones = new ConcurrentHashMap<>();

    public void clear() {
        zones.clear();
    }

    public void registerZone(TriggerInstance instance) {
        if (instance == null) {
            return;
        }
        ResourceKey<Level> dimension = parseDimension(instance.dimensionId());
        BlockPos min = instance.areaMin();
        BlockPos max = instance.areaMax();
        zones.put(instance.id(), new ProtectedZone(instance.id(), dimension, min, max));
    }

    public void removeZone(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return;
        }
        zones.remove(instanceId);
    }

    public Map<String, ProtectedZone> snapshot() {
        return Map.copyOf(zones);
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
