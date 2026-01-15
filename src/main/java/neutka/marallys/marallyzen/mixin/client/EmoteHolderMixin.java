package neutka.marallys.marallyzen.mixin.client;

import net.minecraft.client.Minecraft;
import neutka.marallys.marallyzen.client.cutscene.editor.CutsceneRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Pseudo
@Mixin(targets = "io.github.kosmx.emotes.main.EmoteHolder", remap = false)
public abstract class EmoteHolderMixin {
    @Shadow
    public abstract UUID getUuid();

    @Inject(
        method = "playEmote(Lio/github/kosmx/emotes/executor/emotePlayer/IEmotePlayerEntity;)Z",
        at = @At("RETURN")
    )
    private void marallyzen$recordLocalEmote(Object player,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        CutsceneRecorder recorder = CutsceneRecorder.getInstance();
        if (recorder == null || !recorder.isRecording()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        String emoteId = resolveEmoteId();
        if (emoteId == null || emoteId.isBlank()) {
            return;
        }
        recorder.recordEmoteEvent(mc.player, emoteId);
    }

    private String resolveEmoteId() {
        String id = extractFieldValue("name");
        if (id != null && !id.isBlank()) {
            return id;
        }
        UUID emoteUuid = getUuid();
        if (emoteUuid != null) {
            return emoteUuid.toString();
        }
        return null;
    }

    private String extractFieldValue(String fieldName) {
        try {
            java.lang.reflect.Field field = getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object val = field.get(this);
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
