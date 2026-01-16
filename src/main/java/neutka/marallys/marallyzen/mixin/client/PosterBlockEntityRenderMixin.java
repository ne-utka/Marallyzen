package neutka.marallys.marallyzen.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import neutka.marallys.marallyzen.blocks.PosterBlock;
import neutka.marallys.marallyzen.client.ClientPosterManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class PosterBlockEntityRenderMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void marallyzen$hidePosterBlockEntity(
            BlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            CallbackInfo ci
    ) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return;
        }
        if (blockEntity.getBlockState().getBlock() instanceof PosterBlock) {
            BlockPos pos = blockEntity.getBlockPos();
            if (ClientPosterManager.isPosterHidden(pos)) {
                ci.cancel();
            }
        }
    }
}
