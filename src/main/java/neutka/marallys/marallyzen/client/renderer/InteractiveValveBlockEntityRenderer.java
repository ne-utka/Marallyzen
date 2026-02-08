package neutka.marallys.marallyzen.client.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import neutka.marallys.marallyzen.blocks.InteractiveValveBlockEntity;
import neutka.marallys.marallyzen.client.model.InteractiveValveModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Map;

public class InteractiveValveBlockEntityRenderer extends GeoBlockRenderer<InteractiveValveBlockEntity, InteractiveValveBlockEntityRenderer.RenderState> {
    public InteractiveValveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new InteractiveValveModel());
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<RenderState> renderPassInfo) {
        // Model pivot is 1 block too high; pull it down and add extra 0.25 block drop.
        renderPassInfo.poseStack().translate(0.0, -1.25, 0.0);
    }

    public static final class RenderState extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> data = new Reference2ObjectOpenHashMap<>();

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return data;
        }
    }
}

