package neutka.marallys.marallyzen.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import com.mojang.math.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.server.DecoratedPotCarryManager;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public class DecoratedPotCarryEntity extends Entity {
    public static final ResourceKey<net.minecraft.world.damagesource.DamageType> HEAVY_POT_DAMAGE =
        ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Marallyzen.MODID, "heavy_pot"));
    public static final float CARRY_DISTANCE = 3.0f;
    public static final float CARRY_Y_OFFSET = -0.2f;
    public static final float CARRY_WALL_PADDING = 0.25f;
    public static final float CARRY_RADIUS = 0.35f;
    public static final float CARRY_HEIGHT = 0.7f;
    public static final float CARRY_MIN_Y_OFFSET = 0.2f;
    public static final float THROW_SPEED = 0.35f;
    public static final float THROW_LIFT = 0.08f;
    public static final float DROP_SPEED = 0.08f;
    public static final float THROW_GRAVITY = 0.04f;
    public static final float THROW_AIR_DRAG = 0.98f;
    public static final float LANDING_SPEED_THRESHOLD = 0.02f;
    public static final int LANDING_GRACE_TICKS = 5;
    public static final String ROTATION_TAG = "marallyzen_pot_yaw";
    public static final AABB POT_BOUNDS_RAW = Blocks.DECORATED_POT.defaultBlockState()
        .getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
        .bounds();
    public static final AABB POT_BOUNDS = new AABB(
        POT_BOUNDS_RAW.minX,
        POT_BOUNDS_RAW.minY,
        POT_BOUNDS_RAW.minZ,
        POT_BOUNDS_RAW.maxX,
        POT_BOUNDS_RAW.maxY,
        POT_BOUNDS_RAW.maxZ
    );
    public static final float COLLISION_WIDTH = (float) (POT_BOUNDS.maxX - POT_BOUNDS.minX);
    public static final float COLLISION_HEIGHT = (float) (POT_BOUNDS.maxY - POT_BOUNDS.minY);
    public static final double COLLISION_EPS = 0.05;
    public static final double CARRY_CLIENT_LERP = 0.25;
    public static final double CARRY_CLIENT_MAX_STEP = 0.3;
    public static final double CARRY_VISUAL_LERP = 1.0;
    public static final double CARRY_VISUAL_MAX_STEP = 1000.0;

    private static final EntityDataAccessor<Integer> DATA_MODE =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_YAW =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GRAB_OFFSET_X =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GRAB_OFFSET_Y =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GRAB_OFFSET_Z =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> DATA_BLOCK_ENTITY =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
        SynchedEntityData.defineId(DecoratedPotCarryEntity.class, EntityDataSerializers.BLOCK_STATE);

    public enum Mode {
        CARRIED(0),
        THROWN(1),
        RESTING(2);

        private final int id;

        Mode(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Mode fromId(int id) {
            for (Mode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return CARRIED;
        }
    }

    private int landingTicks = 0;
    private BlockEntity cachedRenderEntity;
    private int cachedRenderTagHash = 0;
    private BlockPos cachedRenderPos;
    private boolean wasOnGround;
    private boolean wasHorizontalCollision;
    private Vec3 clientCarryPos;
    private boolean clientCarryPosValid;
    private Vec3 clientCarryRenderPrev;
    private Vec3 clientCarryRenderCurr;
    private boolean clientCarryRenderValid;
    private boolean crushedThisFall;

    public DecoratedPotCarryEntity(EntityType<? extends DecoratedPotCarryEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void initializeFromBlock(BlockPos pos, BlockState state, CompoundTag blockEntityTag, float yaw, UUID owner) {
        BlockState normalized = normalizeFacing(state);
        this.entityData.set(DATA_BLOCK_STATE, normalized);
        this.entityData.set(DATA_BLOCK_ENTITY, blockEntityTag == null ? new CompoundTag() : blockEntityTag.copy());
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, 0.0f);
        this.entityData.set(DATA_ROLL, 0.0f);
        this.entityData.set(DATA_OWNER, Optional.ofNullable(owner));
        setGrabOffset(Vec3.ZERO);
        setMode(Mode.CARRIED);
        this.setPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, Mode.CARRIED.getId());
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_YAW, 0.0f);
        builder.define(DATA_PITCH, 0.0f);
        builder.define(DATA_ROLL, 0.0f);
        builder.define(DATA_GRAB_OFFSET_X, 0.0f);
        builder.define(DATA_GRAB_OFFSET_Y, 0.0f);
        builder.define(DATA_GRAB_OFFSET_Z, 0.0f);
        builder.define(DATA_BLOCK_ENTITY, new CompoundTag());
        builder.define(DATA_BLOCK_STATE, Blocks.DECORATED_POT.defaultBlockState());
    }

    public Mode getMode() {
        return Mode.fromId(this.entityData.get(DATA_MODE));
    }

    public void setMode(Mode mode) {
        setMode(mode, null);
    }

    private void setMode(Mode mode, Vec3 initialVelocity) {
        this.entityData.set(DATA_MODE, mode.getId());
        if (mode == Mode.CARRIED) {
            this.noPhysics = true;
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            resetImpactState();
            crushedThisFall = false;
        } else if (mode == Mode.THROWN) {
            this.noPhysics = false;
            this.setNoGravity(false);
            if (initialVelocity != null) {
                this.setDeltaMovement(initialVelocity);
            }
            resetImpactState();
            crushedThisFall = false;
        } else if (mode == Mode.RESTING) {
            this.noPhysics = false;
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            wasOnGround = onGround();
            wasHorizontalCollision = horizontalCollision;
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_MODE.equals(key)) {
            Mode mode = getMode();
            if (mode == Mode.CARRIED) {
                this.noPhysics = true;
                this.setNoGravity(true);
                this.setDeltaMovement(Vec3.ZERO);
            } else if (mode == Mode.THROWN) {
                this.noPhysics = false;
                this.setNoGravity(false);
            } else if (mode == Mode.RESTING) {
                this.noPhysics = false;
                this.setNoGravity(true);
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    public UUID getOwnerUuid() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    public void setOwnerUuid(UUID owner) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(owner));
    }

    public Vec3 getGrabOffset() {
        return new Vec3(
            this.entityData.get(DATA_GRAB_OFFSET_X),
            this.entityData.get(DATA_GRAB_OFFSET_Y),
            this.entityData.get(DATA_GRAB_OFFSET_Z)
        );
    }

    public void setGrabOffset(Vec3 offset) {
        Vec3 safe = offset == null ? Vec3.ZERO : offset;
        this.entityData.set(DATA_GRAB_OFFSET_X, (float) safe.x);
        this.entityData.set(DATA_GRAB_OFFSET_Y, (float) safe.y);
        this.entityData.set(DATA_GRAB_OFFSET_Z, (float) safe.z);
    }

    public float getPotYaw() {
        return this.entityData.get(DATA_YAW);
    }

    public float getPotPitch() {
        return this.entityData.get(DATA_PITCH);
    }

    public float getPotRoll() {
        return this.entityData.get(DATA_ROLL);
    }

    public BlockState getStoredBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE);
    }

    public CompoundTag getStoredBlockEntityTag() {
        return this.entityData.get(DATA_BLOCK_ENTITY);
    }

    public boolean isCarriedBy(UUID owner) {
        UUID current = getOwnerUuid();
        return current != null && current.equals(owner) && getMode() == Mode.CARRIED;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return getMode() == Mode.RESTING ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (getMode() != Mode.RESTING) {
            return InteractionResult.PASS;
        }
        boolean picked = DecoratedPotCarryManager.tryPickupEntity(serverPlayer, this, Vec3.ZERO);
        return picked ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hitPos, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return getMode() == Mode.RESTING ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (getMode() != Mode.RESTING) {
            return InteractionResult.PASS;
        }
        Vec3 offsetWorld = hitPos;
        Vec3 offsetLocal = rotateAroundY(offsetWorld, -getCarryYaw());
        boolean picked = DecoratedPotCarryManager.tryPickupEntity(serverPlayer, this, offsetLocal);
        return picked ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public void throwFromPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 target = computeGrabPoint(player).add(0.0, -0.3, 0.0);
        Vec3 pivot = resolveCarryPivot(level(), target, direction);
        this.setPos(pivot.x, pivot.y, pivot.z);
        Vec3 impulse = direction.scale(THROW_SPEED).add(0.0, THROW_LIFT, 0.0);
        setMode(Mode.THROWN, impulse);
        this.hasImpulse = true;
        this.entityData.set(DATA_YAW, getPotYaw());
        this.entityData.set(DATA_ROLL, getPotRoll());
        landingTicks = 0;
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.CRIT,
                getX(),
                getY() + 0.4,
                getZ(),
                8,
                0.15,
                0.15,
                0.15,
                0.2
            );
        }
    }

    public void dropFromCarry(ServerPlayer player) {
        if (player == null) {
            return;
        }
        setMode(Mode.THROWN, new Vec3(0.0, -DROP_SPEED, 0.0));
        this.hasImpulse = true;
        landingTicks = 0;
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = new Vec3(getX(), getY(), getZ());
            serverLevel.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 6, 0.08, 0.04, 0.08, 0.02);
        }
    }

    @Override
    public void tick() {
        super.tick();
        Mode mode = getMode();
        if (mode == Mode.THROWN) {
            tickThrown();
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (!level().isClientSide) {
                handleImpactParticles();
                if (!crushedThisFall) {
                    crushEntitiesIfFalling();
                }
            }
        } else if (mode == Mode.CARRIED) {
            if (level().isClientSide) {
                tickCarriedClient();
            } else {
                tickCarriedServer();
            }
        } else if (mode == Mode.RESTING) {
            if (level().isClientSide) {
                clientCarryPosValid = false;
            } else {
                tickResting();
            }
        } else if (level().isClientSide) {
            clientCarryPosValid = false;
        }
    }

    private void tickCarriedServer() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) {
            discard();
            return;
        }
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 target = computeGrabPoint(owner).add(0.0, -0.3, 0.0);
        Vec3 pivot = resolveCarryPivot(level(), target, look);
        setPos(pivot.x, pivot.y, pivot.z);
    }

    private void tickCarriedClient() {
        LocalPlayer owner = Minecraft.getInstance().player;
        if (owner == null || !isCarriedBy(owner.getUUID())) {
            clientCarryPosValid = false;
            clientCarryRenderValid = false;
            return;
        }
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 target = computeGrabPoint(owner).add(0.0, -0.3, 0.0);
        Vec3 pivot = resolveCarryPivot(level(), target, look);
        if (!clientCarryPosValid) {
            clientCarryPos = position();
            clientCarryPosValid = true;
        }
        Vec3 delta = pivot.subtract(clientCarryPos);
        double len = delta.length();
        if (len > CARRY_CLIENT_MAX_STEP && len > 1.0E-6) {
            delta = delta.scale(CARRY_CLIENT_MAX_STEP / len);
        }
        clientCarryPos = clientCarryPos.add(delta.scale(CARRY_CLIENT_LERP));
        if (!clientCarryRenderValid) {
            clientCarryRenderPrev = clientCarryPos;
            clientCarryRenderCurr = clientCarryPos;
            clientCarryRenderValid = true;
            return;
        }
        clientCarryRenderPrev = clientCarryRenderCurr;
        clientCarryRenderCurr = clientCarryPos;
    }

    public Vec3 getClientCarryPos() {
        return clientCarryPosValid ? clientCarryPos : null;
    }

    public Vec3 getClientCarryRenderPos(float partialTick) {
        if (!clientCarryRenderValid || clientCarryRenderPrev == null || clientCarryRenderCurr == null) {
            return null;
        }
        double x = Mth.lerp(partialTick, clientCarryRenderPrev.x, clientCarryRenderCurr.x);
        double y = Mth.lerp(partialTick, clientCarryRenderPrev.y, clientCarryRenderCurr.y);
        double z = Mth.lerp(partialTick, clientCarryRenderPrev.z, clientCarryRenderCurr.z);
        return new Vec3(x, y, z);
    }

    public Vec3 getCarryRenderPivot(Player owner) {
        if (owner == null) {
            return null;
        }
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 target = computeGrabPoint(owner).add(0.0, -0.3, 0.0);
        return resolveCarryPivot(level(), target, look);
    }

    public void updateCarriedPosition(ServerPlayer owner) {
        if (owner == null) {
            return;
        }
        if (getMode() != Mode.CARRIED) {
            return;
        }
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 target = computeGrabPoint(owner);
        Vec3 offset = rotateAroundY(getGrabOffset(), getCarryYaw());
        Vec3 center = target.subtract(offset);
        Vec3 pivot = resolveCarryPivot(level(), center, look);
        setPos(pivot.x, pivot.y, pivot.z);
    }

    private static Vec3 toBlockPivot(Vec3 center) {
        return new Vec3(center.x - 0.5, center.y, center.z - 0.5);
    }

    private Vec3 resolveCarryPivot(Level level, Vec3 desiredCenter, Vec3 look) {
        Vec3 pivot = toBlockPivotForCarry(desiredCenter, look);
        if (level.noCollision(getCollisionBoxAt(pivot))) {
            return pivot;
        }
        double max = Math.max(0.0, CARRY_DISTANCE);
        double step = 0.1;
        for (double back = step; back <= max; back += step) {
            Vec3 candidateCenter = desiredCenter.subtract(look.scale(back));
            Vec3 candidatePivot = toBlockPivotForCarry(candidateCenter, look);
            if (level.noCollision(getCollisionBoxAt(candidatePivot))) {
                return candidatePivot;
            }
        }
        return position();
    }

    private Vec3 computeGrabPoint(Player owner) {
        Vec3 eye = owner.getEyePosition();
        Vec3 dir = owner.getLookAngle().normalize();
        Vec3 desired = eye.add(dir.scale(CARRY_DISTANCE));
        HitResult hit = owner.level().clip(new ClipContext(
            eye,
            desired,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            owner
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z).normalize();
            double halfX = POT_BOUNDS.maxX - 0.5;
            double halfZ = POT_BOUNDS.maxZ - 0.5;
            double extra = CARRY_WALL_PADDING + COLLISION_EPS;
            if (horizontal.lengthSqr() < 1.0E-6) {
                return hit.getLocation().subtract(dir.scale(CARRY_WALL_PADDING));
            }
            double push = extra + Math.abs(horizontal.x) * halfX + Math.abs(horizontal.z) * halfZ;
            return hit.getLocation().subtract(dir.scale(push));
        }
        return desired;
    }

    private static Vec3 toBlockPivotForCarry(Vec3 center, Vec3 look) {
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return new Vec3(center.x, center.y, center.z);
        }
        return toBlockPivot(center);
    }

    private AABB getCollisionBoxAt(Vec3 pivot) {
        AABB b = POT_BOUNDS;
        return new AABB(
            pivot.x + b.minX,
            pivot.y + b.minY,
            pivot.z + b.minZ,
            pivot.x + b.maxX,
            pivot.y + b.maxY,
            pivot.z + b.maxZ
        );
    }

    private Vec3 computeGrabPoint(ServerPlayer owner) {
        Vec3 eye = owner.getEyePosition();
        Vec3 dir = owner.getLookAngle().normalize();
        Vec3 desired = eye.add(dir.scale(CARRY_DISTANCE));
        HitResult hit = owner.level().clip(new ClipContext(
            eye,
            desired,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            owner
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();
            Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
            if (horizontal.lengthSqr() < 1.0E-6) {
                return hitPos;
            }
            Vec3 h = horizontal.normalize();
            double halfX = (POT_BOUNDS.maxX - POT_BOUNDS.minX) * 0.5;
            double halfZ = (POT_BOUNDS.maxZ - POT_BOUNDS.minZ) * 0.5;
            double clearance = (CARRY_WALL_PADDING + COLLISION_EPS)
                + Math.abs(h.x) * halfX
                + Math.abs(h.z) * halfZ;
            return hitPos.subtract(h.scale(clearance));
        }
        return desired;
    }

    private float getCarryYaw() {
        return getPotYaw() + getPotRoll();
    }

    private static Vec3 rotateAroundY(Vec3 vec, float degrees) {
        Vector3f temp = new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
        temp.rotate(Axis.YP.rotationDegrees(degrees));
        return new Vec3(temp.x, temp.y, temp.z);
    }

    private void tickThrown() {
        Vec3 velocity = getDeltaMovement();
        if (!onGround()) {
            velocity = velocity.add(0.0, -THROW_GRAVITY, 0.0);
            velocity = velocity.scale(THROW_AIR_DRAG);
        } else {
            velocity = new Vec3(
                velocity.x * 0.15,
                0.0,
                velocity.z * 0.15
            );
            if (velocity.lengthSqr() < 0.0004 && !level().isClientSide) {
                settle();
            }
        }
        this.setDeltaMovement(velocity);
    }

    private void crushEntitiesIfFalling() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 velocity = getDeltaMovement();
        if (velocity.y >= -0.2) {
            return;
        }
        AABB box = getBoundingBox().inflate(0.0, 0.05, 0.0);
        var targets = serverLevel.getEntities(this, box, entity -> entity instanceof net.minecraft.world.entity.LivingEntity);
        if (targets.isEmpty()) {
            return;
        }
        for (Entity entity : targets) {
            if (entity == null) {
                continue;
            }
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            double speed = Math.min(1.0, Math.abs(velocity.y) * 2.5);
            float damage = (float) (2.0 + speed * 6.0);
            entity.hurt(serverLevel.damageSources().source(HEAVY_POT_DAMAGE, this), damage);
            Vec3 dir = entity.position().subtract(position());
            if (dir.lengthSqr() < 1.0E-6) {
                dir = new Vec3(0.0, 0.0, 1.0);
            }
            Vec3 push = dir.normalize().scale(0.3);
            entity.push(push.x, 0.4, push.z);
        }
        crushedThisFall = true;
    }

    private void handleImpactParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            wasOnGround = onGround();
            wasHorizontalCollision = horizontalCollision;
            return;
        }
        boolean onGroundNow = onGround();
        boolean horizontalNow = horizontalCollision;
        double speedSq = getDeltaMovement().lengthSqr();

        if (!wasOnGround && onGroundNow) {
            spawnGroundImpactParticles(serverLevel, speedSq);
        } else if (!wasHorizontalCollision && horizontalNow && !onGroundNow) {
            spawnSideImpactParticles(serverLevel, speedSq);
        }
        wasOnGround = onGroundNow;
        wasHorizontalCollision = horizontalNow;
    }

    private void spawnGroundImpactParticles(ServerLevel level, double speedSq) {
        Vec3 pos = new Vec3(getX(), getY(), getZ());
        int count = speedSq > 0.15 ? 12 : 8;
        double spread = speedSq > 0.15 ? 0.2 : 0.12;
        BlockState state = getStoredBlockState();
        if (state == null || state.isAir()) {
            state = Blocks.DECORATED_POT.defaultBlockState();
        }
        level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, state),
            pos.x,
            pos.y,
            pos.z,
            count,
            spread,
            0.05,
            spread,
            0.02
        );
    }

    private void spawnSideImpactParticles(ServerLevel level, double speedSq) {
        Vec3 pos = position();
        int count = speedSq > 0.15 ? 8 : 4;
        double spread = speedSq > 0.15 ? 0.15 : 0.08;
        BlockState state = getStoredBlockState();
        if (state == null || state.isAir()) {
            state = Blocks.DECORATED_POT.defaultBlockState();
        }
        level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, state),
            pos.x,
            pos.y,
            pos.z,
            count,
            spread,
            spread * 0.5,
            spread,
            0.02
        );
    }

    private void spawnRestParticles(ServerLevel level) {
        Vec3 pos = new Vec3(getX(), getY(), getZ());
        int count = 4;
        double spread = 0.08;
        BlockState state = getStoredBlockState();
        if (state == null || state.isAir()) {
            state = Blocks.DECORATED_POT.defaultBlockState();
        }
        level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, state),
            pos.x,
            pos.y,
            pos.z,
            count,
            spread,
            0.04,
            spread,
            0.01
        );
    }

    public void settleFromCarry() {
        if (level().isClientSide) {
            return;
        }
        setMode(Mode.RESTING);
        this.entityData.set(DATA_PITCH, 0.0f);
        this.entityData.set(DATA_ROLL, (float) (random.nextGaussian() * 10.0));
    }

    private void tickResting() {
    }

    private void settle() {
        setMode(Mode.RESTING);
        if (level() instanceof ServerLevel serverLevel) {
            spawnRestParticles(serverLevel);
        }
        this.entityData.set(DATA_PITCH, 0.0f);
        this.entityData.set(DATA_ROLL, (float) (random.nextGaussian() * 10.0));
    }

    private void resetImpactState() {
        wasOnGround = false;
        wasHorizontalCollision = false;
    }

    private void dropAsItem(ServerLevel level, BlockPos pos) {
        ItemStack stack = new ItemStack(Blocks.DECORATED_POT.asItem());
        CompoundTag tag = getStoredBlockEntityTag();
        if (tag != null && !tag.isEmpty()) {
            CompoundTag copy = tag.copy();
            copy.remove("x");
            copy.remove("y");
            copy.remove("z");
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(copy));
        }
        this.spawnAtLocation(stack);
        Marallyzen.LOGGER.debug("Decorated pot placement failed at {}, dropped item", pos);
    }

    private ServerPlayer getOwnerPlayer() {
        UUID owner = getOwnerUuid();
        if (owner == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(owner);
    }

    private BlockState normalizeFacing(BlockState state) {
        if (state != null && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        }
        return state;
    }

    public BlockEntity getOrCreateRenderEntity(Level level) {
        if (level == null) {
            return null;
        }
        CompoundTag tag = getStoredBlockEntityTag();
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        BlockPos pos = BlockPos.ZERO;
        int hash = tag.hashCode();
        if (cachedRenderEntity != null && hash == cachedRenderTagHash && pos.equals(cachedRenderPos)) {
            return cachedRenderEntity;
        }
        CompoundTag copy = tag.copy();
        copy.putInt("x", pos.getX());
        copy.putInt("y", pos.getY());
        copy.putInt("z", pos.getZ());
        BlockState state = getStoredBlockState();
        BlockEntity blockEntity = BlockEntity.loadStatic(pos, normalizeFacing(state), copy, level.registryAccess());
        if (blockEntity != null) {
            blockEntity.setLevel(level);
        }
        cachedRenderEntity = blockEntity;
        cachedRenderTagHash = hash;
        cachedRenderPos = pos;
        return cachedRenderEntity;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", getMode().getId());
        UUID owner = getOwnerUuid();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putFloat("Yaw", getPotYaw());
        tag.putFloat("Pitch", getPotPitch());
        tag.putFloat("Roll", getPotRoll());
        Vec3 grabOffset = getGrabOffset();
        tag.putFloat("GrabOffsetX", (float) grabOffset.x);
        tag.putFloat("GrabOffsetY", (float) grabOffset.y);
        tag.putFloat("GrabOffsetZ", (float) grabOffset.z);
        BlockState state = getStoredBlockState();
        if (state != null) {
            var result = BlockState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, state);
            result.result().ifPresent(nbt -> tag.put("BlockState", nbt));
        }
        CompoundTag beTag = getStoredBlockEntityTag();
        if (beTag != null && !beTag.isEmpty()) {
            tag.put("BlockEntity", beTag);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Mode")) {
            setMode(Mode.fromId(tag.getInt("Mode")));
        }
        if (tag.contains("Owner")) {
            this.entityData.set(DATA_OWNER, Optional.of(tag.getUUID("Owner")));
        }
        if (tag.contains("Yaw")) {
            this.entityData.set(DATA_YAW, tag.getFloat("Yaw"));
        }
        if (tag.contains("Pitch")) {
            this.entityData.set(DATA_PITCH, tag.getFloat("Pitch"));
        }
        if (tag.contains("Roll")) {
            this.entityData.set(DATA_ROLL, tag.getFloat("Roll"));
        }
        if (tag.contains("GrabOffsetX") || tag.contains("GrabOffsetY") || tag.contains("GrabOffsetZ")) {
            this.entityData.set(DATA_GRAB_OFFSET_X, tag.getFloat("GrabOffsetX"));
            this.entityData.set(DATA_GRAB_OFFSET_Y, tag.getFloat("GrabOffsetY"));
            this.entityData.set(DATA_GRAB_OFFSET_Z, tag.getFloat("GrabOffsetZ"));
        }
        if (tag.contains("BlockState")) {
            var result = BlockState.CODEC.decode(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("BlockState"));
            result.result().ifPresent(pair -> this.entityData.set(DATA_BLOCK_STATE, pair.getFirst()));
        }
        if (tag.contains("BlockEntity")) {
            this.entityData.set(DATA_BLOCK_ENTITY, tag.getCompound("BlockEntity"));
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        AABB b = POT_BOUNDS;
        return EntityDimensions.fixed(
            (float) (b.maxX - b.minX),
            (float) (b.maxY - b.minY)
        );
    }

    @Override
    protected AABB makeBoundingBox() {
        AABB b = POT_BOUNDS;
        return new AABB(
            getX() + b.minX,
            getY() + b.minY,
            getZ() + b.minZ,
            getX() + b.maxX,
            getY() + b.maxY,
            getZ() + b.maxZ
        ).inflate(COLLISION_EPS, 0.0, COLLISION_EPS);
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isRemoved() && getMode() != Mode.CARRIED;
    }

    @Override
    public boolean isPushable() {
        return getMode() == Mode.THROWN;
    }

    @Override
    public void push(double x, double y, double z) {
        if (getMode() == Mode.RESTING) {
            return;
        }
        super.push(x, y, z);
    }

    @Override
    public boolean isPickable() {
        return getMode() == Mode.RESTING;
    }

    @Override
    public float getPickRadius() {
        return 0.35f;
    }

    @Override
    public boolean isAttackable() {
        return getMode() != Mode.CARRIED;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return true;
        }
        if (getMode() == Mode.CARRIED) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            if (level() instanceof ServerLevel serverLevel) {
                BlockState state = getStoredBlockState();
                if (state == null || state.isAir()) {
                    state = Blocks.DECORATED_POT.defaultBlockState();
                }
                serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    getX(),
                    getY(),
                    getZ(),
                    12,
                    0.2,
                    0.2,
                    0.2,
                    0.02
                );
                serverLevel.playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    SoundEvents.DECORATED_POT_BREAK,
                    SoundSource.BLOCKS,
                    1.0f,
                    1.0f
                );
            }
            if (player.isCreative()) {
                discard();
            } else if (level() instanceof ServerLevel serverLevel) {
                dropAsItem(serverLevel, BlockPos.containing(position()));
                discard();
            }
            return true;
        }
        return false;
    }
}
