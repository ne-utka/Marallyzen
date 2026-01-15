package neutka.marallys.marallyzen.replay.server;

import neutka.marallys.marallyzen.replay.ReplayServerTrack;

public record ReplayServerResult(String replayId, int keyframeInterval, ReplayServerTrack track) {
}
