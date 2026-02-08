package neutka.marallys.marallyzen.blocks;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.server.DecoratedPotCarryManager;

@EventBusSubscriber(modid = Marallyzen.MODID)
public final class DecoratedPotInteractionHandler {
    private DecoratedPotInteractionHandler() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.getBlock() != Blocks.DECORATED_POT) {
            return;
        }
        boolean picked = DecoratedPotCarryManager.tryPickup(player, event.getPos(), state);
        if (picked) {
            event.setCancellationResult(InteractionResult.SUCCESS);
        } else {
            event.setCancellationResult(InteractionResult.CONSUME);
        }
        event.setCanceled(true);
    }
}
