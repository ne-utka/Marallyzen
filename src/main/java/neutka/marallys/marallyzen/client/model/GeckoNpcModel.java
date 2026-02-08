package neutka.marallys.marallyzen.client.model;

import net.minecraft.resources.Identifier;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class GeckoNpcModel extends GeoModel<GeckoNpcEntity> {
    private static final Identifier DEFAULT_MODEL =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "geo/test_npc.geo.json");
    private static final Identifier DEFAULT_ANIMATION =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "animations/test_npc.animation.json");
    private static final Identifier DEFAULT_TEXTURE =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "textures/entity/test_npc.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return DEFAULT_MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return DEFAULT_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(GeckoNpcEntity animatable) {
        Identifier animation = animatable.getGeckolibAnimation();
        return animation != null ? animation : DEFAULT_ANIMATION;
    }
}


