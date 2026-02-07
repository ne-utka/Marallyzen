package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;

/**
 * Fallback renderer to avoid crashes when GeckoLib renderers are disabled.
 */
public class GeckoNpcFallbackRenderer extends EntityRenderer<GeckoNpcEntity, GeckoNpcFallbackRenderer.RenderState> {

    public GeckoNpcFallbackRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(GeckoNpcEntity entity, RenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        // Intentionally empty: prevents renderer null crash while GeckoLib is disabled.
        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static final class RenderState extends EntityRenderState {
    }
}
