package neutka.marallys.marallyzen.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.nbt.CompoundTag;
import neutka.marallys.marallyzen.entity.DecoratedPotCarryEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DecoratedPotRenderer.class)
public class DecoratedPotRendererMixin {
    @Inject(
        method = "render(Lnet/minecraft/world/level/block/entity/DecoratedPotBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At("HEAD")
    )
    private void marallyzen$applyRotation(DecoratedPotBlockEntity blockEntity, float partialTick,
                                          PoseStack poseStack, MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay, CallbackInfo ci) {
        if (blockEntity == null) {
            return;
        }
        CompoundTag data = blockEntity.getPersistentData();
        if (data == null || !data.contains(DecoratedPotCarryEntity.ROTATION_TAG)) {
            return;
        }
        float yaw = data.getFloat(DecoratedPotCarryEntity.ROTATION_TAG).orElse(0.0f);
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5, -0.5, -0.5);
    }
}
