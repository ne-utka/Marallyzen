package neutka.marallys.marallyzen.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.InteractiveBlockTargeting;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;

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
        if (InteractiveBlockTargeting.getType(state) != InteractiveBlockTargeting.Type.NONE
            || MarallyzenBlocks.isBlocksTabBlock(state)
            || state.getBlock() == MarallyzenBlocks.INTERACTIVE_LEVER.get()
            || state.getBlock() == MarallyzenBlocks.INTERACTIVE_VALVE.get()) {
            event.setCanceled(true);
        }
    }
}
