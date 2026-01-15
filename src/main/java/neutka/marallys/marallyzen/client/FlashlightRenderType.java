package neutka.marallys.marallyzen.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Custom RenderType for volumetric flashlight beam with additive blending.
 */
public class FlashlightRenderType {
    
    // White texture for solid color rendering
    private static final ResourceLocation WHITE_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    
    // Additive transparency state
    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY = 
        new RenderStateShard.TransparencyStateShard(
            "additive_transparency",
            () -> {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.blendFunc(
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
                );
            },
            () -> {
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            }
        );
    
    public static final RenderType FLASHLIGHT_BEAM = RenderType.create(
        "flashlight_beam",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLES,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(WHITE_TEXTURE, false, false))
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
            .createCompositeState(false)
    );
}

