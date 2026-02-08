package neutka.marallys.marallyzen.util;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;

public final class VersionBootstrap {
    private VersionBootstrap() {
    }

    public static void ensureGameVersion() {
        try {
            if (SharedConstants.getCurrentVersion() != null) {
                return;
            }
        } catch (Throwable ignored) {
            // Version not set yet.
        }
        WorldVersion detected = null;
        try {
            detected = DetectedVersion.tryDetectVersion();
        } catch (Throwable ignored) {
            // Fall back below.
        }
        if (detected == null) {
            detected = DetectedVersion.BUILT_IN;
        }
        try {
            SharedConstants.setVersion(detected);
        } catch (Throwable ignored) {
            // Already set by the game or another bootstrap.
        }
    }
}
