package neutka.marallys.marallyzen.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.blocks.InteractiveLeverBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class InteractiveLeverModel extends GeoModel<InteractiveLeverBlockEntity> {
    @Override
    public ResourceLocation getModelResource(InteractiveLeverBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "geo/lever.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InteractiveLeverBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/block/lever.png");
    }

    @Override
    public ResourceLocation getAnimationResource(InteractiveLeverBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "animations/interactive_lever.animation.json");
    }

    @Override
    public RenderType getRenderType(InteractiveLeverBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
