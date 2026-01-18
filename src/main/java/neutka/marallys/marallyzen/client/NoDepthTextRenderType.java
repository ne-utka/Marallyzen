package neutka.marallys.marallyzen.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NoDepthTextRenderType {
    private static final Map<ResourceLocation, RenderType> CACHE = new ConcurrentHashMap<>();
    private static final RenderStateShard.DepthTestStateShard ALWAYS_DEPTH_TEST =
        new RenderStateShard.DepthTestStateShard("always", GL11.GL_ALWAYS);

    private NoDepthTextRenderType() {
    }

    public static RenderType textNoDepth(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, NoDepthTextRenderType::create);
    }

    private static RenderType create(ResourceLocation texture) {
        return RenderType.create(
            "marallyzen_text_no_depth",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(ALWAYS_DEPTH_TEST)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                .setOutputState(RenderStateShard.MAIN_TARGET)
                .createCompositeState(false)
        );
    }
}
