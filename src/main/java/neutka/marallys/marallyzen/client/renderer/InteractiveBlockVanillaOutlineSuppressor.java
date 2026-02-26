package neutka.marallys.marallyzen.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.InteractiveBlockTargeting;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.trigger.client.TriggerBindClientCache;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT)
public class InteractiveBlockVanillaOutlineSuppressor {
    @SubscribeEvent
    public static void onRenderBlockHighlight(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        BlockHitResult blockHit = event.getHitResult();
        if (blockHit == null || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        var state = event.getBlockState();
        boolean triggerBound = isTriggerBound(
            mc.level.dimension().identifier().toString(),
            blockHit.getBlockPos(),
            state
        );
        if (InteractiveBlockTargeting.getType(state) != InteractiveBlockTargeting.Type.NONE
            || triggerBound
            || MarallyzenBlocks.isBlocksTabBlock(state)
            || state.getBlock() == MarallyzenBlocks.INTERACTIVE_LEVER.get()
            || state.getBlock() == MarallyzenBlocks.INTERACTIVE_VALVE.get()) {
            event.setCanceled(true);
        }
    }

    private static boolean isTriggerBound(String dimensionId, BlockPos pos, BlockState state) {
        if (TriggerBindClientCache.isBound(dimensionId, pos)) {
            return true;
        }
        if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.HALF)) {
            return false;
        }
        DoubleBlockHalf half = state.getValue(DoorBlock.HALF);
        BlockPos counterpart = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        return TriggerBindClientCache.isBound(dimensionId, counterpart);
    }
}
