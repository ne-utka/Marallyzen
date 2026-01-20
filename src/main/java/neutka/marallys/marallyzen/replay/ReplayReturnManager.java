package neutka.marallys.marallyzen.replay;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.storage.LevelResource;
import neutka.marallys.marallyzen.Marallyzen;

public final class ReplayReturnManager {
    private static final ReplayReturnManager INSTANCE = new ReplayReturnManager();

    private boolean pendingReturn;
    private boolean lastWasSingleplayer;
    private String lastSingleplayerId;
    private String lastSingleplayerName;
    private Object lastServerData;
    private String lastServerName;
    private String lastServerAddress;
    private String pendingSelectWorldId;
    private int pendingSelectWorldAttempts;
    private boolean pendingSingleplayerRetry;
    private long nextSingleplayerRetryAtMs;

    private ReplayReturnManager() {
    }

    public static ReplayReturnManager getInstance() {
        return INSTANCE;
    }

    public void captureSession(Minecraft mc) {
        if (mc == null) {
            return;
        }
        pendingReturn = true;
        lastServerData = null;
        lastServerName = null;
        lastServerAddress = null;
        lastSingleplayerId = null;
        lastSingleplayerName = null;
        pendingSingleplayerRetry = false;
        nextSingleplayerRetryAtMs = 0L;
        lastWasSingleplayer = mc.hasSingleplayerServer();
        if (lastWasSingleplayer) {
            lastSingleplayerId = resolveSingleplayerId(mc);
            lastSingleplayerName = resolveSingleplayerName(mc);
            Marallyzen.LOGGER.info(
                "ReplayReturnManager: captured singleplayer session id={} name={}",
                lastSingleplayerId,
                lastSingleplayerName
            );
        } else {
            Object serverData = mc.getCurrentServer();
            if (serverData != null) {
                lastServerData = serverData;
                lastServerName = readServerValue(serverData, "name", "getName", "getServerName");
                lastServerAddress = readServerValue(serverData, "ip", "getIp", "getAddress", "getServerAddress");
            }
            Marallyzen.LOGGER.info(
                "ReplayReturnManager: captured server session name={} address={}",
                lastServerName,
                lastServerAddress
            );
        }
    }

    public void onReplayStateChanged(boolean wasActive, boolean isActive) {
        if (wasActive && !isActive) {
            requestReturn();
        }
    }

    public void requestReturn() {
        if (!pendingReturn) {
            return;
        }
        pendingReturn = false;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.setScreen(new TitleScreen());
            if (lastWasSingleplayer) {
                if (!openSingleplayer(mc)) {
                    mc.setScreen(new TitleScreen());
                }
                return;
            }
            if (!connectToServer(mc)) {
                mc.setScreen(new TitleScreen());
            }
        });
    }

    public void tick(Minecraft mc, boolean replayActive) {
        if (pendingSelectWorldId != null) {
            if (mc != null) {
                attemptSelectWorldAutoJoin(mc);
            }
        }
        if (pendingSingleplayerRetry && !replayActive) {
            if (mc != null && mc.level == null && System.currentTimeMillis() >= nextSingleplayerRetryAtMs) {
                pendingSingleplayerRetry = false;
                if (!openSingleplayer(mc)) {
                    pendingSingleplayerRetry = true;
                    nextSingleplayerRetryAtMs = System.currentTimeMillis() + 2000L;
                }
            }
        }
        if (!pendingReturn || replayActive) {
            return;
        }
        if (mc == null || mc.level != null) {
            return;
        }
        requestReturn();
    }

    private boolean openSingleplayer(Minecraft mc) {
        if ((lastSingleplayerId == null || lastSingleplayerId.isBlank())
            && (lastSingleplayerName == null || lastSingleplayerName.isBlank())) {
            return false;
        }
        boolean idLooksValid = lastSingleplayerId != null
            && !lastSingleplayerId.isBlank()
            && (lastSingleplayerName == null || !lastSingleplayerId.equals(lastSingleplayerName));
        boolean preferName = isLikelyPlaceholderId(lastSingleplayerId);
        String worldKey = preferName && lastSingleplayerName != null && !lastSingleplayerName.isBlank()
            ? lastSingleplayerName
            : (idLooksValid ? lastSingleplayerId : lastSingleplayerName);
        Marallyzen.LOGGER.info("ReplayReturnManager: reopening singleplayer {}", worldKey);
        if (mc.getSingleplayerServer() != null) {
            scheduleSingleplayerRetry();
            return true;
        }
        if (openSelectWorldScreen(mc, worldKey)) {
            return true;
        }
        scheduleSingleplayerRetry();
        return true;
    }

    private boolean connectToServer(Minecraft mc) {
        if (lastServerData == null && (lastServerAddress == null || lastServerAddress.isBlank())) {
            return false;
        }
        Marallyzen.LOGGER.info(
            "ReplayReturnManager: reconnecting to server name={} address={}",
            lastServerName,
            lastServerAddress
        );
        try {
            Class<?> connectScreenClass = Class.forName("net.minecraft.client.gui.screens.ConnectScreen");
            Object serverData = lastServerData;
            if (serverData == null) {
                Class<?> serverDataClass = Class.forName("net.minecraft.client.multiplayer.ServerData");
                Constructor<?> constructor = serverDataClass.getConstructor(String.class, String.class, boolean.class);
                serverData = constructor.newInstance(
                    lastServerName != null ? lastServerName : "Server",
                    lastServerAddress,
                    false
                );
            }
            Class<?> serverAddressClass = Class.forName("net.minecraft.client.multiplayer.resolver.ServerAddress");
            Method parse = serverAddressClass.getMethod("parseString", String.class);
            Object address = parse.invoke(null, lastServerAddress);
            Method startConnecting = findMethod(
                connectScreenClass,
                "startConnecting",
                net.minecraft.client.gui.screens.Screen.class,
                Minecraft.class,
                serverAddressClass,
                serverData.getClass(),
                boolean.class
            );
            if (startConnecting != null) {
                startConnecting.invoke(null, new TitleScreen(), mc, address, serverData, false);
                return true;
            }
            Method connect = findMethod(
                connectScreenClass,
                "connect",
                net.minecraft.client.gui.screens.Screen.class,
                Minecraft.class,
                serverAddressClass,
                serverData.getClass()
            );
            if (connect != null) {
                connect.invoke(null, new TitleScreen(), mc, address, serverData);
                return true;
            }
        } catch (ReflectiveOperationException e) {
            Marallyzen.LOGGER.warn("ReplayReturnManager: failed to reconnect to server", e);
        }
        return false;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static String readStringField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(target);
            return value != null ? value.toString() : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String readServerValue(Object target, String fieldName, String... methodNames) {
        String value = readStringField(target, fieldName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object result = method.invoke(target);
                if (result != null && !result.toString().isBlank()) {
                    return result.toString();
                }
            } catch (ReflectiveOperationException e) {
                // Try next method.
            }
        }
        return value;
    }

    private static String resolveSingleplayerId(Minecraft mc) {
        try {
            Method getServer = Minecraft.class.getMethod("getSingleplayerServer");
            Object server = getServer.invoke(mc);
            if (server == null) {
                return null;
            }
            try {
                Method getWorldPath = server.getClass().getMethod("getWorldPath", LevelResource.class);
                Object root = getWorldPath.invoke(server, LevelResource.ROOT);
                if (root instanceof Path path) {
                    Path normalized = path.normalize();
                    Path name = normalized.getFileName();
                    if (name != null && ".".equals(name.toString())) {
                        Path parent = normalized.getParent();
                        if (parent != null) {
                            name = parent.getFileName();
                        }
                    }
                    if (name != null) {
                        String value = name.toString();
                        if (!value.isBlank() && !value.equals(".") && !value.equals("..")) {
                            return value;
                        }
                    }
                }
            } catch (ReflectiveOperationException e) {
                // Fall back to world data.
            }
            Method getWorldData = server.getClass().getMethod("getWorldData");
            Object worldData = getWorldData.invoke(server);
            if (worldData == null) {
                return null;
            }
            Method getLevelName = findMethod(worldData.getClass(), "getLevelName");
            if (getLevelName != null) {
                Object value = getLevelName.invoke(worldData);
                return value != null ? value.toString() : null;
            }
            Method getLevelId = findMethod(worldData.getClass(), "getLevelId");
            if (getLevelId != null) {
                Object value = getLevelId.invoke(worldData);
                return value != null ? value.toString() : null;
            }
        } catch (ReflectiveOperationException e) {
            Marallyzen.LOGGER.warn("ReplayReturnManager: failed to capture singleplayer world id", e);
        }
        return null;
    }

    private static boolean isLikelyPlaceholderId(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.equals("map")
            || lower.equals("world")
            || lower.equals("level")
            || lower.equals(".")
            || lower.equals("..");
    }

    private static String resolveSingleplayerName(Minecraft mc) {
        try {
            Method getServer = Minecraft.class.getMethod("getSingleplayerServer");
            Object server = getServer.invoke(mc);
            if (server == null) {
                return null;
            }
            Method getWorldData = server.getClass().getMethod("getWorldData");
            Object worldData = getWorldData.invoke(server);
            if (worldData == null) {
                return null;
            }
            Method getLevelName = findMethod(worldData.getClass(), "getLevelName");
            if (getLevelName != null) {
                Object value = getLevelName.invoke(worldData);
                return value != null ? value.toString() : null;
            }
            Method getLevelId = findMethod(worldData.getClass(), "getLevelId");
            if (getLevelId != null) {
                Object value = getLevelId.invoke(worldData);
                return value != null ? value.toString() : null;
            }
        } catch (ReflectiveOperationException e) {
            return null;
        }
        return null;
    }


    private static Method findCompatibleMethod(Class<?> type, String name, Object... args) {
        for (Method method : getAllMethods(type)) {
            if (!method.getName().equals(name)) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != args.length) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < params.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    continue;
                }
                if (!wrap(params[i]).isAssignableFrom(arg.getClass())) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return method;
            }
        }
        return null;
    }

    private static boolean openSelectWorldScreen(Minecraft mc, String worldId) {
        try {
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.worldselection.SelectWorldScreen");
            Object screen = instantiateSelectWorldScreen(screenClass);
            if (screen == null) {
                return false;
            }
            mc.setScreen((net.minecraft.client.gui.screens.Screen) screen);
            if (invokeScreenMethod(screen, worldId)) {
                return true;
            }
            ReplayReturnManager manager = getInstance();
            manager.pendingSelectWorldId = worldId;
            manager.pendingSelectWorldAttempts = 0;
            Marallyzen.LOGGER.info("ReplayReturnManager: SelectWorldScreen loaded, deferring auto-join");
            return true;
        } catch (ReflectiveOperationException e) {
            Marallyzen.LOGGER.warn("ReplayReturnManager: failed to open SelectWorldScreen", e);
            return false;
        }
    }

    private static Object instantiateSelectWorldScreen(Class<?> screenClass) throws ReflectiveOperationException {
        for (Constructor<?> ctor : screenClass.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 1 && net.minecraft.client.gui.screens.Screen.class.isAssignableFrom(params[0])) {
                return ctor.newInstance(new TitleScreen());
            }
            if (params.length == 2
                && net.minecraft.client.gui.screens.Screen.class.isAssignableFrom(params[0])
                && params[1] == boolean.class) {
                return ctor.newInstance(new TitleScreen(), Boolean.FALSE);
            }
        }
        return null;
    }

    private static boolean invokeScreenMethod(Object screen, String worldId) {
        String[] names = { "loadWorld", "loadLevel", "joinWorld", "tryLoadWorld", "selectWorld" };
        for (String name : names) {
            Method method = findCompatibleMethod(screen.getClass(), name, worldId);
            if (method == null) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(screen, worldId);
                return true;
            } catch (ReflectiveOperationException e) {
                // Try next.
            }
        }
        Object worldList = findWorldList(screen);
        if (worldList != null) {
            if (invokeStringMethod(worldList, worldId, "selectWorld", "select", "setSelected")) {
                if (tryJoinScreen(screen)) {
                    return true;
                }
            }
            Object entry = findWorldEntry(worldList, worldId);
            if (entry != null) {
                if (invokeEntryMethod(worldList, entry, "setSelected", "select", "setSelectedEntry")) {
                    if (tryJoinScreen(screen)) {
                        return true;
                    }
                }
                if (invokeEntryNoArgMethod(entry, "play", "loadWorld", "loadLevel", "joinWorld", "tryLoadWorld", "openWorld")) {
                    return true;
                }
                if (invokeEntryMethod(screen, entry, "loadWorld", "loadLevel", "joinWorld", "tryLoadWorld", "selectWorld")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Object findWorldList(Object screen) {
        for (Field field : screen.getClass().getDeclaredFields()) {
            Class<?> type = field.getType();
            String name = type.getName();
            if (!name.contains("WorldSelectionList") && !name.contains("WorldList")) {
                continue;
            }
            try {
                field.setAccessible(true);
                return field.get(screen);
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private static Object findWorldEntry(Object worldList, String worldId) {
        Object entries = invokeNoArgListMethod(worldList, "children", "getEntries", "getChildren");
        if (!(entries instanceof java.util.List<?> list)) {
            return null;
        }
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String id = invokeEntryId(entry);
            if (matchesWorldId(id, worldId)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean matchesWorldId(String entryId, String worldId) {
        if (entryId == null || worldId == null) {
            return false;
        }
        if (entryId.equals(worldId)) {
            return true;
        }
        String left = normalizeWorldId(entryId);
        String right = normalizeWorldId(worldId);
        return !left.isEmpty() && left.equalsIgnoreCase(right);
    }

    private static String normalizeWorldId(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString();
    }

    private static String invokeEntryId(Object entry) {
        String[] names = { "getLevelId", "getId", "getLevelName", "getName", "getFileName" };
        for (String name : names) {
            for (Method method : getAllMethods(entry.getClass())) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (method.getParameterCount() != 0) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Object value = method.invoke(entry);
                    if (value != null) {
                        return value.toString();
                    }
                } catch (ReflectiveOperationException e) {
                    // Try next.
                }
            }
        }
        return null;
    }

    private static Object invokeNoArgListMethod(Object target, String... names) {
        for (String name : names) {
            for (Method method : getAllMethods(target.getClass())) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (method.getParameterCount() != 0) {
                    continue;
                }
                if (!java.util.List.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (ReflectiveOperationException e) {
                    // Try next.
                }
            }
        }
        return null;
    }

    private static boolean invokeEntryMethod(Object target, Object entry, String... names) {
        for (String name : names) {
            for (Method method : getAllMethods(target.getClass())) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(entry.getClass())) {
                    try {
                        method.setAccessible(true);
                        method.invoke(target, entry);
                        return true;
                    } catch (ReflectiveOperationException e) {
                        // Try next.
                    }
                }
            }
        }
        return false;
    }

    private static boolean invokeEntryNoArgMethod(Object entry, String... names) {
        for (String name : names) {
            for (Method method : getAllMethods(entry.getClass())) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (method.getParameterCount() != 0) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    method.invoke(entry);
                    return true;
                } catch (ReflectiveOperationException e) {
                    // Try next.
                }
            }
        }
        return false;
    }

    private static boolean tryJoinScreen(Object screen) {
        return invokeNoArgMethod(screen, "joinWorld", "loadWorld", "openWorld", "play", "tryLoadWorld")
            || invokeBooleanMethod(screen, false, "joinWorld", "loadWorld", "openWorld", "tryLoadWorld");
    }

    private static boolean hasWorldListEntries(Object worldList) {
        Object entries = invokeNoArgListMethod(worldList, "children", "getEntries", "getChildren");
        if (!(entries instanceof java.util.List<?> list)) {
            return false;
        }
        return !list.isEmpty();
    }

    private static boolean invokeStringMethod(Object target, String value, String... names) {
        for (String name : names) {
            for (Method method : getAllMethods(target.getClass())) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0] == String.class) {
                    try {
                        method.setAccessible(true);
                        method.invoke(target, value);
                        return true;
                    } catch (ReflectiveOperationException e) {
                        // Try next.
                    }
                }
            }
        }
        return false;
    }

    private static boolean invokeNoArgMethod(Object target, String... names) {
        for (String name : names) {
            for (Method method : getAllMethods(target.getClass())) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (method.getParameterCount() == 0) {
                    try {
                        method.setAccessible(true);
                        method.invoke(target);
                        return true;
                    } catch (ReflectiveOperationException e) {
                        // Try next.
                    }
                }
            }
        }
        return false;
    }

    private static boolean invokeBooleanMethod(Object target, boolean value, String... names) {
        for (String name : names) {
            for (Method method : getAllMethods(target.getClass())) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0] == boolean.class) {
                    try {
                        method.setAccessible(true);
                        method.invoke(target, value);
                        return true;
                    } catch (ReflectiveOperationException e) {
                        // Try next.
                    }
                }
            }
        }
        return false;
    }

    private void scheduleSingleplayerRetry() {
        pendingSingleplayerRetry = true;
        nextSingleplayerRetryAtMs = System.currentTimeMillis() + 2000L;
    }

    private void attemptSelectWorldAutoJoin(Minecraft mc) {
        if (pendingSelectWorldId == null || mc == null) {
            return;
        }
        net.minecraft.client.gui.screens.Screen current = mc.screen;
        if (current == null) {
            return;
        }
        if (!"net.minecraft.client.gui.screens.worldselection.SelectWorldScreen".equals(current.getClass().getName())) {
            return;
        }
        Object worldList = findWorldList(current);
        if (worldList == null || !hasWorldListEntries(worldList)) {
            return;
        }
        if (pendingSelectWorldAttempts < 4) {
            pendingSelectWorldAttempts++;
            return;
        }
        if (invokeScreenMethod(current, pendingSelectWorldId)) {
            pendingSelectWorldId = null;
            pendingSelectWorldAttempts = 0;
            return;
        }
        pendingSelectWorldAttempts++;
        if (pendingSelectWorldAttempts > 200) {
            Marallyzen.LOGGER.warn("ReplayReturnManager: auto-join timed out for {}", pendingSelectWorldId);
            pendingSelectWorldId = null;
            pendingSelectWorldAttempts = 0;
        }
    }

    private static java.util.List<Method> getAllMethods(Class<?> type) {
        java.util.List<Method> methods = new java.util.ArrayList<>();
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                methods.add(method);
            }
            current = current.getSuperclass();
        }
        return methods;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
