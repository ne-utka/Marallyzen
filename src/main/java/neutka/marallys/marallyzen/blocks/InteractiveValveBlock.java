package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InteractiveValveBlock extends LeverBlock implements EntityBlock {
    private static final double MODEL_MIN_Y = 0.8;
    private static final double MODEL_MAX_Y = 16.8;
    private static final double MODEL_FACE_MIN = 0.5;
    private static final double MODEL_FACE_MAX = 4.0;
    private static final double MODEL_FACE_MIN_FAR = 12.0;
    private static final double MODEL_FACE_MAX_FAR = 15.5;
    private static final VoxelShape INTERACTION_SHAPE_SOUTH = Block.box(
        0.0, MODEL_MIN_Y, MODEL_FACE_MIN, 16.0, MODEL_MAX_Y, MODEL_FACE_MAX
    );
    private static final VoxelShape INTERACTION_SHAPE_NORTH = Block.box(
        0.0, MODEL_MIN_Y, MODEL_FACE_MIN_FAR, 16.0, MODEL_MAX_Y, MODEL_FACE_MAX_FAR
    );
    private static final VoxelShape INTERACTION_SHAPE_EAST = Block.box(
        MODEL_FACE_MIN, MODEL_MIN_Y, 0.0, MODEL_FACE_MAX, MODEL_MAX_Y, 16.0
    );
    private static final VoxelShape INTERACTION_SHAPE_WEST = Block.box(
        MODEL_FACE_MIN_FAR, MODEL_MIN_Y, 0.0, MODEL_FACE_MAX_FAR, MODEL_MAX_Y, 16.0
    );

    public InteractiveValveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        VoxelShape shape = switch (facing) {
            case NORTH -> INTERACTION_SHAPE_NORTH;
            case SOUTH -> INTERACTION_SHAPE_SOUTH;
            case EAST -> INTERACTION_SHAPE_EAST;
            case WEST -> INTERACTION_SHAPE_WEST;
            default -> INTERACTION_SHAPE_SOUTH;
        };
        return shape.move(0.0, -0.5625, 0.0);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        VoxelShape shape = switch (facing) {
            case NORTH -> INTERACTION_SHAPE_NORTH;
            case SOUTH -> INTERACTION_SHAPE_SOUTH;
            case EAST -> INTERACTION_SHAPE_EAST;
            case WEST -> INTERACTION_SHAPE_WEST;
            default -> INTERACTION_SHAPE_SOUTH;
        };
        return shape.move(0.0, -0.5625, 0.0);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (!level.isClientSide()) {
            level.levelEvent(2001, pos, Block.getId(state));
        }
        return result;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return null;
        }
        return this.defaultBlockState()
            .setValue(FACE, AttachFace.WALL)
            .setValue(FACING, face);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InteractiveValveBlockEntity(pos, state);
    }

}

