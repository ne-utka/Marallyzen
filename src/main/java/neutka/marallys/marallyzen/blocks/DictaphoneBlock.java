package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;
import neutka.marallys.marallyzen.util.PermissionHelper;

public class DictaphoneBlock extends ModelShapeFacingBlock {
    public static final BooleanProperty ANIMATED = BooleanProperty.create("animated");

    public DictaphoneBlock(Properties properties, String modelPath) {
        super(properties, modelPath);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
            .setValue(ANIMATED, Boolean.FALSE));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return state.setValue(ANIMATED, Boolean.FALSE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, ANIMATED);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (player != null && level instanceof Level lvl) {
            if (!(player instanceof ServerPlayer serverPlayer && PermissionHelper.isOp(serverPlayer))
                && neutka.marallys.marallyzen.dictaphone.DictaphoneScriptManager.isProtectedByOp(pos, lvl.dimension())) {
                return 0.0f;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    // onDestroyedByPlayer no longer exists in this NeoForge/Minecraft version.
}
