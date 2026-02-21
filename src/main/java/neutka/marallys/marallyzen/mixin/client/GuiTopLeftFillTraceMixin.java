package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import neutka.marallys.marallyzen.Marallyzen;
import neutka.marallys.marallyzen.client.quest.QuestClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(GuiGraphics.class)
public abstract class GuiTopLeftFillTraceMixin {
    @Unique
    private static final Set<String> marallyzen$loggedCallsites = ConcurrentHashMap.newKeySet();

    @Inject(method = "fill(IIIII)V", at = @At("HEAD"), cancellable = true)
    private void marallyzen$traceTopLeftFill(int minX, int minY, int maxX, int maxY, int color, CallbackInfo ci) {
        int width = maxX - minX;
        int height = maxY - minY;
        int alpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        // Track only suspicious top-left translucent rectangles to avoid log spam.
        if (minX < 0 || minX > 20 || minY < 0 || minY > 20) {
            return;
        }
        if (width < 40 || width > 320 || height < 12 || height > 120) {
            return;
        }
        if (alpha < 32 || alpha > 220) {
            return;
        }

        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        boolean fromQuestHud = false;
        String owner = "unknown";
        for (StackTraceElement element : trace) {
            String className = element.getClassName();
            if ("neutka.marallys.marallyzen.client.quest.QuestHudRenderer".equals(className)) {
                fromQuestHud = true;
            }
            if (className.startsWith("java.")
                    || className.startsWith("sun.")
                    || className.startsWith("jdk.")
                    || className.startsWith("org.spongepowered.asm.")
                    || className.contains("GuiTopLeftFillTraceMixin")
                    || className.equals("net.minecraft.client.gui.GuiGraphics")) {
                continue;
            }
            owner = className + "#" + element.getMethodName() + ":" + element.getLineNumber();
            break;
        }

        String signature = owner + "|" + minX + "," + minY + "," + width + "x" + height + "|a=" + alpha;
        if (marallyzen$loggedCallsites.add(signature)) {
            Marallyzen.LOGGER.warn("GUI_TOP_LEFT_FILL_TRACE source={} rect=({}, {}) {}x{} color=0x{}",
                    owner, minX, minY, width, height, Integer.toHexString(color));
        }

        boolean fromParticularDebugHud =
                owner.startsWith("com.leclowndu93150.particular.NeoForgeClientEvents$ForgeEvents#onRenderHUD");
        if (fromParticularDebugHud) {
            String suppressKey = "SUPPRESS_PARTICULAR|" + signature;
            if (marallyzen$loggedCallsites.add(suppressKey)) {
                Marallyzen.LOGGER.warn("GUI_TOP_LEFT_FILL_SUPPRESSED_PARTICULAR source={} rect=({}, {}) {}x{}",
                        owner, minX, minY, width, height);
            }
            ci.cancel();
            return;
        }

        // Suppress the persistent empty rectangle artifact (top-left dark box) when it is not ours.
        boolean likelyArtifact = minX <= 12
                && minY <= 12
                && width >= 80
                && width <= 240
                && height >= 18
                && height <= 48
                && alpha >= 64
                && alpha <= 200
                && rgb == 0;
        if (likelyArtifact && !fromQuestHud) {
            if (!QuestClientConfig.readQuestHudEnabled()) {
                ci.cancel();
                return;
            }
            String suppressKey = "SUPPRESS|" + signature;
            if (marallyzen$loggedCallsites.add(suppressKey)) {
                Marallyzen.LOGGER.warn("GUI_TOP_LEFT_FILL_SUPPRESSED source={} rect=({}, {}) {}x{}",
                        owner, minX, minY, width, height);
            }
            ci.cancel();
        }
    }
}
