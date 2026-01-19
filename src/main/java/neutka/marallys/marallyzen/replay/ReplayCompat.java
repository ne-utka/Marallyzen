package neutka.marallys.marallyzen.replay;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.neoforged.fml.ModList;

public final class ReplayCompat {
    private ReplayCompat() {
    }

    public static boolean isReplayAvailable() {
        return ModList.get().isLoaded("reforgedplaymod") || ModList.get().isLoaded("replaymod");
    }

    public static boolean isReplayActive() {
        return getReplayHandler() != null;
    }

    public static int getReplayTimeTicks() {
        Object sender = getReplaySender();
        if (sender == null) {
            return 0;
        }
        try {
            Method currentTimeStamp = sender.getClass().getMethod("currentTimeStamp");
            Object value = currentTimeStamp.invoke(sender);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    public static double getReplayTimeSeconds() {
        return getReplayTimeTicks() / 20.0;
    }

    public static boolean setReplaySpeed(double speed) {
        Object sender = getReplaySender();
        if (sender == null) {
            return false;
        }
        try {
            Method setReplaySpeed = sender.getClass().getMethod("setReplaySpeed", double.class);
            setReplaySpeed.invoke(sender, speed);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static boolean seekReplaySeconds(double seconds) {
        return seekReplayTicks((int) Math.round(seconds * 20.0));
    }

    public static boolean seekReplayTicks(int ticks) {
        Object sender = getReplaySender();
        if (sender == null) {
            return false;
        }
        try {
            Method jumpToTime = sender.getClass().getMethod("jumpToTime", int.class);
            jumpToTime.invoke(sender, ticks);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static boolean startReplay(java.io.File replayFile) {
        if (replayFile == null || !replayFile.exists()) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: replay file missing: {}", replayFile);
            return false;
        }
        if (!isReplayAvailable()) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: ReplayMod not available");
            return false;
        }
        try {
            Object handler = startReplayDirect(replayFile);
            if (handler != null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("ReplayCompat: started replay (direct) {}", replayFile);
                return true;
            }

            Object legacy = startReplayLegacy(replayFile);
            if (legacy != null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("ReplayCompat: started replay (legacy) {}", replayFile);
                return true;
            }

            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: start replay returned null {}", replayFile);
            return false;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                "ReplayCompat: failed to start replay {} (cause={})",
                replayFile,
                cause.toString()
            );
            return false;
        } catch (ReflectiveOperationException e) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: failed to start replay {}", replayFile, e);
            return false;
        }
    }


    public static boolean stopReplay() {
        if (!isReplayAvailable()) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: ReplayMod not available");
            return false;
        }
        try {
            Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
            java.lang.reflect.Field instanceField = replayModReplay.getField("instance");
            Object instance = instanceField.get(null);
            if (instance == null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: ReplayModReplay.instance is null");
                return false;
            }
            java.lang.reflect.Method stopReplay = replayModReplay.getMethod("forcefullyStopReplay");
            stopReplay.invoke(instance);
            return true;
        } catch (ReflectiveOperationException e) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: failed to stop replay", e);
            return false;
        }
    }

    public static boolean openReplayViewer() {
        if (!isReplayAvailable()) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: ReplayMod not available");
            return false;
        }
        try {
            Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
            Field instanceField = replayModReplay.getField("instance");
            Object instance = instanceField.get(null);
            if (instance == null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: ReplayModReplay.instance is null");
                return false;
            }

            Class<?> viewerClass = Class.forName("com.replaymod.replay.gui.screen.GuiReplayViewer");
            Object viewer = viewerClass.getConstructor(replayModReplay).newInstance(instance);
            Method display = viewerClass.getMethod("display");
            display.invoke(viewer);
            return true;
        } catch (ReflectiveOperationException e) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: failed to open replay viewer", e);
            return false;
        }
    }

    private static Object getReplayHandler() {
        if (!isReplayAvailable()) {
            return null;
        }
        try {
            Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
            Field instanceField = replayModReplay.getField("instance");
            Object instance = instanceField.get(null);
            if (instance == null) {
                return null;
            }
            Method getReplayHandler = replayModReplay.getMethod("getReplayHandler");
            return getReplayHandler.invoke(instance);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Object getReplaySender() {
        Object handler = getReplayHandler();
        if (handler == null) {
            return null;
        }
        try {
            Method getReplaySender = handler.getClass().getMethod("getReplaySender");
            return getReplaySender.invoke(handler);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Object startReplayDirect(java.io.File replayFile) throws ReflectiveOperationException {
        Class<?> replayModClass = Class.forName("com.replaymod.core.ReplayMod");
        java.lang.reflect.Field replayModInstanceField = replayModClass.getField("instance");
        Object replayModInstance = replayModInstanceField.get(null);
        if (replayModInstance == null) {
            return null;
        }

        java.lang.reflect.Field filesField = replayModClass.getField("files");
        Object filesService = filesField.get(replayModInstance);
        if (filesService == null) {
            return null;
        }

        java.lang.reflect.Method open = filesService.getClass().getMethod("open", java.nio.file.Path.class);
        Object replayFileHandle = open.invoke(filesService, replayFile.toPath());
        if (replayFileHandle == null) {
            return null;
        }

        Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
        java.lang.reflect.Field replayModReplayInstanceField = replayModReplay.getField("instance");
        Object replayModReplayInstance = replayModReplayInstanceField.get(null);
        if (replayModReplayInstance == null) {
            return null;
        }

        Class<?> replayFileClass = Class.forName("com.replaymod.replaystudio.replay.ReplayFile");
        java.lang.reflect.Method startReplay = replayModReplay.getMethod("startReplay", replayFileClass);
        startReplay.invoke(replayModReplayInstance, replayFileHandle);
        return getReplayHandler();
    }

    private static Object startReplayLegacy(java.io.File replayFile) throws ReflectiveOperationException {
        Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
        java.lang.reflect.Field instanceField = replayModReplay.getField("instance");
        Object instance = instanceField.get(null);
        if (instance == null) {
            return null;
        }
        java.lang.reflect.Method startReplay = replayModReplay.getMethod("startReplay", java.io.File.class);
        return startReplay.invoke(instance, replayFile);
    }

}
