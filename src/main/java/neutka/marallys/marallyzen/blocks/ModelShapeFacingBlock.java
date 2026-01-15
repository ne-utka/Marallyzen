package neutka.marallys.marallyzen.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facing block that builds its shape from a model json.
 */
public class ModelShapeFacingBlock extends Block {
    private static final Map<String, Map<Direction, VoxelShape>> SHAPE_CACHE = new ConcurrentHashMap<>();
    private static final float INFLATE_PIXELS = 1.0f;
    private static final float INFLATE = INFLATE_PIXELS / 16.0f;

    private final String modelPath;

    public ModelShapeFacingBlock(Properties properties, String modelPath) {
        super(properties);
        this.modelPath = modelPath;
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForState(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForState(state);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShapeForState(state);
    }

    private VoxelShape getShapeForState(BlockState state) {
        Map<Direction, VoxelShape> shapes = SHAPE_CACHE.computeIfAbsent(modelPath, ModelShapeFacingBlock::buildShapes);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return shapes.getOrDefault(facing, Shapes.block());
    }

    private static Map<Direction, VoxelShape> buildShapes(String modelPath) {
        VoxelShape base = buildBaseShape(modelPath);
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, base);
        shapes.put(Direction.EAST, rotateShapeY(base, 90.0));
        shapes.put(Direction.SOUTH, rotateShapeY(base, 180.0));
        shapes.put(Direction.WEST, rotateShapeY(base, -90.0));
        return shapes;
    }

    private static VoxelShape buildBaseShape(String modelPath) {
        InputStream stream = ModelShapeFacingBlock.class.getClassLoader().getResourceAsStream(modelPath);
        if (stream == null) {
            return Shapes.block();
        }

        VoxelShape shape = Shapes.empty();
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray elements = root.getAsJsonArray("elements");
            if (elements == null) {
                return Shapes.block();
            }

            for (JsonElement element : elements) {
                JsonObject obj = element.getAsJsonObject();
                float[] from = readVec3(obj.getAsJsonArray("from"));
                float[] to = readVec3(obj.getAsJsonArray("to"));

                float[][] corners = buildCorners(from, to);
                if (obj.has("rotation")) {
                    JsonObject rot = obj.getAsJsonObject("rotation");
                    float angle = rot.get("angle").getAsFloat();
                    String axis = rot.get("axis").getAsString();
                    float[] origin = readVec3(rot.getAsJsonArray("origin"));
                    for (int i = 0; i < corners.length; i++) {
                        corners[i] = rotatePoint(corners[i], origin, axis, angle);
                    }
                }

                float minX = Float.MAX_VALUE;
                float minY = Float.MAX_VALUE;
                float minZ = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE;
                float maxY = -Float.MAX_VALUE;
                float maxZ = -Float.MAX_VALUE;

                for (float[] c : corners) {
                    minX = Math.min(minX, c[0]);
                    minY = Math.min(minY, c[1]);
                    minZ = Math.min(minZ, c[2]);
                    maxX = Math.max(maxX, c[0]);
                    maxY = Math.max(maxY, c[1]);
                    maxZ = Math.max(maxZ, c[2]);
                }

                minX = (minX / 16.0f - INFLATE) * 16.0f;
                minY = (minY / 16.0f - INFLATE) * 16.0f;
                minZ = (minZ / 16.0f - INFLATE) * 16.0f;
                maxX = (maxX / 16.0f + INFLATE) * 16.0f;
                maxY = (maxY / 16.0f + INFLATE) * 16.0f;
                maxZ = (maxZ / 16.0f + INFLATE) * 16.0f;

                VoxelShape box = Block.box(minX, minY, minZ, maxX, maxY, maxZ);
                shape = Shapes.or(shape, box);
            }
        } catch (Exception ignored) {
            return Shapes.block();
        }

        return shape.isEmpty() ? Shapes.block() : shape;
    }

    private static VoxelShape rotateShapeY(VoxelShape shape, double angleDeg) {
        if (shape.isEmpty()) {
            return shape;
        }
        VoxelShape rotated = Shapes.empty();
        List<AABB> boxes = shape.toAabbs();
        for (AABB box : boxes) {
            AABB rotatedBox = rotateAabbY(box, angleDeg);
            VoxelShape next = Block.box(
                rotatedBox.minX * 16.0, rotatedBox.minY * 16.0, rotatedBox.minZ * 16.0,
                rotatedBox.maxX * 16.0, rotatedBox.maxY * 16.0, rotatedBox.maxZ * 16.0
            );
            rotated = Shapes.or(rotated, next);
        }
        return rotated;
    }

    private static AABB rotateAabbY(AABB box, double angleDeg) {
        double angle = Math.toRadians(angleDeg);
        double cx = 0.5;
        double cz = 0.5;

        double[] xs = {box.minX, box.maxX};
        double[] zs = {box.minZ, box.maxZ};

        double minX = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (double x : xs) {
            for (double z : zs) {
                double dx = x - cx;
                double dz = z - cz;
                double rx = dx * Math.cos(angle) + dz * Math.sin(angle);
                double rz = -dx * Math.sin(angle) + dz * Math.cos(angle);
                double nx = rx + cx;
                double nz = rz + cz;
                minX = Math.min(minX, nx);
                minZ = Math.min(minZ, nz);
                maxX = Math.max(maxX, nx);
                maxZ = Math.max(maxZ, nz);
            }
        }

        return new AABB(minX, box.minY, minZ, maxX, box.maxY, maxZ);
    }

    private static float[] readVec3(JsonArray array) {
        return new float[] { array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat() };
    }

    private static float[][] buildCorners(float[] from, float[] to) {
        return new float[][] {
            {from[0], from[1], from[2]},
            {to[0], from[1], from[2]},
            {to[0], to[1], from[2]},
            {from[0], to[1], from[2]},
            {from[0], from[1], to[2]},
            {to[0], from[1], to[2]},
            {to[0], to[1], to[2]},
            {from[0], to[1], to[2]}
        };
    }

    private static float[] rotatePoint(float[] p, float[] origin, String axis, float angleDeg) {
        double angle = Math.toRadians(angleDeg);
        double x = p[0] - origin[0];
        double y = p[1] - origin[1];
        double z = p[2] - origin[2];

        double rx = x;
        double ry = y;
        double rz = z;

        if ("x".equals(axis)) {
            ry = y * Math.cos(angle) - z * Math.sin(angle);
            rz = y * Math.sin(angle) + z * Math.cos(angle);
        } else if ("y".equals(axis)) {
            rx = x * Math.cos(angle) + z * Math.sin(angle);
            rz = -x * Math.sin(angle) + z * Math.cos(angle);
        } else if ("z".equals(axis)) {
            rx = x * Math.cos(angle) - y * Math.sin(angle);
            ry = x * Math.sin(angle) + y * Math.cos(angle);
        }

        return new float[] {
            (float) (rx + origin[0]),
            (float) (ry + origin[1]),
            (float) (rz + origin[2])
        };
    }

}
