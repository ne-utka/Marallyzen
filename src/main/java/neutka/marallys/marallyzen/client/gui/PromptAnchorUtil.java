package neutka.marallys.marallyzen.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class PromptAnchorUtil {
    private PromptAnchorUtil() {}

    public static double blockTopY(Level level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return 0.0;
        }
        var shape = state.getShape(level, pos);
        if (shape.isEmpty()) {
            return pos.getY() + 1.0;
        }
        AABB bounds = shape.bounds();
        double maxY = bounds.maxY > 0.0 ? bounds.maxY : 1.0;
        return pos.getY() + maxY;
    }

    public static double entityTopY(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return 0.0;
        }
        return entity.getBoundingBox().maxY;
    }

    public static double pxToWorld(float px, float scale) {
        return px * scale;
    }

    private static double getModelMaxY(BlockState state) {
        return 0.0;
    }
}
