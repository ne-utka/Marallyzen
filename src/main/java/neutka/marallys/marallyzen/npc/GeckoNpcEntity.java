package neutka.marallys.marallyzen.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GeckoNpcEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> NPC_ID =
            SynchedEntityData.defineId(GeckoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> GEO_MODEL =
            SynchedEntityData.defineId(GeckoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> GEO_ANIMATION =
            SynchedEntityData.defineId(GeckoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> GEO_TEXTURE =
            SynchedEntityData.defineId(GeckoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> EXPRESSION =
            SynchedEntityData.defineId(GeckoNpcEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RandomSource random = RandomSource.create();
    private int blinkCooldown = 40 + random.nextInt(60);
    private boolean blinkPending;
    private static final int IDLE_INTERACT_SWAP_TICKS = 80;

    public GeckoNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NPC_ID, "");
        builder.define(GEO_MODEL, "");
        builder.define(GEO_ANIMATION, "");
        builder.define(GEO_TEXTURE, "");
        builder.define(EXPRESSION, "");
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            return;
        }
        blinkCooldown--;
        if (blinkCooldown <= 0) {
            blinkCooldown = 40 + random.nextInt(60);
            blinkPending = true;
        }
    }

    public void setNpcId(String npcId) {
        this.entityData.set(NPC_ID, npcId != null ? npcId : "");
    }

    public String getNpcId() {
        return this.entityData.get(NPC_ID);
    }

    public void setGeckolibModel(ResourceLocation model) {
        this.entityData.set(GEO_MODEL, model != null ? model.toString() : "");
    }

    public ResourceLocation getGeckolibModel() {
        return parseResource(this.entityData.get(GEO_MODEL));
    }

    public void setGeckolibAnimation(ResourceLocation animation) {
        this.entityData.set(GEO_ANIMATION, animation != null ? animation.toString() : "");
    }

    public ResourceLocation getGeckolibAnimation() {
        return parseResource(this.entityData.get(GEO_ANIMATION));
    }

    public void setGeckolibTexture(ResourceLocation texture) {
        this.entityData.set(GEO_TEXTURE, texture != null ? texture.toString() : "");
    }

    public ResourceLocation getGeckolibTexture() {
        return parseResource(this.entityData.get(GEO_TEXTURE));
    }

    public void setExpression(String expression) {
        this.entityData.set(EXPRESSION, expression != null ? expression : "");
    }

    public String getExpression() {
        return this.entityData.get(EXPRESSION);
    }

    private boolean consumeBlink() {
        if (!blinkPending) {
            return false;
        }
        blinkPending = false;
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::mainPredicate));
        controllers.add(new AnimationController<>(this, "blink", 0, this::blinkPredicate));
    }

    private <T extends GeoEntity> PlayState mainPredicate(AnimationState<T> state) {
        String expression = getExpression();
        if (expression != null && !expression.isBlank()) {
            if ("idle_interact".equalsIgnoreCase(expression)) {
                String cycle = ((this.tickCount / IDLE_INTERACT_SWAP_TICKS) % 2 == 0) ? "idle" : "interact";
                if ("interact".equalsIgnoreCase(cycle)) {
                    state.getController().setAnimation(RawAnimation.begin()
                            .then("interact", Animation.LoopType.PLAY_ONCE));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
                }
                return PlayState.CONTINUE;
            }
            if (!"idle".equalsIgnoreCase(expression)) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop(expression));
                return PlayState.CONTINUE;
            }
        }
        String fallbackAnimation = isMoving() ? "walk" : "idle";
        state.getController().setAnimation(RawAnimation.begin().thenLoop(fallbackAnimation));
        return PlayState.CONTINUE;
    }

    private <T extends GeoEntity> PlayState blinkPredicate(AnimationState<T> state) {
        if (consumeBlink()) {
            state.getController().setAnimation(RawAnimation.begin().then("blink", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcId", getNpcId());
        tag.putString("GeckoModel", this.entityData.get(GEO_MODEL));
        tag.putString("GeckoAnimation", this.entityData.get(GEO_ANIMATION));
        tag.putString("GeckoTexture", this.entityData.get(GEO_TEXTURE));
        tag.putString("Expression", getExpression());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcId")) {
            setNpcId(tag.getString("NpcId"));
        }
        if (tag.contains("GeckoModel")) {
            this.entityData.set(GEO_MODEL, tag.getString("GeckoModel"));
        }
        if (tag.contains("GeckoAnimation")) {
            this.entityData.set(GEO_ANIMATION, tag.getString("GeckoAnimation"));
        }
        if (tag.contains("GeckoTexture")) {
            this.entityData.set(GEO_TEXTURE, tag.getString("GeckoTexture"));
        }
        if (tag.contains("Expression")) {
            setExpression(tag.getString("Expression"));
        }
    }

    private static ResourceLocation parseResource(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ResourceLocation.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
