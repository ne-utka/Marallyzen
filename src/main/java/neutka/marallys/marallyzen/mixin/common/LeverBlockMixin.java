package neutka.marallys.marallyzen.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import neutka.marallys.marallyzen.blocks.LeverInteractionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeverBlock.class)
public class LeverBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void marallyzen$playLeverShake(BlockState state,
                                          Level level,
                                          BlockPos pos,
                                          Player player,
                                          BlockHitResult hit,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) {
            return;
        }
        boolean isLever = state.getBlock() == neutka.marallys.marallyzen.blocks.MarallyzenBlocks.INTERACTIVE_LEVER.get();
        boolean isValve = state.getBlock() == neutka.marallys.marallyzen.blocks.MarallyzenBlocks.INTERACTIVE_VALVE.get();
        if (!isLever && !isValve) {
            return;
        }
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
            "[LeverInteract] useWithoutItem player={} pos={} state={} face={}",
            player.getGameProfile().name(),
            pos,
            state.getBlock().getName().getString(),
            state.getValue(net.minecraft.world.level.block.LeverBlock.FACE)
        );
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            boolean started = isLever
                ? LeverInteractionHandler.start(serverPlayer, pos, state, hit != null ? hit.getLocation() : null)
                : neutka.marallys.marallyzen.blocks.ValveInteractionHandler.start(serverPlayer, pos, state, hit != null ? hit.getLocation() : null);
            if (started) {
                cir.setReturnValue(InteractionResult.CONSUME);
                cir.cancel();
            }
        }
    }
}
