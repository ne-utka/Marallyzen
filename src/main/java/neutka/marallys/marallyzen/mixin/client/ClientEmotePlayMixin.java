package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.Minecraft;
import neutka.marallys.marallyzen.client.cutscene.editor.CutsceneRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Pseudo
@Mixin(targets = "io.github.kosmx.emotes.main.network.ClientEmotePlay", remap = false)
public abstract class ClientEmotePlayMixin {

    @Inject(
        method = "clientStartLocalEmote(Lio/github/kosmx/emotes/main/EmoteHolder;)V",
        at = @At("TAIL")
    )
    private static void marallyzen$recordLocalEmoteStartFromHolder(@Coerce Object holder,
                                                                   CallbackInfo ci) {
        CutsceneRecorder recorder = CutsceneRecorder.getInstance();
        if (recorder == null || !recorder.isRecording()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        String emoteId = resolveEmoteIdFromHolder(holder);
        if (emoteId == null || emoteId.isBlank()) {
            return;
        }
        recorder.recordEmoteEvent(mc.player, emoteId);
    }

    @Inject(
        method = "clientStartLocalEmote(Ldev/kosmx/playerAnim/core/data/KeyframeAnimation;)Z",
        at = @At("RETURN")
    )
    private static void marallyzen$recordLocalEmoteStartFromAnimation(@Coerce Object animation,
                                                                      CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        recordLocalEmoteFromAnimation(animation);
    }

    @Inject(
        method = "clientStartLocalEmote(Ldev/kosmx/playerAnim/core/data/KeyframeAnimation;I)Z",
        at = @At("RETURN")
    )
    private static void marallyzen$recordLocalEmoteStartFromAnimationLayer(@Coerce Object animation,
                                                                           int layer,
                                                                           CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        recordLocalEmoteFromAnimation(animation);
    }

    private static void recordLocalEmoteFromAnimation(Object animation) {
        CutsceneRecorder recorder = CutsceneRecorder.getInstance();
        if (recorder == null || !recorder.isRecording()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        String emoteId = resolveEmoteId(animation);
        if (emoteId == null || emoteId.isBlank()) {
            return;
        }
        recorder.recordEmoteEvent(mc.player, emoteId);
    }

    private static String resolveEmoteId(Object emote) {
        if (emote == null) {
            return null;
        }
        try {
            java.lang.reflect.Method uuidMethod = emote.getClass().getMethod("uuid");
            Object val = uuidMethod.invoke(emote);
            if (val instanceof UUID uuid) {
                return uuid.toString();
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method uuidMethod = emote.getClass().getMethod("getUuid");
            Object val = uuidMethod.invoke(emote);
            if (val instanceof UUID uuid) {
                return uuid.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String resolveEmoteIdFromHolder(Object holder) {
        if (holder == null) {
            return null;
        }
        String id = extractFieldValue(holder, "name");
        if (id != null && !id.isBlank()) {
            return id;
        }
        try {
            java.lang.reflect.Method getUuid = holder.getClass().getMethod("getUuid");
            Object val = getUuid.invoke(holder);
            if (val instanceof UUID uuid) {
                return uuid.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String extractFieldValue(Object holder, String fieldName) {
        try {
            java.lang.reflect.Field field = holder.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object val = field.get(holder);
            if (val == null) {
                return null;
            }
            if (val instanceof net.minecraft.resources.ResourceLocation id) {
                return id.toString();
            }
            if (val instanceof String s) {
                return s;
            }
            try {
                java.lang.reflect.Method getString = val.getClass().getMethod("getString");
                Object result = getString.invoke(val);
                if (result != null) {
                    return result.toString();
                }
            } catch (Exception ignored) {
            }
            return val.toString();
        } catch (Exception ignored) {
        }
        return null;
    }
}
