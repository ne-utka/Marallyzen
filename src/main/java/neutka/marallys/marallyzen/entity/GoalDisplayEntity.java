package neutka.marallys.marallyzen.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import neutka.marallys.marallyzen.goals.GoalScript;

import java.util.Map;

/**
 * Lightweight visual-only entity that carries synchronized goal display data.
 */
public class GoalDisplayEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_GOAL_ID =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> DATA_PROGRESS =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_TARGET =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<String> DATA_TITLE =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_LINES =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TITLE_COLOR =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LINES_COLOR =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FLOAT_AMPLITUDE =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FLOAT_SPEED =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FLOAT_HEIGHT =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_VARS =
            SynchedEntityData.defineId(GoalDisplayEntity.class, EntityDataSerializers.STRING);

    public GoalDisplayEntity(EntityType<? extends GoalDisplayEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_GOAL_ID, "");
        builder.define(DATA_PROGRESS, 0L);
        builder.define(DATA_TARGET, 0L);
        builder.define(DATA_TITLE, "");
        builder.define(DATA_LINES, "");
        builder.define(DATA_TITLE_COLOR, 0xFFC832);
        builder.define(DATA_LINES_COLOR, 0xB4FFB4);
        builder.define(DATA_FLOAT_AMPLITUDE, 0.12F);
        builder.define(DATA_FLOAT_SPEED, 0.04F);
        builder.define(DATA_FLOAT_HEIGHT, 2.2F);
        builder.define(DATA_VARS, "");
    }

    public void configureFromScript(String goalId, GoalScript script) {
        this.entityData.set(DATA_GOAL_ID, goalId == null ? "" : goalId);
        GoalScript.Display display = script != null ? script.display() : GoalScript.Display.defaults();
        this.entityData.set(DATA_TITLE, display.title());
        this.entityData.set(DATA_LINES, String.join("\n", display.lines()));
        this.entityData.set(DATA_TITLE_COLOR, display.titleColor().packedRgb());
        this.entityData.set(DATA_LINES_COLOR, display.linesColor().packedRgb());
        this.entityData.set(DATA_FLOAT_AMPLITUDE, (float) display.floating().amplitude());
        this.entityData.set(DATA_FLOAT_SPEED, (float) display.floating().speed());
        this.entityData.set(DATA_FLOAT_HEIGHT, (float) display.floating().height());
    }

    public void updateRuntime(long progress, long target, Map<String, String> variables) {
        this.entityData.set(DATA_PROGRESS, Math.max(0L, progress));
        this.entityData.set(DATA_TARGET, Math.max(0L, target));
        this.entityData.set(DATA_VARS, serializeVars(variables));
    }

    public String goalId() {
        return this.entityData.get(DATA_GOAL_ID);
    }

    public long progress() {
        return this.entityData.get(DATA_PROGRESS);
    }

    public long target() {
        return this.entityData.get(DATA_TARGET);
    }

    public String displayTitle() {
        return this.entityData.get(DATA_TITLE);
    }

    public String displayLinesRaw() {
        return this.entityData.get(DATA_LINES);
    }

    public int titleColor() {
        return this.entityData.get(DATA_TITLE_COLOR);
    }

    public int linesColor() {
        return this.entityData.get(DATA_LINES_COLOR);
    }

    public float floatAmplitude() {
        return this.entityData.get(DATA_FLOAT_AMPLITUDE);
    }

    public float floatSpeed() {
        return this.entityData.get(DATA_FLOAT_SPEED);
    }

    public float floatHeight() {
        return this.entityData.get(DATA_FLOAT_HEIGHT);
    }

    public String serializedVars() {
        return this.entityData.get(DATA_VARS);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("GoalId", goalId());
        output.putLong("Progress", progress());
        output.putLong("Target", target());
        output.putString("Title", displayTitle());
        output.putString("Lines", displayLinesRaw());
        output.putInt("TitleColor", titleColor());
        output.putInt("LinesColor", linesColor());
        output.putFloat("Amplitude", floatAmplitude());
        output.putFloat("Speed", floatSpeed());
        output.putFloat("Height", floatHeight());
        output.putString("Vars", serializedVars());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_GOAL_ID, input.getStringOr("GoalId", ""));
        this.entityData.set(DATA_PROGRESS, input.getLongOr("Progress", 0L));
        this.entityData.set(DATA_TARGET, input.getLongOr("Target", 0L));
        this.entityData.set(DATA_TITLE, input.getStringOr("Title", ""));
        this.entityData.set(DATA_LINES, input.getStringOr("Lines", ""));
        this.entityData.set(DATA_TITLE_COLOR, input.getIntOr("TitleColor", 0xFFC832));
        this.entityData.set(DATA_LINES_COLOR, input.getIntOr("LinesColor", 0xB4FFB4));
        this.entityData.set(DATA_FLOAT_AMPLITUDE, input.getFloatOr("Amplitude", 0.12F));
        this.entityData.set(DATA_FLOAT_SPEED, input.getFloatOr("Speed", 0.04F));
        this.entityData.set(DATA_FLOAT_HEIGHT, input.getFloatOr("Height", 2.2F));
        this.entityData.set(DATA_VARS, input.getStringOr("Vars", ""));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 262144.0D;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    private String serializeVars(Map<String, String> vars) {
        if (vars == null || vars.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(escape(entry.getKey())).append('=').append(escape(entry.getValue() == null ? "" : entry.getValue()));
        }
        return builder.toString();
    }

    private String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=");
    }
}
