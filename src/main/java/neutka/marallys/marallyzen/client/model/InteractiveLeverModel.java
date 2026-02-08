package neutka.marallys.marallyzen.client.model;

import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.InteractiveLeverBlockEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class InteractiveLeverModel extends GeoModel<InteractiveLeverBlockEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Marallyzen.MODID, "lever");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/lever.png");
    }

    @Override
    public Identifier getAnimationResource(InteractiveLeverBlockEntity animatable) {
        return Identifier.fromNamespaceAndPath(Marallyzen.MODID, "interactive_lever");
    }

    public net.minecraft.client.renderer.rendertype.RenderType getRenderType(Identifier texture) {
        return RenderTypes.entityCutoutNoCull(texture);
    }
}







