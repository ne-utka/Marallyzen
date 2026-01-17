package neutka.marallys.marallyzen.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.InteractiveBlockTargeting;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class InteractiveBlockVanillaOutlineSuppressor {
    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        HitResult target = event.getTarget();
        if (target == null || target.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) target;
        if (InteractiveBlockTargeting.getType(mc.level.getBlockState(blockHit.getBlockPos()))
            != InteractiveBlockTargeting.Type.NONE
            || MarallyzenBlocks.isBlocksTabBlock(mc.level.getBlockState(blockHit.getBlockPos()))) {
            event.setCanceled(true);
        }
    }
}
