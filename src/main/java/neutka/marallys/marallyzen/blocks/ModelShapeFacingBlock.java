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
    private static final float INFLATE_PIXELS = 0.0f;
    private static final float INFLATE = INFLATE_PIXELS / 16.0f;
    private static final float THIN_FACE_EPSILON = 0.001f;

    private final String modelPath;
    private final boolean rotateAroundModelCenter;
    private final boolean preventReplacement;
    private final int rotationOffsetDegrees;
    private final boolean recenterShape;
    private final double offsetXBlocks;
    private final double offsetZBlocks;
    private final boolean ignoreElementRotations;
    private final boolean ignoreXRotations;
    private final boolean useBoundsOnly;
    private final double extraEastWestOffsetX;
    private final double extraEastWestOffsetZ;

    public ModelShapeFacingBlock(Properties properties, String modelPath) {
        this(properties, modelPath, false, false, 0, false, 0.0, 0.0, false, false, false, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter) {
        this(properties, modelPath, rotateAroundModelCenter, false, 0, false, 0.0, 0.0, false, false, false, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter, boolean preventReplacement) {
        this(properties, modelPath, rotateAroundModelCenter, preventReplacement, 0, false, 0.0, 0.0, false, false, false, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter, boolean preventReplacement, int rotationOffsetDegrees) {
        this(properties, modelPath, rotateAroundModelCenter, preventReplacement, rotationOffsetDegrees, false, 0.0, 0.0, false, false, false, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter, boolean preventReplacement, int rotationOffsetDegrees, boolean recenterShape) {
        this(properties, modelPath, rotateAroundModelCenter, preventReplacement, rotationOffsetDegrees, recenterShape, 0.0, 0.0, false, false, false, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter, boolean preventReplacement, int rotationOffsetDegrees, boolean recenterShape, double offsetXBlocks, double offsetZBlocks) {
        this(properties, modelPath, rotateAroundModelCenter, preventReplacement, rotationOffsetDegrees, recenterShape, offsetXBlocks, offsetZBlocks, false, false, false, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter, boolean preventReplacement, int rotationOffsetDegrees, boolean recenterShape, double offsetXBlocks, double offsetZBlocks, boolean ignoreElementRotations) {
        this(properties, modelPath, rotateAroundModelCenter, preventReplacement, rotationOffsetDegrees, recenterShape, offsetXBlocks, offsetZBlocks, ignoreElementRotations, false, false, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter, boolean preventReplacement, int rotationOffsetDegrees, boolean recenterShape, double offsetXBlocks, double offsetZBlocks, boolean ignoreElementRotations, boolean ignoreXRotations) {
        this(properties, modelPath, rotateAroundModelCenter, preventReplacement, rotationOffsetDegrees, recenterShape, offsetXBlocks, offsetZBlocks, ignoreElementRotations, ignoreXRotations, false, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter, boolean preventReplacement, int rotationOffsetDegrees, boolean recenterShape, double offsetXBlocks, double offsetZBlocks, boolean ignoreElementRotations, boolean ignoreXRotations, boolean useBoundsOnly) {
        this(properties, modelPath, rotateAroundModelCenter, preventReplacement, rotationOffsetDegrees, recenterShape, offsetXBlocks, offsetZBlocks, ignoreElementRotations, ignoreXRotations, useBoundsOnly, 0.0, 0.0);
    }

    public ModelShapeFacingBlock(Properties properties, String modelPath, boolean rotateAroundModelCenter, boolean preventReplacement, int rotationOffsetDegrees, boolean recenterShape, double offsetXBlocks, double offsetZBlocks, boolean ignoreElementRotations, boolean ignoreXRotations, boolean useBoundsOnly, double extraEastWestOffsetX, double extraEastWestOffsetZ) {
        super(properties);
        this.modelPath = modelPath;
        this.rotateAroundModelCenter = rotateAroundModelCenter;
        this.preventReplacement = preventReplacement;
        this.rotationOffsetDegrees = rotationOffsetDegrees;
        this.recenterShape = recenterShape;
        this.offsetXBlocks = offsetXBlocks;
        this.offsetZBlocks = offsetZBlocks;
        this.ignoreElementRotations = ignoreElementRotations;
        this.ignoreXRotations = ignoreXRotations;
        this.useBoundsOnly = useBoundsOnly;
        this.extraEastWestOffsetX = extraEastWestOffsetX;
        this.extraEastWestOffsetZ = extraEastWestOffsetZ;
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

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (preventReplacement) {
            return false;
        }
        return super.canBeReplaced(state, context);
    }

    private VoxelShape getShapeForState(BlockState state) {
        String cacheKey = rotateAroundModelCenter ? modelPath + "|center|rot=" + rotationOffsetDegrees : modelPath + "|rot=" + rotationOffsetDegrees;
        if (recenterShape) {
            cacheKey += "|recenter";
        }
        if (offsetXBlocks != 0.0 || offsetZBlocks != 0.0) {
            cacheKey += "|offset=" + offsetXBlocks + "," + offsetZBlocks;
        }
        if (ignoreElementRotations) {
            cacheKey += "|ignoreRot";
        }
        if (ignoreXRotations) {
            cacheKey += "|ignoreXRot";
        }
        if (useBoundsOnly) {
            cacheKey += "|boundsOnly";
        }
        if (extraEastWestOffsetX != 0.0 || extraEastWestOffsetZ != 0.0) {
            cacheKey += "|ew=" + extraEastWestOffsetX + "," + extraEastWestOffsetZ;
        }
        Map<Direction, VoxelShape> shapes = SHAPE_CACHE.computeIfAbsent(cacheKey,
            key -> buildShapes(modelPath, rotateAroundModelCenter, rotationOffsetDegrees, recenterShape, offsetXBlocks, offsetZBlocks, ignoreElementRotations, ignoreXRotations, useBoundsOnly, extraEastWestOffsetX, extraEastWestOffsetZ));
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return shapes.getOrDefault(facing, Shapes.block());
    }

    private static Map<Direction, VoxelShape> buildShapes(String modelPath, boolean rotateAroundModelCenter, int rotationOffsetDegrees, boolean recenterShape, double offsetXBlocks, double offsetZBlocks, boolean ignoreElementRotations, boolean ignoreXRotations, boolean useBoundsOnly, double extraEastWestOffsetX, double extraEastWestOffsetZ) {
        VoxelShape base = buildBaseShape(modelPath, ignoreElementRotations, ignoreXRotations);
        if (useBoundsOnly && !base.isEmpty()) {
            double[] bounds = getShapeBounds(base);
            base = Block.box(
                bounds[0] * 16.0, bounds[1] * 16.0, bounds[2] * 16.0,
                bounds[3] * 16.0, bounds[4] * 16.0, bounds[5] * 16.0
            );
        }
        double centerX = 0.5;
        double centerZ = 0.5;
        if (rotateAroundModelCenter) {
            double[] bounds = getShapeBounds(base);
            centerX = (bounds[0] + bounds[3]) * 0.5;
            centerZ = (bounds[2] + bounds[5]) * 0.5;
        }
        if (recenterShape) {
            double[] bounds = getShapeBounds(base);
            double shapeCenterX = (bounds[0] + bounds[3]) * 0.5;
            double shapeCenterZ = (bounds[2] + bounds[5]) * 0.5;
            base = offsetShape(base, 0.5 - shapeCenterX, 0.0, 0.5 - shapeCenterZ);
            centerX = 0.5;
            centerZ = 0.5;
        }
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, rotateAndOffset(base, rotationOffsetDegrees, centerX, centerZ, offsetXBlocks, offsetZBlocks));
        shapes.put(Direction.EAST, rotateAndOffset(base, 90.0 + rotationOffsetDegrees, centerX, centerZ, offsetXBlocks + extraEastWestOffsetX, offsetZBlocks + extraEastWestOffsetZ));
        shapes.put(Direction.SOUTH, rotateAndOffset(base, 180.0 + rotationOffsetDegrees, centerX, centerZ, offsetXBlocks, offsetZBlocks));
        shapes.put(Direction.WEST, rotateAndOffset(base, -90.0 + rotationOffsetDegrees, centerX, centerZ, offsetXBlocks + extraEastWestOffsetX, offsetZBlocks + extraEastWestOffsetZ));
        return shapes;
    }

    private static VoxelShape buildBaseShape(String modelPath, boolean ignoreElementRotations, boolean ignoreXRotations) {
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
                if (!ignoreElementRotations && obj.has("rotation")) {
                    JsonObject rot = obj.getAsJsonObject("rotation");
                    float angle = rot.get("angle").getAsFloat();
                    String axis = rot.get("axis").getAsString();
                    if (ignoreXRotations && "x".equals(axis)) {
                        axis = null;
                    }
                    if (axis == null) {
                        continue;
                    }
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

                if (minX == maxX) {
                    minX -= THIN_FACE_EPSILON;
                    maxX += THIN_FACE_EPSILON;
                }
                if (minY == maxY) {
                    minY -= THIN_FACE_EPSILON;
                    maxY += THIN_FACE_EPSILON;
                }
                if (minZ == maxZ) {
                    minZ -= THIN_FACE_EPSILON;
                    maxZ += THIN_FACE_EPSILON;
                }

                VoxelShape box = Block.box(minX, minY, minZ, maxX, maxY, maxZ);
                shape = Shapes.or(shape, box);
            }
        } catch (Exception ignored) {
            return Shapes.block();
        }

        return shape.isEmpty() ? Shapes.block() : shape;
    }

    private static VoxelShape rotateShapeY(VoxelShape shape, double angleDeg, double centerX, double centerZ) {
        if (shape.isEmpty()) {
            return shape;
        }
        VoxelShape rotated = Shapes.empty();
        List<AABB> boxes = shape.toAabbs();
        for (AABB box : boxes) {
            AABB rotatedBox = rotateAabbY(box, angleDeg, centerX, centerZ);
            VoxelShape next = Block.box(
                rotatedBox.minX * 16.0, rotatedBox.minY * 16.0, rotatedBox.minZ * 16.0,
                rotatedBox.maxX * 16.0, rotatedBox.maxY * 16.0, rotatedBox.maxZ * 16.0
            );
            rotated = Shapes.or(rotated, next);
        }
        return rotated;
    }

    private static VoxelShape rotateAndOffset(VoxelShape base, double angleDeg, double centerX, double centerZ, double offsetX, double offsetZ) {
        VoxelShape rotated = rotateShapeY(base, angleDeg, centerX, centerZ);
        if (offsetX == 0.0 && offsetZ == 0.0) {
            return rotated;
        }
        double angle = Math.toRadians(angleDeg);
        double dx = offsetX * Math.cos(angle) + offsetZ * Math.sin(angle);
        double dz = -offsetX * Math.sin(angle) + offsetZ * Math.cos(angle);
        return offsetShape(rotated, dx, 0.0, dz);
    }

    private static VoxelShape offsetShape(VoxelShape shape, double dx, double dy, double dz) {
        if (shape.isEmpty()) {
            return shape;
        }
        VoxelShape shifted = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            AABB moved = box.move(dx, dy, dz);
            VoxelShape next = Block.box(
                moved.minX * 16.0, moved.minY * 16.0, moved.minZ * 16.0,
                moved.maxX * 16.0, moved.maxY * 16.0, moved.maxZ * 16.0
            );
            shifted = Shapes.or(shifted, next);
        }
        return shifted;
    }

    private static AABB rotateAabbY(AABB box, double angleDeg, double centerX, double centerZ) {
        double angle = Math.toRadians(angleDeg);
        double cx = centerX;
        double cz = centerZ;

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

    private static double[] getShapeBounds(VoxelShape shape) {
        List<AABB> boxes = shape.toAabbs();
        if (boxes.isEmpty()) {
            return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (AABB box : boxes) {
            minX = Math.min(minX, box.minX);
            minY = Math.min(minY, box.minY);
            minZ = Math.min(minZ, box.minZ);
            maxX = Math.max(maxX, box.maxX);
            maxY = Math.max(maxY, box.maxY);
            maxZ = Math.max(maxZ, box.maxZ);
        }
        return new double[] {minX, minY, minZ, maxX, maxY, maxZ};
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
