package neutka.marallys.marallyzen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import neutka.marallys.marallyzen.blocks.MarallyzenBlocks;
import neutka.marallys.marallyzen.client.narration.NarrationManager;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class OldTvBindHandler {
    private OldTvBindHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (!OldTvMediaManager.isBindMode()) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.getBlock() != MarallyzenBlocks.OLD_TV.get()) {
            return;
        }
        OldTvMediaManager.bind(event.getPos(), event.getLevel().dimension());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            String mediaName = OldTvMediaManager.getSelectedMedia();
            if (mediaName != null) {
                NarrationManager.getInstance().startNarration(
                        OldTvMediaManager.buildBindNarration(mediaName),
                        null,
                        5,
                        100,
                        3
                );
            }
        }
    }
}
