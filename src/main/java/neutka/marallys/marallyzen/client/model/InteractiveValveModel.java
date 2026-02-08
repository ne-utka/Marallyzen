package neutka.marallys.marallyzen.client.model;

import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.InteractiveValveBlockEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class InteractiveValveModel extends GeoModel<InteractiveValveBlockEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Marallyzen.MODID, "valve");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/valve.png");
    }

    @Override
    public Identifier getAnimationResource(InteractiveValveBlockEntity animatable) {
        return Identifier.fromNamespaceAndPath(Marallyzen.MODID, "interactive_valve");
    }

    public net.minecraft.client.renderer.rendertype.RenderType getRenderType(Identifier texture) {
        return RenderTypes.entityCutoutNoCull(texture);
    }
}








