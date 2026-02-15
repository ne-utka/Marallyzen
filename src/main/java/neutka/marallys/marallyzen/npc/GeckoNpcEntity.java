package neutka.marallys.marallyzen.npc;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.LoopType;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.state.AnimationTest;
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
        if (!level().isClientSide()) {
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

    public void setGeckolibModel(Identifier model) {
        this.entityData.set(GEO_MODEL, model != null ? model.toString() : "");
    }

    public Identifier getGeckolibModel() {
        return parseModelResource(this.entityData.get(GEO_MODEL));
    }

    public void setGeckolibAnimation(Identifier animation) {
        this.entityData.set(GEO_ANIMATION, animation != null ? animation.toString() : "");
    }

    public Identifier getGeckolibAnimation() {
        return parseAnimationResource(this.entityData.get(GEO_ANIMATION));
    }

    public void setGeckolibTexture(Identifier texture) {
        this.entityData.set(GEO_TEXTURE, texture != null ? texture.toString() : "");
    }

    public Identifier getGeckolibTexture() {
        return parseTextureResource(this.entityData.get(GEO_TEXTURE));
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
        controllers.add(new AnimationController<GeckoNpcEntity>("main", 0, this::mainPredicate));
        controllers.add(new AnimationController<GeckoNpcEntity>("blink", 0, this::blinkPredicate));
    }

    private PlayState mainPredicate(AnimationTest<GeckoNpcEntity> state) {
        String expression = getExpression();
        if (expression != null && !expression.isBlank()) {
            if ("idle_interact".equalsIgnoreCase(expression)) {
                String cycle = ((this.tickCount / IDLE_INTERACT_SWAP_TICKS) % 2 == 0) ? "idle" : "interact";
                if ("interact".equalsIgnoreCase(cycle)) {
                    state.controller().setAnimation(RawAnimation.begin()
                            .then("interact", LoopType.PLAY_ONCE));
                } else {
                    state.controller().setAnimation(RawAnimation.begin().thenLoop("idle"));
                }
                return PlayState.CONTINUE;
            }
            if (!"idle".equalsIgnoreCase(expression)) {
                state.controller().setAnimation(RawAnimation.begin().thenLoop(expression));
                return PlayState.CONTINUE;
            }
        }
        String fallbackAnimation = state.isMoving() ? "walk" : "idle";
        state.controller().setAnimation(RawAnimation.begin().thenLoop(fallbackAnimation));
        return PlayState.CONTINUE;
    }

    private PlayState blinkPredicate(AnimationTest<GeckoNpcEntity> state) {
        if (consumeBlink()) {
            state.controller().setAnimation(RawAnimation.begin().then("blink", LoopType.PLAY_ONCE));
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
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("NpcId", getNpcId());
        output.putString("GeckoModel", this.entityData.get(GEO_MODEL));
        output.putString("GeckoAnimation", this.entityData.get(GEO_ANIMATION));
        output.putString("GeckoTexture", this.entityData.get(GEO_TEXTURE));
        output.putString("Expression", getExpression());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("NpcId").ifPresent(this::setNpcId);
        input.getString("GeckoModel").ifPresent(value -> this.entityData.set(GEO_MODEL, value));
        input.getString("GeckoAnimation").ifPresent(value -> this.entityData.set(GEO_ANIMATION, value));
        input.getString("GeckoTexture").ifPresent(value -> this.entityData.set(GEO_TEXTURE, value));
        input.getString("Expression").ifPresent(this::setExpression);
    }

    private static Identifier parseModelResource(String raw) {
        return parseGeckoResource(raw, true);
    }

    private static Identifier parseAnimationResource(String raw) {
        return parseGeckoResource(raw, false);
    }

    private static Identifier parseTextureResource(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Identifier.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static Identifier parseGeckoResource(String raw, boolean model) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Identifier id = Identifier.parse(raw);
            String path = normalizeGeckoPath(id.getPath(), model);
            return id.withPath(path);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeGeckoPath(String path, boolean model) {
        if (path == null || path.isBlank()) {
            return path;
        }

        String normalized = path;
        if (normalized.startsWith("geckolib/models/")) {
            normalized = normalized.substring("geckolib/models/".length());
        } else if (normalized.startsWith("geckolib/animations/")) {
            normalized = normalized.substring("geckolib/animations/".length());
        } else if (normalized.startsWith("geo/")) {
            normalized = normalized.substring("geo/".length());
        } else if (normalized.startsWith("animations/")) {
            normalized = normalized.substring("animations/".length());
        }

        if (normalized.endsWith(".geo.json")) {
            normalized = normalized.substring(0, normalized.length() - ".geo.json".length());
        } else if (normalized.endsWith(".animation.json")) {
            normalized = normalized.substring(0, normalized.length() - ".animation.json".length());
        } else if (normalized.endsWith(".geo")) {
            normalized = normalized.substring(0, normalized.length() - ".geo".length());
        } else if (normalized.endsWith(".animation")) {
            normalized = normalized.substring(0, normalized.length() - ".animation".length());
        } else if (normalized.endsWith(".json")) {
            normalized = normalized.substring(0, normalized.length() - ".json".length());
        }

        if (normalized.isBlank()) {
            return path;
        }

        if (model && normalized.startsWith("animations/")) {
            return normalized.substring("animations/".length());
        }
        if (!model && normalized.startsWith("geo/")) {
            return normalized.substring("geo/".length());
        }

        return normalized;
    }
}


