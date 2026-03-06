package neutka.marallys.marallyzen.npc.replay;

/**
 * Runtime replay state for a single NPC.
 */
public final class NpcReplayState {
    private final String npcId;
    private final String replayId;
    private final NpcReplayScript.ReplayMode mode;
    private final boolean loop;
    private final boolean interpolate;
    private final String nextTriggerId;
    private final boolean persistState;
    private final boolean preview;
    private Runnable onComplete;
    private int frameIndex;
    private int repeatTick;
    private int stuckTicks;
    private long startedGameTick;
    private long lastProcessedGameTick = -1L;
    private long replayUptimeTicks;
    private long totalTickCostNanos;
    private long lastTickCostNanos;
    private float lastSwingProgress;
    private String lastMainHandItemId = "";
    private int lastMainHandCount;
    private boolean hasLastMainHand;
    private String lastOffHandItemId = "";
    private int lastOffHandCount;
    private boolean hasLastOffHand;
    private boolean lastCrouching;
    private boolean hasLastCrouching;
    private boolean lastUsingItem;
    private boolean hasLastUsingItem;
    private double lastAppliedX;
    private double lastAppliedY;
    private double lastAppliedZ;
    private boolean hasLastAppliedPosition;
    private ReplayStopReason lastStopReason = ReplayStopReason.NONE;
    private boolean originalNoAiCaptured;
    private boolean originalNoAi;
    private boolean running = true;

    public NpcReplayState(
            String npcId,
            String replayId,
            NpcReplayScript.ReplayMode mode,
            boolean loop,
            boolean interpolate,
            String nextTriggerId,
            int frameIndex,
            boolean persistState,
            boolean preview
    ) {
        this.npcId = npcId;
        this.replayId = replayId;
        this.mode = mode == null ? NpcReplayScript.ReplayMode.PHYSICS : mode;
        this.loop = loop;
        this.interpolate = interpolate;
        this.nextTriggerId = nextTriggerId == null ? "" : nextTriggerId;
        this.frameIndex = Math.max(0, frameIndex);
        this.persistState = persistState;
        this.preview = preview;
    }

    public String npcId() {
        return npcId;
    }

    public String replayId() {
        return replayId;
    }

    public NpcReplayScript.ReplayMode mode() {
        return mode;
    }

    public boolean loop() {
        return loop;
    }

    public boolean interpolate() {
        return interpolate;
    }

    public String nextTriggerId() {
        return nextTriggerId;
    }

    public boolean persistState() {
        return persistState;
    }

    public boolean preview() {
        return preview;
    }

    public int frameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = Math.max(0, frameIndex);
    }

    public int repeatTick() {
        return repeatTick;
    }

    public void setRepeatTick(int repeatTick) {
        this.repeatTick = Math.max(0, repeatTick);
    }

    public void incrementRepeatTick() {
        this.repeatTick++;
    }

    public int stuckTicks() {
        return stuckTicks;
    }

    public void resetStuckTicks() {
        this.stuckTicks = 0;
    }

    public void incrementStuckTicks() {
        this.stuckTicks++;
    }

    public long startedGameTick() {
        return startedGameTick;
    }

    public void setStartedGameTick(long startedGameTick) {
        this.startedGameTick = Math.max(0L, startedGameTick);
    }

    public long lastProcessedGameTick() {
        return lastProcessedGameTick;
    }

    public void setLastProcessedGameTick(long tick) {
        this.lastProcessedGameTick = tick;
    }

    public long replayUptimeTicks() {
        return replayUptimeTicks;
    }

    public void incrementReplayUptimeTicks() {
        this.replayUptimeTicks++;
    }

    public long totalTickCostNanos() {
        return totalTickCostNanos;
    }

    public long lastTickCostNanos() {
        return lastTickCostNanos;
    }

    public void recordTickCostNanos(long nanos) {
        long safe = Math.max(0L, nanos);
        this.lastTickCostNanos = safe;
        this.totalTickCostNanos += safe;
    }

    public long averageTickCostNanos() {
        long ticks = Math.max(1L, replayUptimeTicks);
        return totalTickCostNanos / ticks;
    }

    public float lastSwingProgress() {
        return lastSwingProgress;
    }

    public void setLastSwingProgress(float lastSwingProgress) {
        this.lastSwingProgress = lastSwingProgress;
    }

    public String lastMainHandItemId() {
        return lastMainHandItemId;
    }

    public int lastMainHandCount() {
        return lastMainHandCount;
    }

    public boolean isSameMainHand(String itemId, int count) {
        if (!hasLastMainHand) {
            return false;
        }
        String normalized = itemId == null ? "" : itemId;
        int normalizedCount = Math.max(0, count);
        return normalized.equals(lastMainHandItemId) && normalizedCount == lastMainHandCount;
    }

    public void setLastMainHand(String itemId, int count) {
        this.lastMainHandItemId = itemId == null ? "" : itemId;
        this.lastMainHandCount = Math.max(0, count);
        this.hasLastMainHand = true;
    }

    public boolean isSameOffHand(String itemId, int count) {
        if (!hasLastOffHand) {
            return false;
        }
        String normalized = itemId == null ? "" : itemId;
        int normalizedCount = Math.max(0, count);
        return normalized.equals(lastOffHandItemId) && normalizedCount == lastOffHandCount;
    }

    public void setLastOffHand(String itemId, int count) {
        this.lastOffHandItemId = itemId == null ? "" : itemId;
        this.lastOffHandCount = Math.max(0, count);
        this.hasLastOffHand = true;
    }

    public boolean isSameCrouching(boolean crouching) {
        return hasLastCrouching && lastCrouching == crouching;
    }

    public void setLastCrouching(boolean crouching) {
        this.lastCrouching = crouching;
        this.hasLastCrouching = true;
    }

    public boolean isSameUsingItem(boolean usingItem) {
        return hasLastUsingItem && lastUsingItem == usingItem;
    }

    public void setLastUsingItem(boolean usingItem) {
        this.lastUsingItem = usingItem;
        this.hasLastUsingItem = true;
    }

    public boolean hasLastAppliedPosition() {
        return hasLastAppliedPosition;
    }

    public double lastAppliedX() {
        return lastAppliedX;
    }

    public double lastAppliedY() {
        return lastAppliedY;
    }

    public double lastAppliedZ() {
        return lastAppliedZ;
    }

    public void setLastAppliedPosition(double x, double y, double z) {
        this.lastAppliedX = x;
        this.lastAppliedY = y;
        this.lastAppliedZ = z;
        this.hasLastAppliedPosition = true;
    }

    public void clearLastAppliedPosition() {
        this.hasLastAppliedPosition = false;
        this.lastAppliedX = 0.0D;
        this.lastAppliedY = 0.0D;
        this.lastAppliedZ = 0.0D;
    }

    public boolean running() {
        return running;
    }

    public void stop() {
        this.running = false;
    }

    public ReplayStopReason lastStopReason() {
        return lastStopReason;
    }

    public void setLastStopReason(ReplayStopReason lastStopReason) {
        this.lastStopReason = lastStopReason == null ? ReplayStopReason.NONE : lastStopReason;
    }

    public boolean originalNoAiCaptured() {
        return originalNoAiCaptured;
    }

    public boolean originalNoAi() {
        return originalNoAi;
    }

    public void captureOriginalNoAi(boolean originalNoAi) {
        if (this.originalNoAiCaptured) {
            return;
        }
        this.originalNoAiCaptured = true;
        this.originalNoAi = originalNoAi;
    }

    public Runnable onComplete() {
        return onComplete;
    }

    public void setOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
    }
}
