package neutka.marallys.marallyzen.replay;

import java.util.List;

public record ReplayServerSnapshot(long tick, long worldTime, boolean raining, float rainLevel,
                                   boolean thundering, float thunderLevel, List<ReplayEntityFrame> entities) {
}
