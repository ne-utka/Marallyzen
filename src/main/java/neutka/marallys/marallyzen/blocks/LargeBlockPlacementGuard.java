package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.List;

@EventBusSubscriber(modid = Marallyzen.MODID)
public class LargeBlockPlacementGuard {
    private static final int CHECK_RADIUS = 3;

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();

        BlockPos placedPos = event.getPos();
        if (isPlacementBlocked(level, placedPos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level == null) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof BlockItem)) {
            return;
        }

        BlockPlaceContext context = new BlockPlaceContext(
            event.getEntity(),
            event.getHand(),
            event.getItemStack(),
            event.getHitVec()
        );
        BlockPos targetPos = context.getClickedPos();
        BlockState targetState = level.getBlockState(targetPos);
        if (!targetState.canBeReplaced(context)) {
            targetPos = targetPos.relative(context.getClickedFace());
        }

        if (isPlacementBlocked(level, targetPos)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private static boolean isProtectedLargeBlock(BlockState state) {
        return state.is(MarallyzenBlocks.COACH.get())
            || state.is(MarallyzenBlocks.BARREL_FULL_PILE.get())
            || state.is(MarallyzenBlocks.DRYING_FISH_RACK.get())
            || state.is(MarallyzenBlocks.FISH.get())
            || state.is(MarallyzenBlocks.FISH_BOX.get())
            || state.is(MarallyzenBlocks.FISH_BOX_EMPTY.get())
            || state.is(MarallyzenBlocks.FISH_PILE.get())
            || state.is(MarallyzenBlocks.FISH_PRIZE_WALL_DECORATION.get())
            || state.is(MarallyzenBlocks.FISHING_NET_WALL_DECORATION.get())
            || state.is(MarallyzenBlocks.FISHING_ROD.get())
            || state.is(MarallyzenBlocks.FISHING_ROD_RACK.get())
            || state.is(MarallyzenBlocks.LEANING_FISHING_ROD.get());
    }

    private static boolean isPlacementBlocked(LevelAccessor level, BlockPos placedPos) {
        AABB placedBox = new AABB(placedPos);
        for (int dx = -CHECK_RADIUS; dx <= CHECK_RADIUS; dx++) {
            for (int dy = -CHECK_RADIUS; dy <= CHECK_RADIUS; dy++) {
                for (int dz = -CHECK_RADIUS; dz <= CHECK_RADIUS; dz++) {
                    BlockPos checkPos = placedPos.offset(dx, dy, dz);
                    if (checkPos.equals(placedPos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(checkPos);
                    if (!isProtectedLargeBlock(state)) {
                        continue;
                    }
                    if (intersectsBlockShape(level, checkPos, state, placedBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean intersectsBlockShape(LevelAccessor level, BlockPos pos, BlockState state, AABB targetBox) {
        VoxelShape shape = state.getShape(level, pos, CollisionContext.empty());
        if (shape.isEmpty()) {
            return false;
        }
        List<AABB> boxes = shape.toAabbs();
        for (AABB box : boxes) {
            AABB worldBox = box.move(pos);
            if (worldBox.intersects(targetBox)) {
                return true;
            }
        }
        return false;
    }
}
