package neutka.marallys.marallyzen.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class DictaphoneEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_STATE =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> DATA_ORIGIN_POS =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_START_TICK =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RETURN_ANIMATION_START_TICK =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FACE_YAW =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FACE_PITCH =
        SynchedEntityData.defineId(DictaphoneEntity.class, EntityDataSerializers.FLOAT);

    public enum State {
        FLYING_OUT(0),
        VIEWING(1),
        RETURNING(2);

        private final int id;

        State(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static State fromId(int id) {
            for (State state : values()) {
                if (state.id == id) {
                    return state;
                }
            }
            return FLYING_OUT;
        }
    }

    public static final float FLY_OUT_DURATION_TICKS = 25.0f;
    public static final float RETURN_DURATION_TICKS = FLY_OUT_DURATION_TICKS;
    private static final double RETURN_DISTANCE_THRESHOLD = 5.0;

    private BlockPos originPos;
    private BlockState originalBlockState;
    private Vec3 startPosition;
    private Vec3 targetPosition;
    private Vec3 returnStartPosition;
    private int animationStartTick = -1;
    private int returnAnimationStartTick = -1;
    private State currentState = State.FLYING_OUT;
    private boolean isClientOnly = false;
    private float faceYaw = 0.0f;
    private float facePitch = 0.0f;

    public DictaphoneEntity(EntityType<? extends DictaphoneEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setBoundingBox(this.getBoundingBox().inflate(0.5, 0.5, 0.5));
    }

    public void initializeFromBlock(BlockPos pos, BlockState state) {
        this.originPos = pos;
        this.originalBlockState = state;
        this.entityData.set(DATA_ORIGIN_POS, pos);
        this.entityData.set(DATA_STATE, State.FLYING_OUT.getId());

        Vec3 blockCenter = Vec3.atCenterOf(pos);
        this.setPos(blockCenter.x, blockCenter.y, blockCenter.z);
        this.startPosition = blockCenter;

        Vec3 targetPos = blockCenter;
        if (level().isClientSide()) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null) {
                Vec3 playerEyePos = minecraft.player.getEyePosition();
                Vec3 direction = playerEyePos.subtract(blockCenter).normalize();
                targetPos = blockCenter.add(direction.scale(1.5));
                double horiz = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
                this.faceYaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
                this.facePitch = (float) (-Math.toDegrees(Math.atan2(direction.y, horiz)));
            } else if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                Vec3 forward = Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(1.5);
                targetPos = blockCenter.add(forward);
                this.faceYaw = switch (facing) {
                    case EAST -> 90.0f;
                    case SOUTH -> 180.0f;
                    case WEST -> 270.0f;
                    default -> 0.0f;
                };
                this.facePitch = 0.0f;
            }
        }

        this.targetPosition = targetPos;
        this.entityData.set(DATA_TARGET_X, (float) targetPos.x);
        this.entityData.set(DATA_TARGET_Y, (float) targetPos.y);
        this.entityData.set(DATA_TARGET_Z, (float) targetPos.z);
        this.entityData.set(DATA_FACE_YAW, this.faceYaw);
        this.entityData.set(DATA_FACE_PITCH, this.facePitch);

        this.animationStartTick = this.tickCount;
        this.entityData.set(DATA_ANIMATION_START_TICK, this.tickCount);
        this.currentState = State.FLYING_OUT;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STATE, State.FLYING_OUT.getId());
        builder.define(DATA_ORIGIN_POS, BlockPos.ZERO);
        builder.define(DATA_ANIMATION_START_TICK, -1);
        builder.define(DATA_RETURN_ANIMATION_START_TICK, -1);
        builder.define(DATA_TARGET_X, 0.0f);
        builder.define(DATA_TARGET_Y, 0.0f);
        builder.define(DATA_TARGET_Z, 0.0f);
        builder.define(DATA_FACE_YAW, 0.0f);
        builder.define(DATA_FACE_PITCH, 0.0f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (originPos != null) {
            output.putInt("OriginX", originPos.getX());
            output.putInt("OriginY", originPos.getY());
            output.putInt("OriginZ", originPos.getZ());
        }
        output.putInt("State", getCurrentState().getId());
        output.putInt("AnimationStartTick", getAnimationStartTick());
        output.putInt("ReturnAnimationStartTick", getReturnAnimationStartTick());

        if (startPosition != null) {
            output.putDouble("StartX", startPosition.x);
            output.putDouble("StartY", startPosition.y);
            output.putDouble("StartZ", startPosition.z);
        }
        Vec3 target = getTargetPosition();
        if (target != null) {
            output.putDouble("TargetX", target.x);
            output.putDouble("TargetY", target.y);
            output.putDouble("TargetZ", target.z);
        }
        if (returnStartPosition != null) {
            output.putDouble("ReturnStartX", returnStartPosition.x);
            output.putDouble("ReturnStartY", returnStartPosition.y);
            output.putDouble("ReturnStartZ", returnStartPosition.z);
        }

        if (originalBlockState != null && level() != null) {
            output.store("BlockState", net.minecraft.world.level.block.state.BlockState.CODEC, originalBlockState);
        }
        output.putFloat("FaceYaw", this.faceYaw);
        output.putFloat("FacePitch", this.facePitch);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        var originX = input.getInt("OriginX");
        var originY = input.getInt("OriginY");
        var originZ = input.getInt("OriginZ");
        if (originX.isPresent() && originY.isPresent() && originZ.isPresent()) {
            BlockPos loadedPos = new BlockPos(originX.get(), originY.get(), originZ.get());
            this.originPos = loadedPos;
            this.entityData.set(DATA_ORIGIN_POS, loadedPos);
        }

        input.getInt("State").ifPresent(stateId -> {
            this.currentState = State.fromId(stateId);
            this.entityData.set(DATA_STATE, this.currentState.getId());
        });

        input.getInt("AnimationStartTick").ifPresent(tick -> {
            this.animationStartTick = tick;
            if (this.animationStartTick >= 0) {
                this.entityData.set(DATA_ANIMATION_START_TICK, this.animationStartTick);
            }
        });

        input.getInt("ReturnAnimationStartTick").ifPresent(tick -> {
            this.returnAnimationStartTick = tick;
            if (this.returnAnimationStartTick >= 0) {
                this.entityData.set(DATA_RETURN_ANIMATION_START_TICK, this.returnAnimationStartTick);
            }
        });

        double startX = input.getDoubleOr("StartX", Double.NaN);
        double startY = input.getDoubleOr("StartY", Double.NaN);
        double startZ = input.getDoubleOr("StartZ", Double.NaN);
        if (!Double.isNaN(startX) && !Double.isNaN(startY) && !Double.isNaN(startZ)) {
            this.startPosition = new Vec3(startX, startY, startZ);
        }

        double targetX = input.getDoubleOr("TargetX", Double.NaN);
        double targetY = input.getDoubleOr("TargetY", Double.NaN);
        double targetZ = input.getDoubleOr("TargetZ", Double.NaN);
        if (!Double.isNaN(targetX) && !Double.isNaN(targetY) && !Double.isNaN(targetZ)) {
            this.targetPosition = new Vec3(targetX, targetY, targetZ);
            this.entityData.set(DATA_TARGET_X, (float) this.targetPosition.x);
            this.entityData.set(DATA_TARGET_Y, (float) this.targetPosition.y);
            this.entityData.set(DATA_TARGET_Z, (float) this.targetPosition.z);
        }

        float faceYaw = input.getFloatOr("FaceYaw", Float.NaN);
        if (!Float.isNaN(faceYaw)) {
            this.faceYaw = faceYaw;
            this.entityData.set(DATA_FACE_YAW, this.faceYaw);
        }

        float facePitch = input.getFloatOr("FacePitch", Float.NaN);
        if (!Float.isNaN(facePitch)) {
            this.facePitch = facePitch;
            this.entityData.set(DATA_FACE_PITCH, this.facePitch);
        }

        double returnX = input.getDoubleOr("ReturnStartX", Double.NaN);
        double returnY = input.getDoubleOr("ReturnStartY", Double.NaN);
        double returnZ = input.getDoubleOr("ReturnStartZ", Double.NaN);
        if (!Double.isNaN(returnX) && !Double.isNaN(returnY) && !Double.isNaN(returnZ)) {
            this.returnStartPosition = new Vec3(returnX, returnY, returnZ);
        }

        input.read("BlockState", net.minecraft.world.level.block.state.BlockState.CODEC)
            .ifPresent(state -> this.originalBlockState = state);
    }

    public void setClientOnly(boolean clientOnly) {
        this.isClientOnly = clientOnly;
    }

    public boolean isClientOnly() {
        return isClientOnly;
    }

    public void startReturningClient() {
        if (!level().isClientSide() || !isClientOnly) {
            return;
        }
        if (getCurrentState() != State.RETURNING) {
            transitionToReturningClient();
        }
    }

    public State getCurrentState() {
        return State.fromId(this.entityData.get(DATA_STATE));
    }

    public BlockPos getOriginPos() {
        BlockPos synced = this.entityData.get(DATA_ORIGIN_POS);
        if (synced != null && !synced.equals(BlockPos.ZERO)) {
            this.originPos = synced;
            return synced;
        }
        return originPos;
    }

    public BlockState getOriginalBlockState() {
        return originalBlockState;
    }

    public Vec3 getStartPosition() {
        return startPosition;
    }

    public Vec3 getTargetPosition() {
        int syncedTick = this.entityData.get(DATA_ANIMATION_START_TICK);
        if (syncedTick >= 0) {
            float x = this.entityData.get(DATA_TARGET_X);
            float y = this.entityData.get(DATA_TARGET_Y);
            float z = this.entityData.get(DATA_TARGET_Z);
            return new Vec3(x, y, z);
        }
        return targetPosition;
    }

    public Vec3 getReturnStartPosition() {
        return returnStartPosition;
    }

    public float getFaceYaw() {
        return this.entityData.get(DATA_FACE_YAW);
    }

    public float getFacePitch() {
        return this.entityData.get(DATA_FACE_PITCH);
    }

    public int getAnimationStartTick() {
        int synced = this.entityData.get(DATA_ANIMATION_START_TICK);
        return synced >= 0 ? synced : animationStartTick;
    }

    public int getReturnAnimationStartTick() {
        int synced = this.entityData.get(DATA_RETURN_ANIMATION_START_TICK);
        return synced >= 0 ? synced : returnAnimationStartTick;
    }

    @Override
    public void tick() {
        super.tick();
        this.currentState = getCurrentState();

        if (!level().isClientSide() || !isClientOnly) {
            return;
        }
        switch (currentState) {
            case FLYING_OUT -> tickFlyingOutClient();
            case VIEWING -> tickViewingClient();
            case RETURNING -> tickReturningClient();
        }
    }

    private void tickFlyingOutClient() {
        if (startPosition == null || getTargetPosition() == null) {
            transitionToViewingClient();
            return;
        }
        int elapsedTicks = this.tickCount - getAnimationStartTick();
        if (elapsedTicks >= FLY_OUT_DURATION_TICKS) {
            transitionToViewingClient();
        }
    }

    private void tickViewingClient() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        Vec3 targetPos = getTargetPosition();
        if (minecraft.player != null && targetPos != null) {
            Vec3 playerPos = minecraft.player.getEyePosition();
            double distance = playerPos.distanceTo(targetPos);
            if (distance > RETURN_DISTANCE_THRESHOLD) {
                transitionToReturningClient();
            }
        }
    }

    private void tickReturningClient() {
        BlockPos origin = getOriginPos();
        if (origin == null) {
            discard();
            return;
        }
        if (returnStartPosition == null) {
            Vec3 targetPos = getTargetPosition();
            returnStartPosition = targetPos != null ? targetPos : this.position();
            returnAnimationStartTick = this.tickCount;
            this.entityData.set(DATA_RETURN_ANIMATION_START_TICK, this.tickCount);
        }

        int elapsedTicks = this.tickCount - getReturnAnimationStartTick();
        if (elapsedTicks >= RETURN_DURATION_TICKS) {
            neutka.marallys.marallyzen.client.ClientDictaphoneManager.removeClientDictaphone(origin);
            discard();
        }
    }

    private void transitionToViewingClient() {
        this.currentState = State.VIEWING;
        this.entityData.set(DATA_STATE, State.VIEWING.getId());
        if (level().isClientSide() && isClientOnly) {
            BlockPos origin = getOriginPos();
            if (origin != null) {
            }
        }
        Vec3 targetPos = getTargetPosition();
        if (targetPos != null) {
            this.setPos(targetPos.x, targetPos.y, targetPos.z);
        }
    }

    private void transitionToReturningClient() {
        this.currentState = State.RETURNING;
        this.entityData.set(DATA_STATE, State.RETURNING.getId());
        Vec3 targetPos = getTargetPosition();
        this.returnStartPosition = targetPos != null ? targetPos : this.position();
        this.returnAnimationStartTick = this.tickCount;
        this.entityData.set(DATA_RETURN_ANIMATION_START_TICK, this.tickCount);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Client-only visual entity; ignore damage.
        return false;
    }
}
