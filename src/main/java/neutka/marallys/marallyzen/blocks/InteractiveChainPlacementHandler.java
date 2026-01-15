package neutka.marallys.marallyzen.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import neutka.marallys.marallyzen.Marallyzen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Animates interactive chain placement by placing segments over several ticks.
 */
@EventBusSubscriber(modid = Marallyzen.MODID)
public class InteractiveChainPlacementHandler {
    private static final int BLOCKS_PER_TICK = 3;
    private static final Map<ServerLevel, Deque<PendingPlacement>> pending = new HashMap<>();

    private static final class PendingPlacement {
        private final BlockState state;
        private final List<BlockPos> positions;
        private int index = 0;

        private PendingPlacement(BlockState state, List<BlockPos> positions) {
            this.state = state;
            this.positions = positions;
        }
    }

    public static void enqueue(ServerLevel level, BlockState state, List<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) {
            return;
        }
        pending.computeIfAbsent(level, key -> new ArrayDeque<>())
            .add(new PendingPlacement(state, positions));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Deque<PendingPlacement> queue = pending.get(level);
            if (queue == null || queue.isEmpty()) {
                continue;
            }

            PendingPlacement current = queue.peek();
            if (current == null) {
                continue;
            }

            int placedThisTick = 0;
            while (current.index < current.positions.size() && placedThisTick < BLOCKS_PER_TICK) {
                BlockPos pos = current.positions.get(current.index++);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, current.state, 11);
                }
                placedThisTick++;
            }

            if (current.index >= current.positions.size()) {
                queue.poll();
            }

            if (queue.isEmpty()) {
                pending.remove(level);
            }
        }
    }
}
