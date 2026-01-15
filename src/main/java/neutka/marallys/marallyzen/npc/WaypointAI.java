package neutka.marallys.marallyzen.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * AI system for NPC waypoint navigation.
 * Manages movement between waypoints with configurable speed and wait times.
 */
public class WaypointAI {
    private final Entity entity;
    private final List<NpcData.Waypoint> waypoints;
    private final Level level;

    private int currentWaypointIndex = 0;
    private int waitTicksRemaining = 0;
    private boolean isMoving = false;
    private boolean loop = true; // Loop waypoints infinitely
    private BlockPos currentTarget;
    private Vec3 lastPos;
    private int stuckTicks;
    private static final float TURN_SPEED_DEG = 12.0f;
    private static final float MOVE_START_ANGLE_DEG = 25.0f;

    public WaypointAI(Entity entity, List<NpcData.Waypoint> waypoints, Level level) {
        this.entity = entity;
        this.waypoints = waypoints;
        this.level = level;
    }

    /**
     * Update AI logic every tick.
     */
    public void tick() {
        if (waypoints.isEmpty()) {
            return; // No waypoints to follow
        }

        if (waitTicksRemaining > 0) {
            waitTicksRemaining--;
            return; // Still waiting at current waypoint
        }

        if (!isMoving) {
            startMovingToNextWaypoint();
        } else {
            checkIfReachedWaypoint();
        }
    }

    /**
     * Start moving to the next waypoint.
     */
    private void startMovingToNextWaypoint() {
        if (waypoints.isEmpty()) return;

        NpcData.Waypoint currentWaypoint = waypoints.get(currentWaypointIndex);
        BlockPos targetPos = resolveGroundTarget(currentWaypoint.getPos());
        currentTarget = targetPos;
        double groundY = computeGroundY(targetPos);

        // Set movement speed (only for LivingEntity)
        if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            livingEntity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .setBaseValue(currentWaypoint.getSpeed());
        }

        // Navigate to waypoint (only for PathfinderMob)
        if (entity instanceof net.minecraft.world.entity.PathfinderMob pathfinderMob) {
            net.minecraft.world.entity.ai.navigation.PathNavigation navigator = pathfinderMob.getNavigation();
            navigator.moveTo(targetPos.getX() + 0.5, groundY, targetPos.getZ() + 0.5, 1.0);
        }

        isMoving = true;
    }

    /**
     * Check if the entity has reached the current waypoint.
     */
    private void checkIfReachedWaypoint() {
        if (waypoints.isEmpty()) return;

        NpcData.Waypoint currentWaypoint = waypoints.get(currentWaypointIndex);
        BlockPos targetPos = currentTarget != null ? currentTarget : resolveGroundTarget(currentWaypoint.getPos());
        currentTarget = targetPos;
        double groundY = computeGroundY(targetPos);
        if (!(entity instanceof net.minecraft.world.entity.PathfinderMob)) {
            boolean blocked = moveDirectly(targetPos, groundY, currentWaypoint.getSpeed());
            if (blocked) {
                reachWaypoint();
                return;
            }
        }

        // Check if entity is close enough to the waypoint
        Vec3 entityPos = entity.position();
        double dx = (targetPos.getX() + 0.5) - entityPos.x;
        double dz = (targetPos.getZ() + 0.5) - entityPos.z;
        double distanceSq = (entity instanceof net.minecraft.world.entity.PathfinderMob)
                ? entityPos.distanceToSqr(targetPos.getX() + 0.5, groundY, targetPos.getZ() + 0.5)
                : (dx * dx + dz * dz);

        if (distanceSq < 1.0) { // Within 1 block
            reachWaypoint();
        }
    }

    /**
     * Called when entity reaches a waypoint.
     */
    private void reachWaypoint() {
        isMoving = false;
        currentTarget = null;
        stuckTicks = 0;
        lastPos = null;

        if (waypoints.isEmpty()) return;

        NpcData.Waypoint currentWaypoint = waypoints.get(currentWaypointIndex);

        // Stop navigation
        if (entity instanceof net.minecraft.world.entity.PathfinderMob pathfinderMob) {
            pathfinderMob.getNavigation().stop();
        }

        // Set wait time
        waitTicksRemaining = currentWaypoint.getWaitTicks();

        // Move to next waypoint
        currentWaypointIndex++;
        if (currentWaypointIndex >= waypoints.size()) {
            if (loop) {
                currentWaypointIndex = 0; // Loop back to start
            } else {
                currentWaypointIndex = waypoints.size() - 1; // Stay at last waypoint
            }
        }
    }

    /**
     * Set whether waypoints should loop.
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    /**
     * Get current waypoint index.
     */
    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    /**
     * Force move to a specific waypoint index.
     */
    public void moveToWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            currentWaypointIndex = index;
            waitTicksRemaining = 0;
            isMoving = false;
        }
    }

    /**
     * Check if entity is currently moving.
     */
    public boolean isMoving() {
        return isMoving;
    }

    /**
     * Check if entity is waiting at a waypoint.
     */
    public boolean isWaiting() {
        return waitTicksRemaining > 0;
    }

    /**
     * Get remaining wait ticks.
     */
    public int getWaitTicksRemaining() {
        return waitTicksRemaining;
    }

    private boolean moveDirectly(BlockPos targetPos, double groundY, double speed) {
        Vec3 entityPos = entity.position();
        double tx = targetPos.getX() + 0.5;
        double tz = targetPos.getZ() + 0.5;
        double ty = groundY;
        double dx = tx - entityPos.x;
        double dz = tz - entityPos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 0.0001) {
            return false;
        }
        float targetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float currentYaw = entity.getYRot();
        float deltaYaw = Mth.wrapDegrees(targetYaw - currentYaw);
        float stepYaw = Math.min(Math.abs(deltaYaw), TURN_SPEED_DEG) * Math.signum(deltaYaw);
        float newYaw = Mth.wrapDegrees(currentYaw + stepYaw);
        applyYaw(newYaw);

        float remainingYaw = Math.abs(Mth.wrapDegrees(targetYaw - newYaw));
        if (remainingYaw > MOVE_START_ANGLE_DEG) {
            stuckTicks = 0;
            return false;
        }

        double step = Math.min(speed * 0.35, distance);
        double yawRad = Math.toRadians(newYaw);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double dy = Mth.clamp(ty - entityPos.y, -1.5, 1.0);
        Vec3 move = new Vec3(fx * step, dy, fz * step);
        lastPos = entityPos;
        entity.move(MoverType.SELF, move);

        Vec3 newPos = entity.position();
        double desiredY = ty;
        if (Math.abs(desiredY - newPos.y) <= 1.5 && Math.abs(desiredY - newPos.y) > 0.001) {
            entity.setPos(newPos.x, desiredY, newPos.z);
            newPos = entity.position();
        }
        double moved = newPos.distanceToSqr(entityPos);
        if (moved < 0.0004 && distance > 0.6) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        return stuckTicks > 10;
    }

    private BlockPos resolveGroundTarget(BlockPos targetPos) {
        if (level == null || targetPos == null) {
            return targetPos;
        }
        if (!level.hasChunkAt(targetPos)) {
            return targetPos;
        }
        int baseY = targetPos.getY();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos(targetPos.getX(), baseY + 2, targetPos.getZ());
        for (int y = baseY + 2; y >= baseY - 8; y--) {
            scan.setY(y);
            if (!level.hasChunkAt(scan)) {
                return targetPos;
            }
            if (level.getBlockState(scan).isCollisionShapeFullBlock(level, scan)) {
                BlockPos above = scan.above();
                if (level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                    return above;
                }
            }
        }
        return targetPos;
    }

    private double computeGroundY(BlockPos targetPos) {
        if (level == null || targetPos == null) {
            return targetPos != null ? targetPos.getY() : 0.0;
        }
        if (!level.hasChunkAt(targetPos)) {
            return targetPos.getY();
        }
        int baseY = targetPos.getY();
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos(targetPos.getX(), baseY + 2, targetPos.getZ());
        for (int y = baseY + 2; y >= baseY - 8; y--) {
            scan.setY(y);
            if (!level.hasChunkAt(scan)) {
                return targetPos.getY();
            }
            var shape = level.getBlockState(scan).getCollisionShape(level, scan);
            if (!shape.isEmpty()) {
                double maxY = shape.max(Direction.Axis.Y);
                return scan.getY() + maxY;
            }
        }
        return targetPos.getY();
    }

    private void applyYaw(float newYaw) {
        entity.setYRot(newYaw);
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            living.yBodyRot = newYaw;
            living.yBodyRotO = newYaw;
            if (shouldSyncHeadToBody()) {
                living.yHeadRot = newYaw;
                living.yHeadRotO = newYaw;
            }
        }
    }

    private boolean shouldSyncHeadToBody() {
        NpcRegistry registry = NpcClickHandler.getRegistry();
        String npcId = registry.getNpcId(entity);
        if (npcId == null) {
            return true;
        }
        NpcData data = registry.getNpcData(npcId);
        return data == null || !Boolean.TRUE.equals(data.getLookAtPlayers());
    }
}
