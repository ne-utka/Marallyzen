package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;

/**
 * Fallback renderer to avoid crashes when GeckoLib renderers are disabled.
 */
public class GeckoNpcFallbackRenderer extends EntityRenderer<GeckoNpcEntity> {

    public GeckoNpcFallbackRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GeckoNpcEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // Intentionally empty: prevents renderer null crash while GeckoLib is disabled.
    }

    @Override
    public ResourceLocation getTextureLocation(GeckoNpcEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    }
}
