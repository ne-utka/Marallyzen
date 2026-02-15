package neutka.marallys.marallyzen.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;

public class TransparentItemFrameRenderer<T extends ItemFrame> extends ItemFrameRenderer<T> {
    private final boolean glowFrame;

    public TransparentItemFrameRenderer(EntityRendererProvider.Context context, boolean glowFrame) {
        super(context);
        this.glowFrame = glowFrame;
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        if (this.glowFrame) {
            return Math.max(GLOW_FRAME_BRIGHTNESS, super.getBlockLightLevel(entity, pos));
        }
        return super.getBlockLightLevel(entity, pos);
    }

    @Override
    public void extractRenderState(T entity, ItemFrameRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isGlowFrame = this.glowFrame;
        state.isInvisible = true;
    }
}
