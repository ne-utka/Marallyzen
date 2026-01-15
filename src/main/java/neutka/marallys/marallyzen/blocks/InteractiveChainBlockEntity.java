package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

public class InteractiveChainBlockEntity extends BlockEntity {
    public InteractiveChainBlockEntity(BlockPos pos, BlockState state) {
        super(MarallyzenBlockEntities.INTERACTIVE_CHAIN_BE.get(), pos, state);
    }

    public AABB getRenderBoundingBox() {
        Level level = getLevel();
        if (level == null) {
            BlockPos pos = getBlockPos();
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        }
        int length = 0;
        BlockPos current = getBlockPos();
        while (level.getBlockState(current).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            length++;
            current = current.below();
        }
        if (length <= 0) {
            BlockPos pos = getBlockPos();
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        }
        BlockPos min = getBlockPos().below(length - 1);
        BlockPos max = getBlockPos();
        return new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1, max.getY() + 1, max.getZ() + 1
        ).inflate(1.0);
    }
}
