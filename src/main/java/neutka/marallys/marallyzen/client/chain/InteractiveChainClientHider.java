package neutka.marallys.marallyzen.client.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only helper to hide static chain blocks while a swing render is active.
 */
public final class InteractiveChainClientHider {
    private static final Map<BlockPos, List<HiddenBlock>> HIDDEN = new ConcurrentHashMap<>();

    private InteractiveChainClientHider() {}

    private static final class HiddenBlock {
        private final BlockPos pos;
        private final BlockState state;

        private HiddenBlock(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }

    public static void hideChain(Level level, BlockPos root) {
        if (level == null || root == null || HIDDEN.containsKey(root)) {
            return;
        }
        List<HiddenBlock> hidden = new ArrayList<>();
        BlockPos current = root;
        while (level.getBlockState(current).getBlock() == MarallyzenBlocks.INTERACTIVE_CHAIN.get()) {
            BlockState state = level.getBlockState(current);
            hidden.add(new HiddenBlock(current.immutable(), state));
            level.setBlock(current, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
            current = current.below();
        }
        if (!hidden.isEmpty()) {
            HIDDEN.put(root, hidden);
        }
    }

    public static void restoreChain(Level level, BlockPos root) {
        if (level == null || root == null) {
            return;
        }
        List<HiddenBlock> hidden = HIDDEN.remove(root);
        if (hidden == null) {
            return;
        }
        for (HiddenBlock block : hidden) {
            level.setBlock(block.pos, block.state, 11);
        }
    }

    public static int getHiddenLength(BlockPos root) {
        List<HiddenBlock> hidden = HIDDEN.get(root);
        return hidden != null ? hidden.size() : 0;
    }
}
