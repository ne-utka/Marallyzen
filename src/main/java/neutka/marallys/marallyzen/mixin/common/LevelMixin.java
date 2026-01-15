package neutka.marallys.marallyzen.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import neutka.marallys.marallyzen.cutscene.world.server.CutsceneWorldRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
    @Inject(method = "setBlock", at = @At("HEAD"))
    private void marallyzen$cutscenePre(BlockPos pos, BlockState state, int flags,
                                        CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerLevel serverLevel) {
            CutsceneWorldRecorder.capturePre(serverLevel, pos);
        }
    }

    @Inject(method = "setBlock", at = @At("RETURN"))
    private void marallyzen$cutscenePost(BlockPos pos, BlockState state, int flags,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }
        if ((Object) this instanceof ServerLevel serverLevel) {
            CutsceneWorldRecorder.capturePost(serverLevel, pos);
        }
    }
}
