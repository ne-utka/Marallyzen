package neutka.marallys.marallyzen.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class PromptAnchorUtil {
    private PromptAnchorUtil() {}

    public static double blockTopY(Level level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return 0.0;
        }
        double modelMaxY = getModelMaxY(state);
        if (modelMaxY > 0.0) {
            return pos.getY() + modelMaxY;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getBlockRenderer() == null) {
            return 0.0;
        }
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        if (model == null) {
            return 0.0;
        }
        RandomSource random = RandomSource.create(0);
        double maxY = 0.0;
        boolean found = false;

        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : model.getQuads(state, direction, random)) {
                maxY = Math.max(maxY, quadMaxY(quad));
                found = true;
            }
        }
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            maxY = Math.max(maxY, quadMaxY(quad));
            found = true;
        }
        return found ? maxY : 0.0;
    }

    private static double quadMaxY(BakedQuad quad) {
        if (quad == null) {
            return 0.0;
        }
        int[] data = quad.getVertices();
        if (data == null || data.length < 8) {
            return 0.0;
        }
        double maxY = 0.0;
        int stride = 8;
        for (int i = 0; i + 2 < data.length; i += stride) {
            float y = Float.intBitsToFloat(data[i + 1]);
            if (y > maxY) {
                maxY = y;
            }
        }
        return maxY;
    }
}
