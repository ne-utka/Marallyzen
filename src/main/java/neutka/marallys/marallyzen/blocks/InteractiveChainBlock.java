package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import neutka.marallys.marallyzen.client.fpv.FpvEmoteInvoker;
import neutka.marallys.marallyzen.client.fpv.MarallyzenRenderContext;
import neutka.marallys.marallyzen.network.NetworkHelper;
import neutka.marallys.marallyzen.network.InteractiveChainInteractPacket;
import neutka.marallys.marallyzen.blocks.InteractiveChainJumpHandler;

/**
 * Interactive chain block that only hangs vertically from a block above.
 */
public class InteractiveChainBlock extends ChainBlock implements EntityBlock {
    public InteractiveChainBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, Direction.Axis.Y);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            MarallyzenRenderContext.setFpvEmoteEnabled(true);
            FpvEmoteInvoker.play(player, InteractiveChainJumpHandler.CHAIN_EMOTE_ID);
            NetworkHelper.sendToServer(new InteractiveChainInteractPacket(pos));
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(AXIS) != Direction.Axis.Y) {
            return false;
        }
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.getBlock() == this) {
            return true;
        }
        return aboveState.isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InteractiveChainBlockEntity(pos, state);
    }
}
