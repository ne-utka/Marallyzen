package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import neutka.marallys.marallyzen.client.NpcSkinCache;
import neutka.marallys.marallyzen.npc.NpcEntity;

public class NpcRenderer extends LivingEntityRenderer<NpcEntity, PlayerModel<NpcEntity>> {
    private final PlayerModel<NpcEntity> defaultModel;
    private final PlayerModel<NpcEntity> slimModel;

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.defaultModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        return NpcSkinCache.getInstance().getSkin(entity.getNpcId());
    }

    @Override
    public void render(NpcEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        this.model = NpcSkinCache.getInstance().isSlim(entity.getNpcId()) ? slimModel : defaultModel;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
