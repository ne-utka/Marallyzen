package neutka.marallys.marallyzen.replay;

import net.minecraft.client.Minecraft;

public final class ReplayStartQueue {
    private static volatile Object pendingReplayFile;
    private static volatile boolean pendingCheckModCompat;
    private static volatile boolean pendingAsyncMode;
    private static volatile boolean deferredStart;
    private static volatile int lastStateHash;
    private static volatile boolean lastStateLogged;
    private static volatile boolean watchdogRunning;

    public static boolean hasPending() {
        return pendingReplayFile != null;
    }

    private ReplayStartQueue() {
    }

    public static boolean isDeferredStart() {
        return deferredStart;
    }

    public static void enqueue(Object replayFile, boolean checkModCompat, boolean asyncMode) {
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
            "ReplayStartQueue: enqueue replay start (checkModCompat={}, asyncMode={})",
            checkModCompat,
            asyncMode
        );
        pendingReplayFile = replayFile;
        pendingCheckModCompat = checkModCompat;
        pendingAsyncMode = asyncMode;
        lastStateLogged = false;
        startWatchdog(replayFile, checkModCompat, asyncMode);
    }

    public static void tick() {
        Object replayFile = pendingReplayFile;
        if (replayFile == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        logStateOnce(mc);
        if (mc.level != null) {
            return;
        }
        startNow(replayFile, pendingCheckModCompat, pendingAsyncMode);
    }

    public static void startNow(Object replayFile, boolean checkModCompat, boolean asyncMode) {
        if (replayFile == null) {
            return;
        }
        pendingReplayFile = null;
        deferredStart = true;
        watchdogRunning = false;
        try {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("ReplayStartQueue: starting deferred replay");
            Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
            Object instance = replayModReplay.getField("instance").get(null);
            if (instance == null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayStartQueue: ReplayModReplay.instance is null");
                return;
            }
            Class<?> replayFileClass = Class.forName("com.replaymod.replaystudio.replay.ReplayFile");
            java.lang.reflect.Method startReplay = replayModReplay.getMethod(
                "startReplay",
                replayFileClass,
                boolean.class,
                boolean.class
            );
            startReplay.invoke(instance, replayFile, checkModCompat, asyncMode);
        } catch (ReflectiveOperationException e) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                "ReplayStartQueue: failed to start deferred replay",
                e
            );
        } finally {
            deferredStart = false;
        }
    }

    public static void startPendingNow() {
        Object replayFile = pendingReplayFile;
        if (replayFile == null) {
            return;
        }
        startNow(replayFile, pendingCheckModCompat, pendingAsyncMode);
    }

    private static void startWatchdog(Object replayFile, boolean checkModCompat, boolean asyncMode) {
        if (watchdogRunning) {
            return;
        }
        watchdogRunning = true;
        Thread watchdog = new Thread(() -> {
            try {
                for (int i = 0; i < 400; i++) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) {
                        mc.execute(() -> startNow(replayFile, checkModCompat, asyncMode));
                        return;
                    }
                    Thread.sleep(50L);
                }
                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                    "ReplayStartQueue: watchdog timed out waiting for world unload"
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                watchdogRunning = false;
            }
        }, "Marallyzen-ReplayStartWatchdog");
        watchdog.start();
    }

    private static void logStateOnce(Minecraft mc) {
        String screenName = mc.screen == null ? "null" : mc.screen.getClass().getName();
        int hash = (mc.level != null ? 1 : 0)
            | (mc.player != null ? 2 : 0)
            | (screenName.hashCode() << 2);
        if (!lastStateLogged || hash != lastStateHash) {
            lastStateHash = hash;
            lastStateLogged = true;
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
                "ReplayStartQueue: state level={}, player={}, screen={}",
                mc.level != null,
                mc.player != null,
                screenName
            );
        }
    }
}
