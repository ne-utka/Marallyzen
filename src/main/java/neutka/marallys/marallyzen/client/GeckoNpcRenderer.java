package neutka.marallys.marallyzen.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;
import net.neoforged.api.distmarker.Dist;
import neutka.marallys.marallyzen.client.model.GeckoNpcModel;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.constant.dataticket.DataTicket;

public class GeckoNpcRenderer extends GeoEntityRenderer<GeckoNpcEntity, GeckoNpcRenderer.GeckoNpcRenderState> {
    public GeckoNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new GeckoNpcModel());
        this.shadowRadius = 0.35f;
    }

    @Override
    public GeckoNpcRenderState createRenderState(GeckoNpcEntity animatable, @Nullable Void relatedObject) {
        return new GeckoNpcRenderState();
    }

    public static final class GeckoNpcRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final java.util.Map<DataTicket<?>, Object> dataMap = new Reference2ObjectOpenHashMap<>();

        @Override
        public java.util.Map<DataTicket<?>, Object> getDataMap() {
            return dataMap;
        }
    }
}
