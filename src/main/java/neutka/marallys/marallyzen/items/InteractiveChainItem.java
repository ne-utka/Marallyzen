package neutka.marallys.marallyzen.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import neutka.marallys.marallyzen.blocks.InteractiveChainPlacementHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Places an interactive chain column that auto-extends downwards.
 */
public class InteractiveChainItem extends BlockItem {
    public InteractiveChainItem(net.minecraft.world.level.block.Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos placePos = clickedPos;

        if (!level.getBlockState(placePos).isAir()) {
            placePos = placePos.relative(face);
        }

        if (!level.getBlockState(placePos).isAir()) {
            return InteractionResult.FAIL;
        }

        BlockPos above = placePos.above();
        if (!level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN)) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        int maxBlocks = stack.getCount();
        if (context.getPlayer() != null && context.getPlayer().getAbilities().instabuild) {
            maxBlocks = Integer.MAX_VALUE;
        }

        List<BlockPos> positions = collectChainPositions(level, placePos, context, maxBlocks);
        if (positions.isEmpty()) {
            return InteractionResult.FAIL;
        }

        BlockState chainState = getBlock().defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);

        BlockPos topPos = positions.get(0);
        if (!level.setBlock(topPos, chainState, 11)) {
            return InteractionResult.FAIL;
        }

        SoundType sound = chainState.getSoundType(level, topPos, context.getPlayer());
        level.playSound(context.getPlayer(), topPos, sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
        level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, topPos);

        if (!level.isClientSide && positions.size() > 1) {
            InteractiveChainPlacementHandler.enqueue((ServerLevel) level, chainState, positions.subList(1, positions.size()));
        }

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(positions.size());
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private List<BlockPos> collectChainPositions(Level level, BlockPos startPos, BlockPlaceContext context, int maxBlocks) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos current = startPos;
        int minY = level.getMinBuildHeight();

        while (current.getY() >= minY && positions.size() < maxBlocks) {
            if (!level.getBlockState(current).isAir()) {
                break;
            }

            BlockPos below = current.below();
            if (below.getY() < minY) {
                break;
            }

            // Keep a one-block air gap above the first solid block below.
            if (!level.getBlockState(below).isAir()) {
                break;
            }

            positions.add(current.immutable());
            current = below;
        }

        return positions;
    }
}
