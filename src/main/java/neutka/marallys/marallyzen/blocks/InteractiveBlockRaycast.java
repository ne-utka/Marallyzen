package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class InteractiveBlockRaycast {
    private InteractiveBlockRaycast() {}

    public static BlockHitResult raycast(Level level, Vec3 start, Vec3 end) {
        if (level == null) {
            return null;
        }

        AABB scanBox = new AABB(start, end).inflate(1.0);
        BlockPos min = BlockPos.containing(scanBox.minX, scanBox.minY, scanBox.minZ);
        BlockPos max = BlockPos.containing(scanBox.maxX, scanBox.maxY, scanBox.maxZ);

        BlockHitResult closest = null;
        double closestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (InteractiveBlockTargeting.getType(state) == InteractiveBlockTargeting.Type.NONE) {
                continue;
            }

            VoxelShape shape = state.getInteractionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }

            BlockHitResult hit = shape.clip(start, end, pos);
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                continue;
            }

            double dist = hit.getLocation().distanceTo(start);
            if (dist < closestDist) {
                closestDist = dist;
                closest = hit;
            }
        }

        return closest;
    }
}
