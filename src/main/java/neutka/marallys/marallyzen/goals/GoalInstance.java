package neutka.marallys.marallyzen.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;

/**
 * Runtime wrapper around script + persisted progress data.
 */
public final class GoalInstance {
    private final String goalId;
    private final GoalScript script;
    private final GoalSavedData.GoalProgressData progressData;

    public GoalInstance(String goalId, GoalScript script, GoalSavedData.GoalProgressData progressData) {
        this.goalId = goalId;
        this.script = script;
        this.progressData = progressData;
    }

    public String goalId() {
        return goalId;
    }

    public GoalScript script() {
        return script;
    }

    public GoalSavedData.GoalProgressData progressData() {
        return progressData;
    }

    public long progress() {
        return progressData.progress();
    }

    public long target() {
        return script.goal().target();
    }

    public BlockPos spawnPos() {
        return progressData.spawnPos();
    }

    public String dimensionId() {
        return progressData.dimensionId();
    }

    public boolean completed() {
        return progressData.completed();
    }

    public void setCompleted(boolean completed) {
        progressData.setCompleted(completed);
    }

    public long addProgress(long amount) {
        if (amount <= 0L) {
            return progress();
        }
        long next = progressData.progress() + amount;
        if (target() > 0L) {
            next = Math.min(next, target());
        }
        progressData.setProgress(next);
        return next;
    }

    public boolean hasMilestone(long threshold) {
        return progressData.triggeredMilestones().contains(threshold);
    }

    public void markMilestone(long threshold) {
        progressData.triggeredMilestones().add(threshold);
    }

    public Set<Long> triggeredMilestonesSnapshot() {
        return new HashSet<>(progressData.triggeredMilestones());
    }

    public CompoundTag customData() {
        return progressData.customData();
    }

    public String entityUuid() {
        return progressData.entityUuid();
    }

    public void setEntityUuid(String uuid) {
        progressData.setEntityUuid(uuid);
    }
}
