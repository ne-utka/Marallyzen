package neutka.marallys.marallyzen.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import neutka.marallys.marallyzen.client.model.GeckoNpcModel;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class GeckoNpcRenderer extends GeoEntityRenderer<GeckoNpcEntity> {
    public GeckoNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new GeckoNpcModel());
        this.shadowRadius = 0.35f;
    }
}
