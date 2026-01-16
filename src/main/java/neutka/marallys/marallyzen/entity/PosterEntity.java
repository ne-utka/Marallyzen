package neutka.marallys.marallyzen.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.audio.MarallyzenSounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PosterEntity extends Entity {
    private static final Logger LOGGER = LoggerFactory.getLogger(PosterEntity.class);
    private static final EntityDataAccessor<Integer> DATA_POSTER_NUMBER = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RETURN_ANIMATION_START_TICK = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> DATA_ORIGIN_POS = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_START_TICK = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.INT);
    // Target position coordinates (synced separately for immediate client access)
    // Using FLOAT instead of DOUBLE as EntityDataSerializers doesn't have DOUBLE
    private static final EntityDataAccessor<Float> DATA_TARGET_X = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_FLIPPED = SynchedEntityData.defineId(PosterEntity.class, EntityDataSerializers.BOOLEAN);
    
    private BlockPos originPos;
    private BlockState originalBlockState;
    private int posterNumber;
    private State currentState = State.FLYING_OUT;
    private String oldposterVariant = "default"; // For oldposter (ID 11): "default", "alive", "band", "dead"
    private String targetPlayerName = ""; // Player name for head display on oldposter variants (single name for alive/dead)
    private java.util.List<String> targetPlayerNames = new java.util.ArrayList<>(); // List of player names for band variant (up to 3)
    
    // Animation state - synchronized for client-side smooth animation
    private Vec3 targetPosition;
    private Vec3 startPosition;
    private int animationStartTick = -1; // Tick when animation started (-1 means not initialized)
    public static final float FLY_OUT_DURATION_TICKS = 15.0f; // 0.75 seconds at 20 TPS - fast and smooth
    
    // Return animation state
    private Vec3 returnStartPosition; // Position when returning animation starts
    private int returnAnimationStartTick = -1; // Tick when return animation started
    public static final float RETURN_DURATION_TICKS = 15.0f; // Same duration as fly out for consistency
    
    // Viewing state - slow sway animation (client-side only)
    private float swayOffset = 0.0f;
    // Stronger wind-like sway for full posters
    private static final float SWAY_SPEED = 0.035f; // Slightly faster
    private static final float SWAY_AMPLITUDE = 6.0f; // Degrees (was 3.0)
    
    // Flip animation state
    private boolean targetFlipped = false; // Target flip state
    private float currentFlipRotation = 0.0f; // Current rotation (0-180 degrees)
    private int flipAnimationStartTick = -1; // Tick when flip animation started (-1 means not animating)
    private static final float FLIP_ANIMATION_DURATION = 15.0f; // 15 ticks (0.75 seconds) for smooth flip animation
    
    // Return state
    private static final double RETURN_DISTANCE_THRESHOLD = 5.0;
    
    // Client-only flag: if true, this entity exists only on the client and is not synchronized with server
    private boolean isClientOnly = false;
    
    // Text data (client-only) - DEPRECATED: use textTexture instead
    private String posterText = "";
    private String posterTitle = "";
    private String posterAuthor = "";
    
    // Pre-rendered text texture (client-only)
    @org.jetbrains.annotations.Nullable
    private net.minecraft.resources.ResourceLocation textTexture = null;
    
    // Pre-rendered text texture for back side (client-only)
    @org.jetbrains.annotations.Nullable
    private net.minecraft.resources.ResourceLocation textTextureBack = null;

    public void setPosterText(String text) { this.posterText = text; }
    public String getPosterText() { return posterText != null ? posterText : ""; }
    
    public void setPosterTitle(String title) { this.posterTitle = title; }
    public String getPosterTitle() { return posterTitle != null ? posterTitle : ""; }
    
    public void setPosterAuthor(String author) { this.posterAuthor = author; }
    public String getPosterAuthor() { return posterAuthor != null ? posterAuthor : ""; }
    
    /**
     * Sets the text texture for this poster.
     * Called when text data is available to create a pre-rendered texture.
     */
    public void setTextTexture(@org.jetbrains.annotations.Nullable net.minecraft.resources.ResourceLocation texture) {
        this.textTexture = texture;
    }
    
    /**
     * Gets the text texture for this poster.
     * Returns null if no text texture is set.
     */
    @org.jetbrains.annotations.Nullable
    public net.minecraft.resources.ResourceLocation getTextTexture() {
        return textTexture;
    }
    
    /**
     * Gets the text texture for the back side of this poster.
     * Returns null if no back texture is set.
     */
    @org.jetbrains.annotations.Nullable
    public net.minecraft.resources.ResourceLocation getTextTextureBack() {
        return textTextureBack;
    }
    
    /**
     * Sets text data and creates a pre-rendered texture.
     * This is the new way to set poster text - it creates a texture once.
     * @param frontData Text data for the front side
     * @param backData Text data for the back side (can be null)
     */
    public void setText(neutka.marallys.marallyzen.client.poster.text.PosterTextData frontData, 
                       @org.jetbrains.annotations.Nullable neutka.marallys.marallyzen.client.poster.text.PosterTextData backData) {
        LOGGER.warn("========== PosterEntity.setText() CALLED ==========");
        LOGGER.warn("Front Data: {}", frontData);
        LOGGER.warn("Back Data: {}", backData);
        this.textTexture = neutka.marallys.marallyzen.client.poster.text.PosterTextTextureCache.getOrCreate(frontData);
        if (backData != null) {
            this.textTextureBack = neutka.marallys.marallyzen.client.poster.text.PosterTextTextureCache.getOrCreate(backData);
        } else {
            // Create default test text for back side
            neutka.marallys.marallyzen.client.poster.text.PosterTextData defaultBackData = 
                new neutka.marallys.marallyzen.client.poster.text.PosterTextData(
                    "Оборотная сторона",
                    java.util.List.of("Это тестовый текст", "для обратной стороны плаката"),
                    "Тест",
                    frontData.style()
                );
            this.textTextureBack = neutka.marallys.marallyzen.client.poster.text.PosterTextTextureCache.getOrCreate(defaultBackData);
        }
        LOGGER.warn("Result: textTexture = {}, textTextureBack = {}", this.textTexture, this.textTextureBack);
        LOGGER.warn("========== PosterEntity.setText() DONE ==========");
    }
    
    /**
     * Sets text data for front side only (backward compatibility).
     */
    public void setText(neutka.marallys.marallyzen.client.poster.text.PosterTextData data) {
        setText(data, null);
    }

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
                if (state.id == id) return state;
            }
            return FLYING_OUT;
        }
    }
    
    public PosterEntity(EntityType<? extends PosterEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        // Set a reasonable hitbox size for interaction (1x1 block)
        this.setBoundingBox(this.getBoundingBox().inflate(0.5, 0.5, 0.5));
    }
    
    public void initializeFromBlock(BlockPos originPos, int posterNumber, BlockState originalBlockState) {
        this.originPos = originPos;
        this.posterNumber = posterNumber;
        this.originalBlockState = originalBlockState;
        this.entityData.set(DATA_POSTER_NUMBER, posterNumber);
        this.entityData.set(DATA_STATE, State.FLYING_OUT.getId());
        this.entityData.set(DATA_ORIGIN_POS, originPos); // Sync to client immediately
        // Initialize flip animation state
        this.targetFlipped = false;
        this.currentFlipRotation = 0.0f;
        this.flipAnimationStartTick = -1;
        
        // Set starting position (block center)
        Vec3 blockCenter = Vec3.atCenterOf(originPos);
        this.setPos(blockCenter.x, blockCenter.y, blockCenter.z);
        this.startPosition = blockCenter;
        
        // Calculate target position (1.5 blocks away from wall towards nearest player)
        // Find nearest player to determine direction
        Vec3 targetPos = blockCenter;
        if (level() instanceof ServerLevel serverLevel) {
            net.minecraft.world.entity.player.Player nearestPlayer = serverLevel.getNearestPlayer(
                this, 10.0
            );
            if (nearestPlayer != null) {
                Vec3 playerEyePos = nearestPlayer.getEyePosition();
                Vec3 direction = playerEyePos.subtract(blockCenter).normalize();
                // Move 1.5 blocks in the direction of the player
                targetPos = blockCenter.add(direction.scale(1.5));
            } else {
                // No player found, move forward from facing direction
                Direction facing = originalBlockState.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
                Vec3 forward = Vec3.atLowerCornerOf(facing.getNormal()).scale(1.5);
                targetPos = blockCenter.add(forward);
            }
        } else if (level().isClientSide) {
            // Client-side: use local player's position
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player != null) {
                Vec3 playerEyePos = minecraft.player.getEyePosition();
                Vec3 direction = playerEyePos.subtract(blockCenter).normalize();
                // Move 1.5 blocks in the direction of the player
                targetPos = blockCenter.add(direction.scale(1.5));
            } else {
                // No player found, move forward from facing direction
                Direction facing = originalBlockState.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
                Vec3 forward = Vec3.atLowerCornerOf(facing.getNormal()).scale(1.5);
                targetPos = blockCenter.add(forward);
            }
        }
        this.targetPosition = targetPos;
        
        // Sync targetPosition to client immediately via EntityDataAccessor
        // Convert double to float for EntityDataAccessor
        this.entityData.set(DATA_TARGET_X, (float)targetPos.x);
        this.entityData.set(DATA_TARGET_Y, (float)targetPos.y);
        this.entityData.set(DATA_TARGET_Z, (float)targetPos.z);
        
        // Mark animation start (use tickCount, which is synchronized)
        this.animationStartTick = this.tickCount;
        // Sync to client immediately via EntityDataAccessor
        this.entityData.set(DATA_ANIMATION_START_TICK, this.tickCount);
        this.currentState = State.FLYING_OUT;
        
        LOGGER.info("[SERVER] PosterEntity initialized: startPos={}, targetPos={}, animationStartTick={}, tickCount={}", 
            startPosition, targetPosition, animationStartTick, this.tickCount);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_POSTER_NUMBER, 1);
        builder.define(DATA_STATE, State.FLYING_OUT.getId());
        builder.define(DATA_RETURN_ANIMATION_START_TICK, -1); // -1 means not set yet
        builder.define(DATA_ORIGIN_POS, BlockPos.ZERO); // Default value, will be set on initialization
        builder.define(DATA_ANIMATION_START_TICK, -1); // -1 means not set yet
        builder.define(DATA_TARGET_X, 0.0f); // Default value, will be set on initialization
        builder.define(DATA_TARGET_Y, 0.0f);
        builder.define(DATA_TARGET_Z, 0.0f);
        builder.define(DATA_FLIPPED, false); // Default: not flipped
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OriginX") && tag.contains("OriginY") && tag.contains("OriginZ")) {
            BlockPos loadedPos = new BlockPos(
                tag.getInt("OriginX"),
                tag.getInt("OriginY"),
                tag.getInt("OriginZ")
            );
            this.originPos = loadedPos;
            // Also sync to entityData if value is valid
            if (!loadedPos.equals(BlockPos.ZERO)) {
                this.entityData.set(DATA_ORIGIN_POS, loadedPos);
            }
            LOGGER.info("[CLIENT] readAdditionalSaveData: loaded originPos={}", originPos);
        } else {
            LOGGER.warn("[CLIENT] readAdditionalSaveData: originPos not found in NBT!");
        }
        this.posterNumber = tag.getInt("PosterNumber");
        this.entityData.set(DATA_POSTER_NUMBER, posterNumber);
        
        if (tag.contains("State")) {
            this.currentState = State.fromId(tag.getInt("State"));
            this.entityData.set(DATA_STATE, currentState.getId());
        }
        
        if (tag.contains("AnimationStartTick")) {
            int tick = tag.getInt("AnimationStartTick");
            this.animationStartTick = tick;
            // Also sync to entityData if value is valid
            if (tick >= 0) {
                this.entityData.set(DATA_ANIMATION_START_TICK, tick);
            }
        }
        
        // Restore start and target positions
        if (tag.contains("StartX") && tag.contains("StartY") && tag.contains("StartZ")) {
            this.startPosition = new Vec3(
                tag.getDouble("StartX"),
                tag.getDouble("StartY"),
                tag.getDouble("StartZ")
            );
        }
        if (tag.contains("TargetX") && tag.contains("TargetY") && tag.contains("TargetZ")) {
            Vec3 loadedTarget = new Vec3(
                tag.getDouble("TargetX"),
                tag.getDouble("TargetY"),
                tag.getDouble("TargetZ")
            );
            this.targetPosition = loadedTarget;
            // Also sync to entityData if value is valid (convert double to float)
            this.entityData.set(DATA_TARGET_X, (float)loadedTarget.x);
            this.entityData.set(DATA_TARGET_Y, (float)loadedTarget.y);
            this.entityData.set(DATA_TARGET_Z, (float)loadedTarget.z);
        }
        
        // Restore return animation state
        if (tag.contains("ReturnStartX") && tag.contains("ReturnStartY") && tag.contains("ReturnStartZ")) {
            this.returnStartPosition = new Vec3(
                tag.getDouble("ReturnStartX"),
                tag.getDouble("ReturnStartY"),
                tag.getDouble("ReturnStartZ")
            );
        }
        if (tag.contains("ReturnAnimationStartTick")) {
            int tick = tag.getInt("ReturnAnimationStartTick");
            this.returnAnimationStartTick = tick;
            // Also sync to entityData if value is valid
            if (tick >= 0) {
                this.entityData.set(DATA_RETURN_ANIMATION_START_TICK, tick);
            }
        }
        
        // Restore BlockState if available
        if (tag.contains("BlockState") && level() != null) {
            var result = BlockState.CODEC.decode(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("BlockState"));
            result.result().ifPresent(pair -> this.originalBlockState = pair.getFirst());
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (originPos != null) {
            tag.putInt("OriginX", originPos.getX());
            tag.putInt("OriginY", originPos.getY());
            tag.putInt("OriginZ", originPos.getZ());
        }
        tag.putInt("PosterNumber", posterNumber);
        tag.putInt("State", currentState.getId());
        tag.putInt("AnimationStartTick", animationStartTick);
        
        // Save start and target positions
        if (startPosition != null) {
            tag.putDouble("StartX", startPosition.x);
            tag.putDouble("StartY", startPosition.y);
            tag.putDouble("StartZ", startPosition.z);
        }
        if (targetPosition != null) {
            tag.putDouble("TargetX", targetPosition.x);
            tag.putDouble("TargetY", targetPosition.y);
            tag.putDouble("TargetZ", targetPosition.z);
        }
        
        // Save return animation state
        if (returnStartPosition != null) {
            tag.putDouble("ReturnStartX", returnStartPosition.x);
            tag.putDouble("ReturnStartY", returnStartPosition.y);
            tag.putDouble("ReturnStartZ", returnStartPosition.z);
        }
        tag.putInt("ReturnAnimationStartTick", returnAnimationStartTick);
        
        // Save BlockState
        if (originalBlockState != null && level() != null) {
            var result = BlockState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, originalBlockState);
            result.result().ifPresent(nbt -> tag.put("BlockState", nbt));
        }
    }
    
    public int getPosterNumber() {
        return entityData.get(DATA_POSTER_NUMBER);
    }

    public Direction getFacing() {
        if (originalBlockState == null) {
            return null;
        }
        if (!originalBlockState.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            return null;
        }
        return originalBlockState.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
    }
    
    public String getOldposterVariant() {
        return oldposterVariant != null ? oldposterVariant : "default";
    }
    
    public void setOldposterVariant(String variant) {
        this.oldposterVariant = variant != null ? variant : "default";
    }
    
    public String getTargetPlayerName() {
        return targetPlayerName != null ? targetPlayerName : "";
    }
    
    public void setTargetPlayerName(String targetPlayerName) {
        this.targetPlayerName = targetPlayerName != null ? targetPlayerName : "";
    }
    
    /**
     * Gets the list of player names for band variant.
     * Returns up to 3 names.
     */
    public java.util.List<String> getTargetPlayerNames() {
        if (targetPlayerNames == null) {
            targetPlayerNames = new java.util.ArrayList<>();
        }
        return targetPlayerNames;
    }
    
    /**
     * Sets the list of player names for band variant.
     * Only first 3 names are stored.
     */
    public void setTargetPlayerNames(java.util.List<String> names) {
        if (targetPlayerNames == null) {
            targetPlayerNames = new java.util.ArrayList<>();
        } else {
            targetPlayerNames.clear();
        }
        if (names != null) {
            // Only store up to 3 names
            for (int i = 0; i < Math.min(3, names.size()); i++) {
                String name = names.get(i);
                if (name != null && !name.isEmpty()) {
                    targetPlayerNames.add(name);
                }
            }
        }
    }
    
    public State getCurrentState() {
        return State.fromId(entityData.get(DATA_STATE));
    }
    
    public boolean isFlipped() {
        return entityData.get(DATA_FLIPPED);
    }
    
    public void setFlipped(boolean flipped) {
        // Only start animation if value actually changed and not currently animating
        if (targetFlipped == flipped) {
            return; // No change, don't do anything
        }
        
        // Don't start new animation if one is already in progress
        if (flipAnimationStartTick >= 0) {
            // Check if animation is still running
            float elapsedTicks = (this.tickCount - flipAnimationStartTick);
            if (elapsedTicks < FLIP_ANIMATION_DURATION) {
                return; // Animation still in progress, ignore new request
            }
        }
        
        boolean oldFlipped = targetFlipped;
        entityData.set(DATA_FLIPPED, flipped);
        targetFlipped = flipped;
        // Start animation
        if (level().isClientSide) {
            flipAnimationStartTick = this.tickCount;
            level().playLocalSound(
                getX(),
                getY(),
                getZ(),
                MarallyzenSounds.POSTER_SHAKE.get(),
                SoundSource.BLOCKS,
                1.0f,
                1.0f,
                false
            );
            LOGGER.warn("PosterEntity: Started flip animation, oldFlipped={}, newFlipped={}, tickCount={}, currentRotation={}", 
                oldFlipped, targetFlipped, this.tickCount, currentFlipRotation);
        }
    }
    
    /**
     * Gets the current flip rotation for smooth animation (0-180 degrees).
     * Returns interpolated value during animation using ease-in-out for smoothness.
     */
    public float getFlipRotation(float partialTick) {
        if (!level().isClientSide) {
            return isFlipped() ? 180.0f : 0.0f;
        }
        
        // If not animating, return current state
        if (flipAnimationStartTick < 0) {
            return currentFlipRotation;
        }
        
        // Calculate animation progress
        float elapsedTicks = (this.tickCount - flipAnimationStartTick) + partialTick;
        float progress = elapsedTicks / FLIP_ANIMATION_DURATION;
        progress = Mth.clamp(progress, 0.0f, 1.0f);
        
        // Use ease-in-out for smooth animation
        float easedProgress = easeInOutCubic(progress);
        
        // Interpolate between start and target rotation
        float startRotation = targetFlipped ? 0.0f : 180.0f; // Opposite of target
        float targetRotation = targetFlipped ? 180.0f : 0.0f;
        float currentRotation = Mth.lerp(easedProgress, startRotation, targetRotation);
        
        // Update currentFlipRotation for next frame
        currentFlipRotation = currentRotation;
        
        // If animation is complete, reset animation state
        if (progress >= 1.0f) {
            flipAnimationStartTick = -1;
            currentFlipRotation = targetRotation;
        }
        
        return currentRotation;
    }
    
    /**
     * Ease-in-out cubic function for smooth animation.
     * Starts slow, speeds up in middle, slows down at end.
     */
    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }
    
    // Getters for client-side animation
    public Vec3 getStartPosition() { return startPosition; }
    public Vec3 getTargetPosition() { 
        // Prefer synchronized data from EntityDataAccessor (available on client immediately)
        Float syncedX = this.entityData.get(DATA_TARGET_X);
        Float syncedY = this.entityData.get(DATA_TARGET_Y);
        Float syncedZ = this.entityData.get(DATA_TARGET_Z);
        if (syncedX != null && syncedY != null && syncedZ != null && 
            (syncedX != 0.0f || syncedY != 0.0f || syncedZ != 0.0f)) {
            // Update local field for consistency (convert float to double for Vec3)
            Vec3 synced = new Vec3(syncedX, syncedY, syncedZ);
            this.targetPosition = synced;
            return synced;
        }
        // Fallback to local field (for NBT-loaded entities before sync)
        return targetPosition;
    }
    public int getAnimationStartTick() { 
        // Prefer synchronized data from EntityDataAccessor (available on client immediately)
        int syncedTick = this.entityData.get(DATA_ANIMATION_START_TICK);
        if (syncedTick >= 0) {
            return syncedTick;
        }
        // Fallback to local field (for NBT-loaded entities before sync)
        return animationStartTick;
    }
    public BlockPos getOriginPos() { 
        // Prefer synchronized data from EntityDataAccessor (available on client immediately)
        BlockPos synced = this.entityData.get(DATA_ORIGIN_POS);
        if (synced != null && !synced.equals(BlockPos.ZERO)) {
            // Update local field for consistency
            this.originPos = synced;
            return synced;
        }
        // Fallback to local field (for NBT-loaded entities before sync)
        return originPos;
    }
    
    // Getters for return animation
    public Vec3 getReturnStartPosition() { return returnStartPosition; }
    public int getReturnAnimationStartTick() { 
        // Prefer synchronized data from EntityDataAccessor (available on client immediately)
        // Fall back to local field if entityData is not available yet or value is -1 and local field has a valid value
        int syncedValue = this.entityData.get(DATA_RETURN_ANIMATION_START_TICK);
        if (syncedValue >= 0) {
            return syncedValue;
        }
        // Fallback to local field (for NBT-loaded entities before sync)
        return returnAnimationStartTick;
    }
    
    // Setter for client-side animation start (used when animation start time is not synced)
    // Allows setting if not set yet, or if current animation hasn't started (too early) or already finished
    public void setAnimationStartTick(int tick) {
        if (level().isClientSide && this.currentState == State.FLYING_OUT) {
            if (this.animationStartTick < 0) {
                // Not set yet, set it
                this.animationStartTick = tick;
            } else {
                // Already set, check if animation hasn't started or already finished
                int elapsedTicks = this.tickCount - this.animationStartTick;
                if (elapsedTicks < 0 || elapsedTicks >= FLY_OUT_DURATION_TICKS) {
                    // Animation hasn't started yet (negative elapsed) or already finished, allow reset
                    this.animationStartTick = tick;
                }
            }
        }
    }
    
    // Setter for return animation start tick (used when animation start time is not synced)
    public void setReturnAnimationStartTick(int tick) {
        if (level().isClientSide && this.returnAnimationStartTick < 0 && this.currentState == State.RETURNING) {
            this.returnAnimationStartTick = tick;
        }
    }
    
    /**
     * Sets whether this entity is client-only (not synchronized with server).
     */
    public void setClientOnly(boolean clientOnly) {
        this.isClientOnly = clientOnly;
    }
    
    /**
     * Returns whether this entity is client-only (not synchronized with server).
     */
    public boolean isClientOnly() {
        return isClientOnly;
    }
    
    
    @Override
    public void tick() {
        super.tick();
        
        // Always sync currentState from synced data
        this.currentState = getCurrentState();
        
        if (level().isClientSide) {
            // Client-side: update sway animation and ensure positions are initialized IMMEDIATELY
            // Position animation is handled in renderer for maximum smoothness
            if (currentState == State.VIEWING) {
                swayOffset += SWAY_SPEED;
            }
            
            // Sync targetFlipped from entityData
            boolean syncedFlipped = isFlipped();
            if (targetFlipped != syncedFlipped && flipAnimationStartTick < 0) {
                // Only update if not currently animating
                targetFlipped = syncedFlipped;
                flipAnimationStartTick = this.tickCount;
            }
            
            // For client-only entities, handle all state transitions on client
            if (isClientOnly) {
                switch (currentState) {
                    case FLYING_OUT:
                        tickFlyingOutClient();
                        break;
                    case VIEWING:
                        tickViewingClient();
                        break;
                    case RETURNING:
                        tickReturningClient();
                        break;
                }
                return;
            }
            
            // For server-synced entities: only update animation state, positions handled in renderer
            // CRITICAL: Initialize startPosition only if null (from originPos as fallback)
            // DO NOT recompute targetPosition here - it must come from server via NBT
            // Recomputing it would override the correct direction-based calculation from server
            if (currentState == State.FLYING_OUT && originPos != null) {
                Vec3 blockCenter = Vec3.atCenterOf(originPos);
                
                if (startPosition == null) {
                    startPosition = blockCenter;
                    // Only log once to avoid spam
                    if (this.tickCount <= 1) {
                        LOGGER.debug("[CLIENT TICK] Initialized startPosition={} from originPos={}, tickCount={}", 
                            startPosition, originPos, this.tickCount);
                    }
                }
                
                // DO NOT recompute targetPosition - it should come from server via NBT
                // If targetPosition is null, the renderer will use entity.position() as fallback
            }
            
            // Initialize return animation positions if needed
            // NOTE: Do NOT set returnAnimationStartTick here - it should come from NBT synced from server
            // Setting it to tickCount here causes desync because server started animation earlier
            if (currentState == State.RETURNING) {
                // Don't require originPos - we can use startPosition as fallback in renderer
                if (returnStartPosition == null) {
                    // Use targetPosition (where poster was viewing) as start, same as server
                    if (targetPosition != null) {
                        returnStartPosition = targetPosition;
                    } else {
                        // Fallback to current entity position
                        returnStartPosition = this.position();
                    }
                }
                
                // Only set returnAnimationStartTick if it's truly not set AND we're sure NBT won't sync it
                // Otherwise, let NBT sync handle it (from readAdditionalSaveData)
                // The renderer will handle initialization if needed
            }
            
            return;
        }
        
        // Server-side logic
        switch (currentState) {
            case FLYING_OUT:
                tickFlyingOut();
                break;
            case VIEWING:
                tickViewing();
                break;
            case RETURNING:
                tickReturning();
                break;
        }
    }
    
    private void tickFlyingOut() {
        if (startPosition == null || targetPosition == null) {
            // Transition to viewing if we don't have start/target
            LOGGER.warn("[SERVER] tickFlyingOut: positions are null, transitioning to VIEWING");
            transitionToViewing();
            return;
        }
        
        // Check if animation duration has elapsed (use >= with tolerance for smooth transition)
        int elapsedTicks = this.tickCount - this.animationStartTick;
        if (elapsedTicks >= FLY_OUT_DURATION_TICKS) {
            // Animation complete, transition to viewing
            LOGGER.info("[SERVER] tickFlyingOut: animation complete (elapsedTicks={}, duration={}), transitioning to VIEWING", 
                elapsedTicks, FLY_OUT_DURATION_TICKS);
            
            // Set final position BEFORE transitioning to ensure smooth sync
            // Client will finish animation smoothly and then match this position
            this.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
            transitionToViewing();
        } else {
            // Keep entity at start position on server during animation
            // Client will compute smooth position in renderer
            this.setPos(startPosition.x, startPosition.y, startPosition.z);
            
            // Log every 5 ticks
            if (this.tickCount % 5 == 0) {
                LOGGER.debug("[SERVER] tickFlyingOut: elapsedTicks={}/{}, keeping at startPos={}", 
                    elapsedTicks, (int)FLY_OUT_DURATION_TICKS, startPosition);
            }
        }
    }
    
    private void tickViewing() {
        // Sway animation is now client-side only
        
        // Check if player moved away
        if (originPos != null && level() instanceof ServerLevel serverLevel) {
            net.minecraft.world.entity.player.Player nearestPlayer = serverLevel.getNearestPlayer(
                this, RETURN_DISTANCE_THRESHOLD + 1.0
            );
            
            if (nearestPlayer == null) {
                // No player nearby, transition to returning
                transitionToReturning();
                return;
            }
            
            double distance = nearestPlayer.position().distanceTo(Vec3.atCenterOf(originPos));
            if (distance >= RETURN_DISTANCE_THRESHOLD) {
                transitionToReturning();
                return;
            }
        }
    }
    
    private void tickReturning() {
        if (originPos == null) {
            LOGGER.warn("[SERVER] tickReturning: originPos is null, discarding entity.");
            discard();
            return;
        }
        
        if (returnStartPosition == null) {
            // Initialize return animation if not already set
            // Use targetPosition (where poster was viewing) as start, same as in transitionToReturning
            if (targetPosition != null) {
                returnStartPosition = targetPosition;
            } else {
                // Fallback to current position
                returnStartPosition = this.position();
            }
            returnAnimationStartTick = this.tickCount;
            LOGGER.info("[SERVER] tickReturning: Initialized return animation: startPos={}, tickCount={}", 
                returnStartPosition, this.tickCount);
        }
        
        Vec3 target = Vec3.atCenterOf(originPos);
        
        // Check if animation duration has elapsed
        int elapsedTicks = this.tickCount - this.returnAnimationStartTick;
        if (elapsedTicks >= RETURN_DURATION_TICKS) {
            // Animation complete, restore block and discard entity
            LOGGER.info("[SERVER] tickReturning: animation complete (elapsedTicks={}, duration={}), restoring block and discarding", 
                elapsedTicks, RETURN_DURATION_TICKS);
            this.setPos(target.x, target.y, target.z);
            restoreBlock();
            discard();
        } else {
            // Keep entity at start position on server during animation
            // Client will compute smooth position in renderer
            this.setPos(returnStartPosition.x, returnStartPosition.y, returnStartPosition.z);
            
            if (this.tickCount % 5 == 0) {
                LOGGER.debug("[SERVER] tickReturning: elapsedTicks={}/{}, keeping at returnStartPos={}", 
                    elapsedTicks, RETURN_DURATION_TICKS, returnStartPosition);
            }
        }
    }
    
    private void transitionToViewing() {
        this.currentState = State.VIEWING;
        this.entityData.set(DATA_STATE, State.VIEWING.getId());
        
        // Set position to target for server-side syncing
        if (targetPosition != null) {
            this.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
        }
        
        LOGGER.info("[SERVER] Transitioned to VIEWING state: targetPos={}, tickCount={}", targetPosition, this.tickCount);
    }
    
    private void transitionToReturning() {
        this.currentState = State.RETURNING;
        this.entityData.set(DATA_STATE, State.RETURNING.getId());
        
        // Initialize return animation: start from targetPosition (where poster was viewing), target is origin
        // This ensures smooth transition from VIEWING to RETURNING
        if (targetPosition != null) {
            this.returnStartPosition = targetPosition;
        } else {
            // Fallback to current position if targetPosition is null
            this.returnStartPosition = this.position();
        }
        this.returnAnimationStartTick = this.tickCount;
        // Sync to client immediately via EntityDataAccessor
        this.entityData.set(DATA_RETURN_ANIMATION_START_TICK, this.tickCount);
        
        LOGGER.info("[SERVER] Transitioned to RETURNING state: returnStartPos={}, originPos={}, tickCount={}", 
            returnStartPosition, originPos, this.tickCount);
    }
    
    // Client-only entity tick methods
    private void tickFlyingOutClient() {
        if (startPosition == null || targetPosition == null) {
            transitionToViewingClient();
            return;
        }
        
        int elapsedTicks = this.tickCount - this.animationStartTick;
        if (elapsedTicks >= FLY_OUT_DURATION_TICKS) {
            transitionToViewingClient();
        }
    }
    
    private void tickViewingClient() {
        // In viewing state, check if player has moved away
        // If player is more than 5 blocks away, trigger return animation
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player != null && targetPosition != null) {
            Vec3 playerPos = minecraft.player.getEyePosition();
            double distance = playerPos.distanceTo(targetPosition);
            
            // If player is too far away, trigger return
            if (distance > RETURN_DISTANCE_THRESHOLD) {
                transitionToReturningClient();
            }
        }
    }
    
    private void tickReturningClient() {
        if (originPos == null) {
            LOGGER.warn("[CLIENT] tickReturningClient: originPos is null, discarding entity");
            discard();
            return;
        }
        
        if (returnStartPosition == null) {
            if (targetPosition != null) {
                returnStartPosition = targetPosition;
            } else {
                returnStartPosition = this.position();
            }
            returnAnimationStartTick = this.tickCount;
            this.entityData.set(DATA_RETURN_ANIMATION_START_TICK, this.tickCount);
        }
        
        int elapsedTicks = this.tickCount - this.returnAnimationStartTick;
        if (elapsedTicks >= RETURN_DURATION_TICKS) {
            // Animation complete, remove client entity
            LOGGER.debug("[CLIENT] tickReturningClient: animation complete, removing client entity");
            neutka.marallys.marallyzen.client.ClientPosterManager.removeClientPoster(originPos);
            discard();
        }
    }
    
    private void transitionToViewingClient() {
        this.currentState = State.VIEWING;
        this.entityData.set(DATA_STATE, State.VIEWING.getId());
        
        if (targetPosition != null) {
            this.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
        }
        
        LOGGER.debug("[CLIENT] Transitioned to VIEWING state (client-only)");
    }
    
    private void transitionToReturningClient() {
        this.currentState = State.RETURNING;
        this.entityData.set(DATA_STATE, State.RETURNING.getId());
        
        if (targetPosition != null) {
            this.returnStartPosition = targetPosition;
        } else {
            this.returnStartPosition = this.position();
        }
        this.returnAnimationStartTick = this.tickCount;
        this.entityData.set(DATA_RETURN_ANIMATION_START_TICK, this.tickCount);
        
        LOGGER.debug("[CLIENT] Transitioned to RETURNING state (client-only)");
    }
    
    /**
     * Manually triggers return animation for client-only entities.
     * Called when player moves away or presses a key to close the poster.
     */
    public void triggerReturn() {
        if (isClientOnly && currentState == State.VIEWING) {
            transitionToReturningClient();
        }
    }
    
    private void restoreBlock() {
        if (originPos == null || originalBlockState == null) {
            return;
        }
        
        if (level() instanceof ServerLevel serverLevel) {
            // Restore the block
            serverLevel.setBlock(originPos, originalBlockState, 3);
        }
    }
    
    public float getSwayRotationX() {
        // Sway is client-side only, check state from synced data on client
        State state = getCurrentState();
        if (state != State.VIEWING) {
            return 0.0f;
        }
        return Mth.sin(swayOffset) * SWAY_AMPLITUDE;
    }
    
    public float getSwayRotationY() {
        // Sway is client-side only, check state from synced data on client
        State state = getCurrentState();
        if (state != State.VIEWING) {
            return 0.0f;
        }
        return Mth.cos(swayOffset * 1.3f) * SWAY_AMPLITUDE;
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true; // Always render
    }
}
