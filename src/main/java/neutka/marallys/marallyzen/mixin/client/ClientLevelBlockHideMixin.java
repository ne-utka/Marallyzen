package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import neutka.marallys.marallyzen.client.ClientDictaphoneManager;
import neutka.marallys.marallyzen.client.ClientPosterManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(net.minecraft.client.multiplayer.ClientLevel.class)
public class ClientLevelBlockHideMixin {
    @ModifyVariable(
        method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
        at = @At("HEAD"),
        argsOnly = true
    )
    private BlockState marallyzen$forceAirForHidden(BlockState state, BlockPos pos) {
        if (state != null && state.isAir()) {
            return state;
        }
        if (ClientDictaphoneManager.hasClientDictaphone(pos)
            || ClientPosterManager.isPosterHidden(pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }
}
