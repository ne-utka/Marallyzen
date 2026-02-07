package neutka.marallys.marallyzen.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import neutka.marallys.marallyzen.client.NpcSkinCache;
import neutka.marallys.marallyzen.npc.NpcEntity;

public class NpcRenderer extends LivingEntityRenderer<NpcEntity, NpcRenderer.NpcRenderState, PlayerModel> {
    private final PlayerModel defaultModel;
    private final PlayerModel slimModel;

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.defaultModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public NpcRenderState createRenderState() {
        return new NpcRenderState();
    }

    @Override
    public void extractRenderState(NpcEntity entity, NpcRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);
        renderState.npcId = entity.getNpcId();
        renderState.slim = NpcSkinCache.getInstance().isSlim(entity.getNpcId());
    }

    @Override
    public Identifier getTextureLocation(NpcRenderState renderState) {
        return NpcSkinCache.getInstance().getSkin(renderState.npcId);
    }

    @Override
    public void submit(NpcRenderState renderState, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        this.model = renderState.slim ? slimModel : defaultModel;
        super.submit(renderState, poseStack, renderTasks, cameraState);
    }

    public static class NpcRenderState extends AvatarRenderState {
        public String npcId = "";
        public boolean slim = false;
    }
}


