package neutka.marallys.marallyzen.client.cutscene.editor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import neutka.marallys.marallyzen.Marallyzen;

@EventBusSubscriber(modid = Marallyzen.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CutscenePreviewGhostRenderHandler {
    private static boolean partialTickMethodsResolved;
    private static java.lang.reflect.Method partialTickGameTimeMethod;
    private static java.lang.reflect.Method partialTickRealtimeMethod;

    private CutscenePreviewGhostRenderHandler() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return;
        }
        float partialTick = resolvePartialTick(event.getPartialTick());
        CutscenePreviewPlayer.renderActiveGhostActors(partialTick);
    }

    private static float resolvePartialTick(Object partialTickSource) {
        if (partialTickSource == null) {
            return 0.0f;
        }
        if (!partialTickMethodsResolved) {
            partialTickMethodsResolved = true;
            Class<?> sourceClass = partialTickSource.getClass();
            try {
                partialTickGameTimeMethod = sourceClass.getMethod("getGameTimeDeltaPartialTick", boolean.class);
            } catch (Exception ignored) {
            }
            if (partialTickGameTimeMethod == null) {
                try {
                    partialTickGameTimeMethod = sourceClass.getMethod("getGameTimeDeltaTicks");
                } catch (Exception ignored) {
                }
            }
            try {
                partialTickRealtimeMethod = sourceClass.getMethod("getRealtimeDeltaPartialTick");
            } catch (Exception ignored) {
            }
            if (partialTickRealtimeMethod == null) {
                try {
                    partialTickRealtimeMethod = sourceClass.getMethod("getRealtimeDeltaTicks");
                } catch (Exception ignored) {
                }
            }
        }
        Float value = invokePartialTick(partialTickSource, partialTickGameTimeMethod, true);
        if (value != null && value > 0.0f) {
            return value;
        }
        value = invokePartialTick(partialTickSource, partialTickRealtimeMethod, false);
        return value != null ? value : 0.0f;
    }

    private static Float invokePartialTick(Object source, java.lang.reflect.Method method, boolean booleanArg) {
        if (method == null) {
            return null;
        }
        try {
            Object result;
            if (method.getParameterCount() == 0) {
                result = method.invoke(source);
            } else {
                result = method.invoke(source, booleanArg);
            }
            if (result instanceof Number number) {
                return number.floatValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
