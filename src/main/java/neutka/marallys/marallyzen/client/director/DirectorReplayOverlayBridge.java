package neutka.marallys.marallyzen.client.director;

import java.lang.reflect.Method;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.director.DirectorRuntime;
import neutka.marallys.marallyzen.director.ReplayTimeSourceHolder;
import neutka.marallys.marallyzen.replay.ReplayCompat;
import neutka.marallys.marallyzen.replay.ReplayModTimeSource;

public final class DirectorReplayOverlayBridge {
    private static final ReplayModTimeSource TIME_SOURCE = new ReplayModTimeSource();
    private static volatile Object patchedOverlay;

    private DirectorReplayOverlayBridge() {
    }

    public static void updateReplayState(boolean wasActive, boolean isActive) {
        if (isActive) {
            ReplayTimeSourceHolder.set(TIME_SOURCE);
            tryAttachButton();
            return;
        }
        if (wasActive) {
            DirectorOverlayHud.hide();
            DirectorRuntime.stop();
            ReplayTimeSourceHolder.set(null);
            patchedOverlay = null;
        }
    }

    public static void onReplayHandlerCreated(Object handler) {
        if (handler == null) {
            return;
        }
        ReplayTimeSourceHolder.set(TIME_SOURCE);
        tryAttachButtonFromHandler(handler);
    }

    private static void tryAttachButton() {
        Object overlay = ReplayCompat.getReplayOverlay();
        attachButton(overlay);
    }

    private static void tryAttachButtonFromHandler(Object handler) {
        try {
            Method getOverlay = handler.getClass().getMethod("getOverlay");
            Object overlay = getOverlay.invoke(handler);
            attachButton(overlay);
        } catch (ReflectiveOperationException e) {
            // Replay handler not ready yet.
        }
    }

    private static void attachButton(Object overlay) {
        if (overlay == null || overlay == patchedOverlay) {
            return;
        }
        try {
            Object topPanel = overlay.getClass().getField("topPanel").get(overlay);
            Object timeline = overlay.getClass().getField("timeline").get(overlay);
            Object button = createDirectorButton();
            if (topPanel != null && button != null) {
                if (timeline != null) {
                    invokeRemoveElement(topPanel, timeline);
                }
                invokeAddElements(topPanel, button);
                if (timeline != null) {
                    invokeAddElements(topPanel, timeline);
                }
                patchedOverlay = overlay;
                Marallyzen.LOGGER.info("Director overlay button attached");
            }
        } catch (ReflectiveOperationException e) {
            // Ignore until overlay is fully initialized.
        }
    }

    private static Object createDirectorButton() throws ReflectiveOperationException {
        Class<?> buttonClass = Class.forName("de.johni0702.minecraft.gui.element.GuiButton");
        Object button = buttonClass.getConstructor().newInstance();
        call(button, "setSize", int.class, int.class, 70, 20);
        call(button, "setLabel", String.class, "Marallyzen");
        call(button, "onClick", Runnable.class, (Runnable) DirectorOverlayHud::toggle);
        return button;
    }

    private static void invokeAddElements(Object panel, Object element) throws ReflectiveOperationException {
        for (Method method : panel.getClass().getMethods()) {
            if (!method.getName().equals("addElements")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 2 || !params[1].isArray()) {
                continue;
            }
            Object array = java.lang.reflect.Array.newInstance(params[1].getComponentType(), 1);
            java.lang.reflect.Array.set(array, 0, element);
            method.invoke(panel, new Object[] { null, array });
            return;
        }
    }

    private static void invokeRemoveElement(Object panel, Object element) throws ReflectiveOperationException {
        for (Method method : panel.getClass().getMethods()) {
            if (!method.getName().equals("removeElement")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(element.getClass())) {
                method.invoke(panel, element);
                return;
            }
        }
    }

    private static void call(Object target, String name, Class<?> p1, Class<?> p2, Object a1, Object a2)
        throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, p1, p2);
        method.invoke(target, a1, a2);
    }

    private static void call(Object target, String name, Class<?> p1, Object a1) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, p1);
        method.invoke(target, a1);
    }
}
