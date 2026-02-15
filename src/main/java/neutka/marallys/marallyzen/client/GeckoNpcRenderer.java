package neutka.marallys.marallyzen.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.model.GeckoNpcModel;
import neutka.marallys.marallyzen.npc.GeckoNpcEntity;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.state.ControllerState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.constant.dataticket.DataTicket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GeckoNpcRenderer extends GeoEntityRenderer<GeckoNpcEntity, GeckoNpcRenderer.GeckoNpcRenderState> {
    public static final DataTicket<Identifier> MODEL_RESOURCE =
            DataTicket.create("marallyzen_gecko_model_resource", Identifier.class);
    public static final DataTicket<Identifier> TEXTURE_RESOURCE =
            DataTicket.create("marallyzen_gecko_texture_resource", Identifier.class);

    private final Map<Integer, AnimatableManager<GeckoNpcEntity>> fallbackManagers = new HashMap<>();
    private final Set<Integer> warnedMissingManager = new HashSet<>();
    private static final Map<Long, Identifier> CACHED_MODEL_BY_INSTANCE = new ConcurrentHashMap<>();
    private static final Map<Long, Identifier> CACHED_TEXTURE_BY_INSTANCE = new ConcurrentHashMap<>();

    public GeckoNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new GeckoNpcModel());
        this.shadowRadius = 0.35f;
    }

    @Override
    public GeckoNpcRenderState createRenderState(GeckoNpcEntity animatable, @Nullable Void relatedObject) {
        return new GeckoNpcRenderState();
    }

    @Override
    public void extractRenderState(GeckoNpcEntity entity, GeckoNpcRenderState state, float partialTick) {
        try {
            super.extractRenderState(entity, state, partialTick);
        } catch (NullPointerException npe) {
            if (!hasAnimationProcessorFrame(npe)) {
                throw npe;
            }

            injectFallbackManager(entity, state, "extractRenderState_npe");
            applyNpcResources(entity, state);
        }

        ensureControllerStates(entity, state);
    }

    @Override
    public void captureDefaultRenderState(GeckoNpcEntity entity, @Nullable Void relatedObject, GeckoNpcRenderState state, float partialTick) {
        super.captureDefaultRenderState(entity, relatedObject, state, partialTick);
        applyNpcResources(entity, state);

        AnimatableManager<?> manager = state.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager != null) {
            fallbackManagers.remove(entity.getId());
            warnedMissingManager.remove(entity.getId());
            return;
        }

        injectFallbackManager(entity, state, "captureDefaultRenderState");
    }

    @Override
    public void addRenderData(GeckoNpcEntity entity, @Nullable Void relatedObject, GeckoNpcRenderState state, float partialTick) {
        super.addRenderData(entity, relatedObject, state, partialTick);
        applyNpcResources(entity, state);
    }

    private static void applyNpcResources(GeckoNpcEntity entity, GeckoNpcRenderState state) {
        long instanceId = entity.getId();
        Identifier model = entity.getGeckolibModel();
        Identifier texture = entity.getGeckolibTexture();

        state.setNpcRenderResources(instanceId, model, texture);
        state.addGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, instanceId);

        if (model != null) {
            state.addGeckolibData(MODEL_RESOURCE, model);
            CACHED_MODEL_BY_INSTANCE.put(instanceId, model);
        }

        if (texture != null) {
            state.addGeckolibData(TEXTURE_RESOURCE, texture);
            CACHED_TEXTURE_BY_INSTANCE.put(instanceId, texture);
        }
    }

    public static @Nullable Identifier getStateModel(GeoRenderState renderState) {
        if (renderState instanceof GeckoNpcRenderState state) {
            return state.getMarallyzenModel();
        }
        return null;
    }

    public static @Nullable Identifier getStateTexture(GeoRenderState renderState) {
        if (renderState instanceof GeckoNpcRenderState state) {
            return state.getMarallyzenTexture();
        }
        return null;
    }

    public static long getStateInstanceId(GeoRenderState renderState) {
        if (renderState instanceof GeckoNpcRenderState state) {
            long id = state.getMarallyzenInstanceId();
            if (id >= 0) {
                return id;
            }
        }

        Long instanceId = renderState.getGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID);
        return instanceId != null ? instanceId : -1L;
    }

    public static @Nullable Identifier getCachedModel(GeoRenderState renderState) {
        long instanceId = getStateInstanceId(renderState);
        if (instanceId < 0) {
            return null;
        }
        return CACHED_MODEL_BY_INSTANCE.get(instanceId);
    }

    public static @Nullable Identifier getCachedTexture(GeoRenderState renderState) {
        long instanceId = getStateInstanceId(renderState);
        if (instanceId < 0) {
            return null;
        }
        return CACHED_TEXTURE_BY_INSTANCE.get(instanceId);
    }

    private void injectFallbackManager(GeckoNpcEntity entity, GeckoNpcRenderState state, String source) {
        AnimatableManager<GeckoNpcEntity> fallback = fallbackManagers.computeIfAbsent(
                entity.getId(),
                id -> new AnimatableManager<>(entity)
        );
        state.setNpcRenderResources(entity.getId(), state.getMarallyzenModel(), state.getMarallyzenTexture());
        state.addGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, (long)entity.getId());
        state.addGeckolibData(DataTickets.ANIMATABLE_MANAGER, fallback);
        state.addGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES, new ControllerState[0]);

        if (warnedMissingManager.add(entity.getId())) {
            Marallyzen.LOGGER.warn(
                    "GeckoNpcRenderer: missing AnimatableManager for entity id={} removed={} npcId='{}' source={} model={} texture={}; using fallback manager",
                    entity.getId(),
                    entity.isRemoved(),
                    entity.getNpcId(),
                    source,
                    entity.getGeckolibModel(),
                    entity.getGeckolibTexture()
            );
        }
    }

    private void ensureControllerStates(GeckoNpcEntity entity, GeckoNpcRenderState state) {
        AnimatableManager<?> manager = state.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            injectFallbackManager(entity, state, "ensureControllerStates_missing_manager");
        }

        ControllerState[] states = state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES);
        if (states != null && states.length > 0) {
            return;
        }

        try {
            AnimationProcessor.extractControllerStates(entity, state, getGeoModel());
        } catch (NullPointerException npe) {
            if (!hasAnimationProcessorFrame(npe)) {
                throw npe;
            }

            injectFallbackManager(entity, state, "ensureControllerStates_npe");
            AnimationProcessor.extractControllerStates(entity, state, getGeoModel());
        }
    }

    private static boolean hasAnimationProcessorFrame(Throwable throwable) {
        for (StackTraceElement element : throwable.getStackTrace()) {
            if ("software.bernie.geckolib.animation.AnimationProcessor".equals(element.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static final class GeckoNpcRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final java.util.Map<DataTicket<?>, Object> dataMap = new Reference2ObjectOpenHashMap<>();
        private long marallyzenInstanceId = -1L;
        private @Nullable Identifier marallyzenModel;
        private @Nullable Identifier marallyzenTexture;

        @Override
        public <D> void addGeckolibData(DataTicket<D> dataTicket, D data) {
            this.dataMap.put(dataTicket, data);
        }

        @Override
        public boolean hasGeckolibData(DataTicket<?> dataTicket) {
            return this.dataMap.containsKey(dataTicket);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <D> @Nullable D getGeckolibData(DataTicket<D> dataTicket) {
            return (D)this.dataMap.get(dataTicket);
        }

        @Override
        public java.util.Map<DataTicket<?>, Object> getDataMap() {
            return this.dataMap;
        }

        private void setNpcRenderResources(long instanceId, @Nullable Identifier model, @Nullable Identifier texture) {
            this.marallyzenInstanceId = instanceId;
            this.marallyzenModel = model;
            this.marallyzenTexture = texture;
        }

        private long getMarallyzenInstanceId() {
            return this.marallyzenInstanceId;
        }

        private @Nullable Identifier getMarallyzenModel() {
            return this.marallyzenModel;
        }

        private @Nullable Identifier getMarallyzenTexture() {
            return this.marallyzenTexture;
        }
    }
}
