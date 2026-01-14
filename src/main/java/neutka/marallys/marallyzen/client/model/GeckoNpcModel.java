package neutka.marallys.marallyzen.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

@OnlyIn(Dist.CLIENT)
public class GeckoNpcModel extends GeoModel<GeckoNpcEntity> {
    private static final ResourceLocation DEFAULT_MODEL =
            ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "geo/test_npc.geo.json");
    private static final ResourceLocation DEFAULT_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "animations/test_npc.animation.json");
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "textures/entity/test_npc.png");

    @Override
    public ResourceLocation getModelResource(GeckoNpcEntity animatable) {
        ResourceLocation model = animatable.getGeckolibModel();
        return model != null ? model : DEFAULT_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GeckoNpcEntity animatable) {
        ResourceLocation texture = animatable.getGeckolibTexture();
        return texture != null ? texture : DEFAULT_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GeckoNpcEntity animatable) {
        ResourceLocation animation = animatable.getGeckolibAnimation();
        return animation != null ? animation : DEFAULT_ANIMATION;
    }

    @Override
    public void setCustomAnimations(GeckoNpcEntity animatable, long instanceId, AnimationState<GeckoNpcEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) {
            return;
        }

        GeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            head.setRotX(modelData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(modelData.netHeadYaw() * Mth.DEG_TO_RAD);
        }

        float eyeYaw = Mth.clamp(modelData.netHeadYaw(), -30.0f, 30.0f) * Mth.DEG_TO_RAD * 0.35f;
        float eyePitch = Mth.clamp(modelData.headPitch(), -20.0f, 20.0f) * Mth.DEG_TO_RAD * 0.35f;

        GeoBone eyeLeft = getAnimationProcessor().getBone("eye_left");
        if (eyeLeft != null) {
            eyeLeft.setRotY(eyeYaw);
            eyeLeft.setRotX(eyePitch);
        }

        GeoBone eyeRight = getAnimationProcessor().getBone("eye_right");
        if (eyeRight != null) {
            eyeRight.setRotY(eyeYaw);
            eyeRight.setRotX(eyePitch);
        }
    }
}
