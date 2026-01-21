package neutka.marallys.marallyzen.replay;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

public final class ReplayCompat {
    private static volatile Object patchedReplayViewer;
    private static volatile long suppressScreensUntilMs;

    private ReplayCompat() {
    }

    public static boolean isReplayAvailable() {
        return ModList.get().isLoaded("reforgedplaymod") || ModList.get().isLoaded("replaymod");
    }

    public static boolean isReplayActive() {
        return getReplayHandler() != null;
    }

    public static long getReplayTimeMs() {
        Object sender = getReplaySender();
        if (sender == null) {
            return 0L;
        }
        try {
            Method currentTimeStamp = sender.getClass().getMethod("currentTimeStamp");
            Object value = currentTimeStamp.invoke(sender);
            return value instanceof Integer i ? i.longValue() : 0L;
        } catch (ReflectiveOperationException e) {
            return 0L;
        }
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

    public static double getReplaySpeed() {
        Object sender = getReplaySender();
        if (sender == null) {
            return 0.0;
        }
        try {
            Method getReplaySpeed = sender.getClass().getMethod("getReplaySpeed");
            Object value = getReplaySpeed.invoke(sender);
            return value instanceof Double d ? d : 0.0;
        } catch (ReflectiveOperationException e) {
            return 0.0;
        }
    }

    public static long getReplayDurationMs() {
        Object handler = getReplayHandler();
        if (handler == null) {
            return 0L;
        }
        Long value = invokeLongMethod(handler, "getReplayDuration", "getDuration", "getReplayLength");
        if (value != null && value > 0L) {
            return value;
        }
        Object sender = getReplaySender();
        if (sender == null) {
            return 0L;
        }
        value = invokeLongMethod(sender, "getReplayDuration", "getDuration", "getReplayLength");
        return value != null ? value : 0L;
    }

    public static Object getReplayOverlay() {
        Object handler = getReplayHandler();
        if (handler == null) {
            return null;
        }
        try {
            Method getOverlay = handler.getClass().getMethod("getOverlay");
            return getOverlay.invoke(handler);
        } catch (ReflectiveOperationException e) {
            return null;
        }
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
            int current = getReplayTimeTicks();
            if (ticks < current) {
                markSeek(1500L);
            } else {
                markSeek();
            }
            Method jumpToTime = sender.getClass().getMethod("jumpToTime", int.class);
            jumpToTime.invoke(sender, ticks);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static void markSeek() {
        markSeek(800L);
    }

    public static void markSeek(long durationMs) {
        suppressScreensUntilMs = System.currentTimeMillis() + durationMs;
    }

    private static boolean isSuppressingReplayScreens() {
        return System.currentTimeMillis() < suppressScreensUntilMs;
    }

    public static boolean shouldSuppressReplayScreen(net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null) {
            return false;
        }
        if (!isReplayActive()) {
            return false;
        }
        if (!isSuppressingReplayScreens()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        String name = screen.getClass().getName();
        return "net.minecraft.client.gui.screens.ProgressScreen".equals(name)
            || "net.minecraft.client.gui.screens.ReceivingLevelScreen".equals(name)
            || "net.minecraft.client.gui.screens.TitleScreen".equals(name);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null || mc.player != null) {
            ReplayReturnManager.getInstance().captureSession(mc);
            return startReplayDeferred(replayFile);
        }
        neutka.marallys.marallyzen.Marallyzen.LOGGER.info("ReplayCompat: starting replay {}", replayFile);
        try {
            Class<?> replayModReplay = Class.forName("com.replaymod.replay.ReplayModReplay");
            java.lang.reflect.Field instanceField = replayModReplay.getField("instance");
            Object instance = instanceField.get(null);
            if (instance == null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: ReplayModReplay.instance is null");
                return false;
            }

            Object handler = startReplayDirect(replayFile);
            if (handler != null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("ReplayCompat: started replay (direct) {}", replayFile);
                return true;
            }
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: direct start returned null {}", replayFile);

            Object legacy = startReplayLegacy(replayFile);
            if (legacy != null) {
                neutka.marallys.marallyzen.Marallyzen.LOGGER.info("ReplayCompat: started replay (legacy) {}", replayFile);
                return true;
            }
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: legacy start returned null {}", replayFile);

            java.lang.reflect.Method startReplay = replayModReplay.getMethod("startReplay", java.io.File.class);
            startReplay.invoke(instance, replayFile);
            return true;
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
            ReplayReturnManager.getInstance().requestReturn();
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

            try {
                Method openViewer = replayModReplay.getMethod("openReplayViewer");
                openViewer.invoke(instance);
                return true;
            } catch (NoSuchMethodException ignored) {
                // Fallback to constructing the viewer directly.
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

    public static boolean isReplayViewerScreen(Object screen) {
        if (screen == null) {
            return false;
        }
        String name = screen.getClass().getName();
        if (name.startsWith("com.replaymod.replay.gui.screen.")) {
            return true;
        }
        return "de.johni0702.minecraft.gui.container.AbstractGuiScreen$MinecraftGuiScreen".equals(name)
            || "de.johni0702.minecraft.gui.container.AbstractGuiScreen.MinecraftGuiScreen".equals(name);
    }

    public static void tryOverrideReplayViewerLoadButton(Object screen) {
        if (screen == null) {
            return;
        }
        try {
            Object wrapper = getReplayViewerWrapper(screen);
            if (wrapper == null || wrapper == patchedReplayViewer) {
                return;
            }
            if (!"com.replaymod.replay.gui.screen.GuiReplayViewer".equals(wrapper.getClass().getName())) {
                return;
            }
            Object loadButton = getField(wrapper, "loadButton");
            Object list = getField(wrapper, "list");
            if (loadButton == null || list == null) {
                return;
            }
            java.lang.reflect.Method onClick = loadButton.getClass().getMethod("onClick", java.lang.Runnable.class);
            onClick.invoke(loadButton, (Runnable) () -> {
                try {
                    java.lang.reflect.Method getSelected = list.getClass().getMethod("getSelected");
                    Object selected = getSelected.invoke(list);
                    if (!(selected instanceof java.util.List<?> selectedList) || selectedList.isEmpty()) {
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                            "ReplayCompat: load button clicked with no selection"
                        );
                        return;
                    }
                    Object entry = selectedList.get(0);
                    Object fileObj = getField(entry, "file");
                    if (!(fileObj instanceof java.io.File file)) {
                        neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                            "ReplayCompat: selected entry has no file"
                        );
                        return;
                    }
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
                        "ReplayCompat: load button override starting replay {}",
                        file
                    );
                    boolean ok = startReplay(file);
                    if (ok) {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> mc.setScreen(new neutka.marallys.marallyzen.client.director.ReplayInputScreen()));
                    }
                } catch (ReflectiveOperationException e) {
                    neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                        "ReplayCompat: load button override failed",
                        e
                    );
                }
            });
            patchedReplayViewer = wrapper;
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info("ReplayCompat: patched ReplayMod load button");
        } catch (ReflectiveOperationException e) {
            // Ignore: viewer may not be fully initialized yet.
        }
    }

    public static void runReplayModTasks() {
        if (!isReplayAvailable()) {
            return;
        }
        try {
            Class<?> replayModClass = Class.forName("com.replaymod.core.ReplayMod");
            Field instanceField = replayModClass.getField("instance");
            Object instance = instanceField.get(null);
            if (instance == null) {
                return;
            }
            Method runTasks = replayModClass.getMethod("runTasks");
            runTasks.invoke(instance);
        } catch (ReflectiveOperationException e) {
            // Ignore: replay mod may not be ready yet.
        }
    }

    private static Object getReplayViewerWrapper(Object screen) throws ReflectiveOperationException {
        String name = screen.getClass().getName();
        if (!"de.johni0702.minecraft.gui.container.AbstractGuiScreen$MinecraftGuiScreen".equals(name)
            && !"de.johni0702.minecraft.gui.container.AbstractGuiScreen.MinecraftGuiScreen".equals(name)) {
            return null;
        }
        java.lang.reflect.Method getWrapper = screen.getClass().getMethod("getWrapper");
        return getWrapper.invoke(screen);
    }

    private static Object getField(Object target, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field field = target.getClass().getField(name);
        return field.get(target);
    }

    public static Object getReplayHandler() {
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

    private static Long invokeLongMethod(Object target, String... names) {
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                Object value = method.invoke(target);
                if (value instanceof Integer i) {
                    return i.longValue();
                }
                if (value instanceof Long l) {
                    return l;
                }
            } catch (ReflectiveOperationException e) {
                // Try next.
            }
        }
        return null;
    }

    private static Object startReplayDirect(java.io.File replayFile) throws ReflectiveOperationException {
        Object replayFileHandle = openReplayFileHandle(replayFile);
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
        java.lang.reflect.Method startReplay = replayModReplay.getMethod(
            "startReplay",
            replayFileClass,
            boolean.class,
            boolean.class
        );
        return startReplay.invoke(replayModReplayInstance, replayFileHandle, true, true);
    }

    private static Object openReplayFileHandle(java.io.File replayFile) throws ReflectiveOperationException {
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
        return open.invoke(filesService, replayFile.toPath());
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

    private static boolean startReplayDeferred(java.io.File replayFile) {
        try {
            Object replayFileHandle = openReplayFileHandle(replayFile);
            if (replayFileHandle == null) {
                return false;
            }
            neutka.marallys.marallyzen.Marallyzen.LOGGER.info(
                "ReplayCompat: deferring replay start until world unload {}",
                replayFile
            );
            ReplayStartQueue.enqueue(replayFileHandle, true, true);
            requestDisconnect();
            return true;
        } catch (ReflectiveOperationException e) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                "ReplayCompat: failed to defer replay start {}",
                replayFile,
                e
            );
            return false;
        }
    }

    private static void requestDisconnect() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            forceDisconnect(mc);
            try {
                Method disconnectWithProgress = mc.getClass().getMethod("disconnectWithProgressScreen");
                disconnectWithProgress.invoke(mc);
                return;
            } catch (ReflectiveOperationException e) {
                // Fall through to other disconnect variants.
            }
            try {
                Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
                Method disconnect = mc.getClass().getMethod("disconnect", screenClass);
                disconnect.invoke(mc, mc.screen);
                return;
            } catch (ReflectiveOperationException e) {
                // Fall through to no-arg disconnect.
            }
            try {
                Method disconnect = mc.getClass().getMethod("disconnect");
                disconnect.invoke(mc);
            } catch (ReflectiveOperationException e) {
                // Give up if we cannot request a disconnect.
            }
        });
        waitForDisconnectThenClearLevel(mc);
    }

    private static void forceDisconnect(Minecraft mc) {
        try {
            var listener = mc.getConnection();
            if (listener == null) {
                return;
            }
            Object connection = listener.getConnection();
            if (connection == null) {
                return;
            }
            Method disconnect = connection.getClass().getMethod("disconnect", Component.class);
            disconnect.invoke(connection, Component.literal("Replay start"));
        } catch (ReflectiveOperationException e) {
            neutka.marallys.marallyzen.Marallyzen.LOGGER.warn("ReplayCompat: failed to force disconnect", e);
        }
    }

    private static void waitForDisconnectThenClearLevel(Minecraft mc) {
        Thread watcher = new Thread(() -> {
            try {
                for (int i = 0; i < 200; i++) {
                    if (!isConnectionActive(mc)) {
                        mc.execute(() -> {
                            try {
                                mc.setLevel(null, ReceivingLevelScreen.Reason.OTHER);
                                ReplayStartQueue.startPendingNow();
                            } catch (Throwable e) {
                                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                                    "ReplayCompat: failed to clear level after disconnect",
                                    e
                                );
                            }
                        });
                        return;
                    }
                    Thread.sleep(25L);
                }
                neutka.marallys.marallyzen.Marallyzen.LOGGER.warn(
                    "ReplayCompat: timed out waiting for disconnect"
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Marallyzen-ReplayDisconnectWatcher");
        watcher.start();
    }

    private static boolean isConnectionActive(Minecraft mc) {
        try {
            var listener = mc.getConnection();
            if (listener == null) {
                return false;
            }
            Object connection = listener.getConnection();
            if (connection == null) {
                return false;
            }
            Method isConnected = connection.getClass().getMethod("isConnected");
            Object value = isConnected.invoke(connection);
            return value instanceof Boolean b && b;
        } catch (ReflectiveOperationException e) {
            return mc.getConnection() != null;
        }
    }

}
