package neutka.marallys.marallyzen.client.model;

import net.minecraft.resources.Identifier;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.GeckoNpcRenderer;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GeckoNpcModel extends GeoModel<GeckoNpcEntity> {
    private static final Identifier DEFAULT_MODEL =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "test_npc");
    private static final Identifier DEFAULT_ANIMATION =
            Identifier.fromNamespaceAndPath(Marallyzen.MODID, "test_npc");
    private static final Identifier DEFAULT_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final Set<Long> WARNED_MODEL_FALLBACK = ConcurrentHashMap.newKeySet();
    private static final Set<Long> WARNED_TEXTURE_FALLBACK = ConcurrentHashMap.newKeySet();

    @Override
    public void addAdditionalStateData(GeckoNpcEntity animatable, Object relatedObject, GeoRenderState renderState) {
        super.addAdditionalStateData(animatable, relatedObject, renderState);

        Identifier model = animatable.getGeckolibModel();
        if (model != null) {
            renderState.addGeckolibData(GeckoNpcRenderer.MODEL_RESOURCE, model);
        }

        Identifier texture = animatable.getGeckolibTexture();
        if (texture != null) {
            renderState.addGeckolibData(GeckoNpcRenderer.TEXTURE_RESOURCE, texture);
        }
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        Identifier model = GeckoNpcRenderer.getStateModel(renderState);
        if (model == null) {
            model = renderState.getGeckolibData(GeckoNpcRenderer.MODEL_RESOURCE);
        }
        if (model == null) {
            model = GeckoNpcRenderer.getCachedModel(renderState);
        }
        if (model == null) {
            long key = GeckoNpcRenderer.getStateInstanceId(renderState);
            if (WARNED_MODEL_FALLBACK.add(key)) {
                Marallyzen.LOGGER.warn("GeckoNpcModel: model fallback to default for instanceId={}", key);
            }
        }
        return model != null ? model : DEFAULT_MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Identifier texture = GeckoNpcRenderer.getStateTexture(renderState);
        if (texture == null) {
            texture = renderState.getGeckolibData(GeckoNpcRenderer.TEXTURE_RESOURCE);
        }
        if (texture == null) {
            texture = GeckoNpcRenderer.getCachedTexture(renderState);
        }
        if (texture == null) {
            long key = GeckoNpcRenderer.getStateInstanceId(renderState);
            if (WARNED_TEXTURE_FALLBACK.add(key)) {
                Marallyzen.LOGGER.warn("GeckoNpcModel: texture fallback to default for instanceId={}", key);
            }
        }
        return texture != null ? texture : DEFAULT_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(GeckoNpcEntity animatable) {
        Identifier animation = animatable.getGeckolibAnimation();
        return animation != null ? animation : DEFAULT_ANIMATION;
    }
}


