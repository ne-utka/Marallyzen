package neutka.marallys.marallyzen.cutscene.world;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import neutka.marallys.marallyzen.Marallyzen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection-based access to light data for cutscene snapshots.
 */
public final class CutsceneLightAccess {
    private static final String DATA_LAYER_CLASS = "net.minecraft.world.level.chunk.DataLayer";
    private static Method getLayerListenerMethod;
    private static Method getDataLayerMethod;
    private static Method setDataLayerMethod;
    private static Constructor<?> dataLayerCtor;
    private static Field dataLayerBytesField;
    private static boolean resolved;

    private CutsceneLightAccess() {
    }

    public static List<CutsceneWorldTrack.LightSectionData> capture(Level level, ChunkPos pos, int sectionCount,
                                                                    int minSection, LightLayer layer) {
        List<CutsceneWorldTrack.LightSectionData> result = new ArrayList<>();
        Object storage = getLightStorage(level, layer);
        if (storage == null) {
            return result;
        }
        resolve(storage.getClass());
        for (int i = 0; i < sectionCount; i++) {
            long sectionPos = SectionPos.asLong(pos.x, minSection + i, pos.z);
            Object dataLayer = getDataLayer(storage, sectionPos);
            byte[] data = readDataLayer(dataLayer);
            if (data != null) {
                result.add(new CutsceneWorldTrack.LightSectionData(i, data));
            }
        }
        return result;
    }

    public static void apply(Level level, int chunkX, int chunkZ, List<CutsceneWorldTrack.LightSectionData> sections,
                             int minSection, LightLayer layer) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        Object storage = getLightStorage(level, layer);
        if (storage == null) {
            return;
        }
        resolve(storage.getClass());
        for (CutsceneWorldTrack.LightSectionData section : sections) {
            if (section == null || section.data() == null) {
                continue;
            }
            long sectionPos = SectionPos.asLong(chunkX, minSection + section.sectionIndex(), chunkZ);
            byte[] bytes = section.data();
            Object dataLayer = createDataLayer(bytes);
            if (dataLayer == null && bytes == null) {
                continue;
            }
            setDataLayer(storage, sectionPos, bytes, dataLayer);
        }
    }

    private static Object getLightStorage(Level level, LightLayer layer) {
        if (level == null) {
            return null;
        }
        LevelLightEngine engine = level.getChunkSource().getLightEngine();
        if (engine == null) {
            return null;
        }
        if (getLayerListenerMethod == null) {
            try {
                getLayerListenerMethod = engine.getClass().getMethod("getLayerListener", LightLayer.class);
            } catch (Exception e) {
                return null;
            }
        }
        try {
            return getLayerListenerMethod.invoke(engine, layer);
        } catch (Exception e) {
            return null;
        }
    }

    private static void resolve(Class<?> storageClass) {
        if (resolved || storageClass == null) {
            return;
        }
        resolved = true;
        try {
            Class<?> dataLayerClass = Class.forName(DATA_LAYER_CLASS);
            dataLayerCtor = findDataLayerCtor(dataLayerClass);
            dataLayerBytesField = findByteArrayField(dataLayerClass);
            getDataLayerMethod = findDataLayerMethod(storageClass, dataLayerClass, false, "getDataLayerData", "getDataLayer");
            setDataLayerMethod = findDataLayerMethod(storageClass, dataLayerClass, true, "setDataLayerData", "setDataLayer");
        } catch (Exception e) {
            Marallyzen.LOGGER.warn("Failed to resolve light data access", e);
        }
    }

    private static Object getDataLayer(Object storage, long sectionPos) {
        if (storage == null || getDataLayerMethod == null) {
            return null;
        }
        try {
            if (getDataLayerMethod.getParameterTypes()[0] == long.class) {
                return getDataLayerMethod.invoke(storage, sectionPos);
            }
            return getDataLayerMethod.invoke(storage, SectionPos.of(sectionPos));
        } catch (Exception e) {
            return null;
        }
    }

    private static void setDataLayer(Object storage, long sectionPos, byte[] data, Object dataLayer) {
        if (storage == null || setDataLayerMethod == null) {
            return;
        }
        try {
            Object payload = dataLayer;
            if (setDataLayerMethod.getParameterTypes()[1] == byte[].class) {
                payload = data;
            }
            if (payload == null) {
                return;
            }
            if (setDataLayerMethod.getParameterTypes()[0] == long.class) {
                setDataLayerMethod.invoke(storage, sectionPos, payload);
            } else {
                setDataLayerMethod.invoke(storage, SectionPos.of(sectionPos), payload);
            }
        } catch (Exception ignored) {
        }
    }

    private static Method findDataLayerMethod(Class<?> storageClass, Class<?> dataLayerClass,
                                              boolean forSet, String... names) {
        for (String name : names) {
            for (Method method : storageClass.getDeclaredMethods()) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                if (forSet) {
                    if (method.getParameterCount() != 2 || method.getReturnType() != Void.TYPE) {
                        continue;
                    }
                    if (!isSectionParam(method.getParameterTypes()[0])) {
                        continue;
                    }
                    Class<?> second = method.getParameterTypes()[1];
                    if (second != dataLayerClass && second != byte[].class) {
                        continue;
                    }
                } else {
                    if (method.getParameterCount() != 1 || !dataLayerClass.isAssignableFrom(method.getReturnType())) {
                        continue;
                    }
                    if (!isSectionParam(method.getParameterTypes()[0])) {
                        continue;
                    }
                }
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static boolean isSectionParam(Class<?> type) {
        return type == long.class || type == SectionPos.class;
    }

    private static Constructor<?> findDataLayerCtor(Class<?> dataLayerClass) {
        for (Constructor<?> ctor : dataLayerClass.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 1 && params[0] == byte[].class) {
                ctor.setAccessible(true);
                return ctor;
            }
        }
        return null;
    }

    private static Field findByteArrayField(Class<?> dataLayerClass) {
        for (Field field : dataLayerClass.getDeclaredFields()) {
            if (field.getType() == byte[].class) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static byte[] readDataLayer(Object dataLayer) {
        if (dataLayer == null || dataLayerBytesField == null) {
            return null;
        }
        try {
            byte[] data = (byte[]) dataLayerBytesField.get(dataLayer);
            return data == null ? null : data.clone();
        } catch (Exception e) {
            return null;
        }
    }

    private static Object createDataLayer(byte[] data) {
        if (data == null || dataLayerCtor == null) {
            return null;
        }
        try {
            return dataLayerCtor.newInstance((Object) data);
        } catch (Exception e) {
            return null;
        }
    }
}
