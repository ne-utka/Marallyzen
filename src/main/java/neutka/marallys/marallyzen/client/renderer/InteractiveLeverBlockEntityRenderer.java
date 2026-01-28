package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import neutka.marallys.marallyzen.blocks.InteractiveLeverBlockEntity;
import neutka.marallys.marallyzen.client.model.InteractiveLeverModel;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class InteractiveLeverBlockEntityRenderer extends GeoBlockRenderer<InteractiveLeverBlockEntity> {
    public InteractiveLeverBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new InteractiveLeverModel());
    }

    @Override
    public void preRender(PoseStack poseStack, InteractiveLeverBlockEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        // Model pivot is 1 block too high; pull it down and add extra 0.25 block drop.
        poseStack.translate(0.0, -1.25, 0.0);
    }
}
