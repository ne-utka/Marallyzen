package neutka.marallys.marallyzen.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.util.PermissionHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime protected-zone registry and enforcement.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public final class GoalZoneManager {
    public record ProtectedZone(String goalId, ResourceKey<Level> dimension, BlockPos center, int radius, boolean protect) {
    }

    private static final GoalZoneManager INSTANCE = new GoalZoneManager();

    private final Map<String, ProtectedZone> zonesByGoal = new ConcurrentHashMap<>();

    private GoalZoneManager() {
    }

    public static GoalZoneManager getInstance() {
        return INSTANCE;
    }

    public void clear() {
        zonesByGoal.clear();
    }

    public void registerZone(String goalId, String dimensionId, BlockPos center, int radius, boolean protect) {
        if (goalId == null || goalId.isBlank() || center == null) {
            return;
        }
        ResourceKey<Level> dimension = parseDimension(dimensionId);
        zonesByGoal.put(goalId, new ProtectedZone(goalId, dimension, center.immutable(), Math.max(1, radius), protect));
    }

    public void removeZone(String goalId) {
        if (goalId == null || goalId.isBlank()) {
            return;
        }
        zonesByGoal.remove(goalId);
    }

    public Map<String, ProtectedZone> zonesSnapshot() {
        return Map.copyOf(zonesByGoal);
    }

    public boolean isInsideGoalZone(String goalId, ServerLevel level, Vec3 pos) {
        if (goalId == null || goalId.isBlank() || level == null || pos == null) {
            return false;
        }
        ProtectedZone zone = zonesByGoal.get(goalId);
        return zone != null && contains(zone, level, pos);
    }

    public boolean isProtectedPosition(ServerLevel level, BlockPos pos, Entity actor) {
        if (level == null || pos == null) {
            return false;
        }
        if (canBypass(actor)) {
            return false;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        for (ProtectedZone zone : zonesByGoal.values()) {
            if (!zone.protect()) {
                continue;
            }
            if (contains(zone, level, center)) {
                return true;
            }
        }
        return false;
    }

    public boolean isProtectedPosition(ServerLevel level, Vec3 pos, Entity actor) {
        if (level == null || pos == null) {
            return false;
        }
        if (canBypass(actor)) {
            return false;
        }
        for (ProtectedZone zone : zonesByGoal.values()) {
            if (!zone.protect()) {
                continue;
            }
            if (contains(zone, level, pos)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceKey<Level> parseDimension(String id) {
        try {
            Identifier identifier = Identifier.parse(id);
            return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, identifier);
        } catch (Exception ignored) {
            return Level.OVERWORLD;
        }
    }

    private static boolean contains(ProtectedZone zone, ServerLevel level, Vec3 pos) {
        if (zone == null || level == null || pos == null) {
            return false;
        }
        if (!zone.dimension().equals(level.dimension())) {
            return false;
        }
        double centerX = zone.center().getX() + 0.5D;
        double centerZ = zone.center().getZ() + 0.5D;
        double dx = Math.abs(pos.x - centerX);
        double dz = Math.abs(pos.z - centerZ);
        return dx < zone.radius() && dz < zone.radius();
    }

    private static boolean canBypass(Entity actor) {
        return actor instanceof ServerPlayer serverPlayer && PermissionHelper.isOp(serverPlayer);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (INSTANCE.isProtectedPosition(level, event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (INSTANCE.isProtectedPosition(level, event.getPos(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (INSTANCE.isProtectedPosition(level, event.getPos(), null)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity source = event.getExplosion().getDirectSourceEntity();
        event.getAffectedBlocks().removeIf(pos -> INSTANCE.isProtectedPosition(level, pos, source));
    }

    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pistonPos = event.getPos();
        if (INSTANCE.isProtectedPosition(level, pistonPos, null)) {
            event.setCanceled(true);
            return;
        }
        BlockPos movedPos = pistonPos.relative(event.getDirection());
        if (INSTANCE.isProtectedPosition(level, movedPos, null)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobGrief(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (INSTANCE.isProtectedPosition(level, entity.position(), entity)) {
            event.setCanGrief(false);
        }
    }
}
