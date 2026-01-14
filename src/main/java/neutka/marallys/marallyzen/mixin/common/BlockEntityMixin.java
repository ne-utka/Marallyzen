package neutka.marallys.marallyzen.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import neutka.marallys.marallyzen.cutscene.world.server.CutsceneWorldRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {
    @Shadow
    protected Level level;

    @Shadow
    public BlockPos worldPosition;

    @Inject(method = "setChanged", at = @At("TAIL"))
    private void marallyzen$cutsceneBlockEntityChanged(CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            CutsceneWorldRecorder.onBlockEntityChanged(serverLevel, (BlockEntity) (Object) this);
        }
    }
}
