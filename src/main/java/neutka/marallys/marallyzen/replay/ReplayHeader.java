package neutka.marallys.marallyzen.replay;

public record ReplayHeader(int version, int tickRate, int keyframeInterval, long durationTicks, String dimension) {
}
